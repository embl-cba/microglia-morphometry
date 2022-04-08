package de.embl.cba.microglia.command;

import de.embl.cba.microglia.MicrogliaMorphometry;
import de.embl.cba.morphometry.Logger;
import de.embl.cba.morphometry.Measurements;
import de.embl.cba.morphometry.Utils;
import de.embl.cba.tables.Tables;
import ij.ImagePlus;
import inra.ijpb.measure.region2d.GeodesicDiameter;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.microglia.Utils.*;
import static de.embl.cba.morphometry.Version.getArtifactVersion;
import static de.embl.cba.tables.Tables.addRelativeImagePathColumn;
import static de.embl.cba.tables.Tables.saveTable;

@Plugin(type = Command.class, menuPath = "Plugins>Microglia>Measure Microglia Morphometry" )
public class MicrogliaMorphometryCommand < T extends RealType< T > & NativeType< T > >
		implements Command
{
	public static final String DELIM = "\t";

	@Parameter
	public OpService opService;

	@Parameter ( label = "Intensity image time series (single channel 2D+t)" )
	public File intensityFile;

	@Parameter ( label = "Label mask time series (single channel 2D+t)" )
	public File labelMaskFile;

	@Parameter ( label = "Output directory", style = "directory" )
	public File outputDirectory;

	@Parameter
	public boolean showIntermediateResults = false;

	private ImagePlus labelMaskImagePlus;
	private File tableOutputFile;
	private String dataSetID;
	private ImagePlus intensityImagePlus;

	public void run()
	{
		if ( ! checkInstallationOfMorpholibJ() ) return;

		dataSetID = FilenameUtils.removeExtension( intensityFile.getName() );

		Logger.log( "\n# Microglia Morphometry Measurements - Version " + getArtifactVersion() );
		Logger.log( "Analyzing: " + dataSetID );

		final MicrogliaMorphometry< T > microgliaMorphometry =
				new MicrogliaMorphometry(
						openLabelMasks(),
						openIntensities(),
						opService );

		microgliaMorphometry.run();
		saveResults( dataSetID, microgliaMorphometry );
		Logger.log( "Done!" );
	}

	private boolean checkInstallationOfMorpholibJ()
	{
		try {
			new GeodesicDiameter(  );
		}
		catch( NoClassDefFoundError e ) {
			Logger.error( "Please install MorpholibJ by adding the Update Site: IJPB-plugins" );
			return false;
		}

		return true;
	}

	private ArrayList< RandomAccessibleInterval< T > > openLabelMasks()
	{
		labelMaskImagePlus = openAsImagePlus( labelMaskFile );
		return Utils.get2DImagePlusMovieAsFrameList( labelMaskImagePlus, 1 );
	}

	private ArrayList< RandomAccessibleInterval< T > > openIntensities()
	{
		intensityImagePlus = openAsImagePlus( intensityFile );
		return Utils.get2DImagePlusMovieAsFrameList( intensityImagePlus, 1 );
	}

	private void saveResults( String dataSetID,
							  MicrogliaMorphometry< T > microgliaMorphometry )
	{
		final ArrayList< HashMap< Integer, Map< String, Object > > > measurementsTimepointList = microgliaMorphometry.getMeasurementsTimepointList();

		Measurements.addCalibration( measurementsTimepointList, labelMaskImagePlus );

		final JTable table = Measurements.asTable( measurementsTimepointList );

		tableOutputFile = new File(
				outputDirectory.toString() + File.separator + dataSetID + ".csv" );

		addRelativeImagePathColumn(
				table,
				labelMaskFile.getAbsolutePath(),
				outputDirectory.getAbsolutePath(),
				"LabelMasks" );

		addRelativeImagePathColumn(
				table,
				intensityFile.getAbsolutePath(),
				outputDirectory.getAbsolutePath(),
				"Intensities" );

		saveSkeletons( dataSetID, microgliaMorphometry, table );

		saveAnnotations( dataSetID, microgliaMorphometry, table );

		Logger.log( "Saving results table: " + tableOutputFile );

		saveTable( table, tableOutputFile );
	}

	public File getTableOutputFile()
	{
		return tableOutputFile;
	}

	private void saveSkeletons(
			String dataSetID,
			MicrogliaMorphometry< T > microgliaMorphometry,
			JTable table )
	{
		final File file = new File( dataSetID + "-skeletons.tif" );

		final String imageName = dataSetID + "-skeletons";

		Utils.saveRAIListAsMovie(
				microgliaMorphometry.getSkeletons(),
				labelMaskImagePlus.getCalibration(),
				outputDirectory.toString() + File.separator + file,
				imageName );

		Tables.addColumn( table, "Path_Skeletons", file );
	}

	private void saveAnnotations(
			String dataSetID,
			MicrogliaMorphometry< T > microgliaMorphometry,
			JTable table )
	{
		final File file = new File( dataSetID + "-annotations.tif" );

		final String imageName = dataSetID + "-annotations";

		Utils.saveRAIListAsMovie(
				microgliaMorphometry.getAnnotations(),
				labelMaskImagePlus.getCalibration(),
				outputDirectory.toString() + File.separator + file,
				imageName );

		Tables.addColumn( table, "Path_Annotations", file );
	}

}
