package de.embl.cba.spindle3d.command;

import de.embl.cba.morphometry.ImageSuite3D;
import de.embl.cba.morphometry.Measurements;
import de.embl.cba.morphometry.Utils;
import de.embl.cba.spindle3d.Spindle3DMeasurements;
import de.embl.cba.spindle3d.Spindle3DMorphometry;
import de.embl.cba.spindle3d.Spindle3DSettings;
import de.embl.cba.spindle3d.Spindle3DVersion;
import de.embl.cba.tables.Tables;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import loci.common.DebugTools;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.io.FilenameUtils;
import org.scijava.ItemVisibility;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class Spindle3DProcessor
{
	public Spindle3DSettings settings = new Spindle3DSettings();

	@Parameter
	public OpService opService;

	@Parameter
	public ScriptService scriptService;

	@Parameter( visibility = ItemVisibility.MESSAGE )
	public String version = "Spindle Morphometry Version: " + Spindle3DVersion.VERSION;

	@Parameter ( label = "Output Directory", style = "directory" )
	public File outputDirectory;

	@Parameter ( label = "DNA Channel [one-based index]" )
	public long dnaChannelIndexOneBased = 2;

	@Parameter ( label = "Spindle Channel [one-based index]" )
	public long spindleChannelIndexOneBased = 1;

	public boolean showIntermediateImages = false;
	public boolean showIntermediatePlots = false;
	public boolean saveResults = true;

	protected File inputImageFilesParentDirectory = new File("/" );
	protected String imageName;
	protected HashMap< Integer, Map< String, Object > > objectMeasurements;

	protected void setSettings()
	{
		DebugTools.setRootLevel( "OFF" ); // Bio-Formats
		if ( ! ImageSuite3D.isAvailable() ) throw new RuntimeException( "Please enable the 3D Image Suite update site!" );

		settings.showIntermediateImages = showIntermediateImages;
		settings.showIntermediatePlots = showIntermediatePlots;
		settings.outputDirectory = outputDirectory;
		settings.version = version;
	}

	public HashMap< Integer, Map< String, Object > > getObjectMeasurements()
	{
		return objectMeasurements;
	}

	protected void processFile( File imagePath )
	{
		setImageName( imagePath.getName() );

		ImagePlus imagePlus;
		try
		{
			// because Bio-Formats does not read the imageJ Rois.
			imagePlus = IJ.openImage( imagePath.toString() );

		}
		catch ( Exception e )
		{
			imagePlus = Utils.openWithBioFormats( imagePath );
		}

		imagePlus.setTitle( imageName );

		final RandomAccessibleInterval< BitType > cellMask = tryOpenCellMask( imagePath.toString() );

		processImagePlus( imagePlus, cellMask );
	}

	protected void processImagePlus( ImagePlus imagePlus, RandomAccessibleInterval< BitType > cellMask )
	{
		IJ.log( settings.toString() );

		settings.cellMask = cellMask;

		setImageName( imagePlus.getTitle() );

		logStart( imageName );

		final RandomAccessibleInterval< ? > raiXYCZ = asRAIXYCZ( imagePlus );

		setSpindlePolePositions( imagePlus );

		Spindle3DMorphometry morphometry = new Spindle3DMorphometry( settings, opService, scriptService );

		final String log = morphometry.run( raiXYCZ );

		IJ.log( log );

		objectMeasurements = morphometry.getObjectMeasurements();

		addImagePathToMeasurements(
				inputImageFilesParentDirectory.toPath(),
				new File( imagePlus.getOriginalFileInfo().directory, imagePlus.getOriginalFileInfo().fileName ),
				objectMeasurements,
				"Path_InputImage" );

		if ( saveResults ) new File( getOutputDirectory() ).mkdirs();

		if ( log.equals( Spindle3DMeasurements.ANALYSIS_FINISHED ))
		{
			if ( this.settings.showOutputImage == true || saveResults )
			{
				final CompositeImage outputImage = morphometry.createOutputImage( 36, 0.5 );

				if ( this.settings.showOutputImage == true )
					outputImage.show();

				if ( saveResults )
					saveOutputImageAndAddImagePathsToMeasurements( outputImage );
			}
		}

		if ( saveResults ) saveMeasurements( );

		logEnd();
	}

	private void setSpindlePolePositions( ImagePlus imagePlus )
	{
		final Roi roi = imagePlus.getRoi();
		if ( roi != null && roi instanceof PointRoi )
		{
			if ( roi.getContainedPoints().length == 2 )
			{
				settings.manualSpindleAxisPositions = new double[ 2 ][ 3 ];

				for ( int i = 0; i < 2; i++ )
				{
					final int z = ( ( PointRoi ) roi ).getPointPosition( i );
					final Point point = roi.getContainedPoints()[ i ];
					settings.manualSpindleAxisPositions[ i ] = new double[]{ point.x, point.y, z };
				}
			}
		}
	}

	protected RandomAccessibleInterval< ? > asRAIXYCZ( ImagePlus imagePlus )
	{
		setSettingsFromImagePlus( imagePlus );

		final RandomAccessibleInterval< ? > raiXYCZ = ImageJFunctions.wrapReal( imagePlus );

		settings.dnaChannelIndex = dnaChannelIndexOneBased - 1;
		settings.tubulinChannelIndex = spindleChannelIndexOneBased - 1;
		return raiXYCZ;
	}

	protected RandomAccessibleInterval< BitType > tryOpenCellMask( String imagePath )
	{
		final String extension = "." + FilenameUtils.getExtension( imagePath );
		final String cellMaskPath = imagePath.replace( extension, "_CellMask" + extension );

		if ( new File( cellMaskPath ).exists() )
		{
			final RandomAccessibleInterval< RealType > rai = ImageJFunctions.wrapReal( Utils.openWithBioFormats( cellMaskPath ) );
			RandomAccessibleInterval< BitType > cellMask = Converters.convert( rai, ( i, o ) -> o.set( i.getRealDouble() > 0.5 ? true : false ), new BitType() );
			IJ.log( "Found cell mask file: " + cellMaskPath );
			return cellMask;
		}
		else
		{
			return null;
		}
	}

	protected void setImageName( String name )
	{
		imageName = name.replace( ".tif", "" );
		imageName = imageName.replace( ".ome", "" );
		imageName = imageName.replace( ".zip", "" );
	}

	protected void logEnd()
	{
		IJ.log( "Done!" );
	}

	protected void logStart( String imageName )
	{
		IJ.log( "\n## Spindle Morphometry Measurements" );
		IJ.log( "Processing image: " + imageName );
	}

	protected void saveMeasurements( )
	{
		final JTable jTable = Measurements.asTable( objectMeasurements );

		final File tableOutputFile = new File( getOutputDirectory() + "measurements.txt" );

		IJ.log( "Saving measurements table:\n" + tableOutputFile );

		Tables.saveTable( jTable, tableOutputFile );
	}

	protected String getOutputDirectory()
	{
		return outputDirectory
				+ File.separator
				+ imageName
				+ File.separator;
	}

	protected void setSettingsFromImagePlus( ImagePlus imagePlus )
	{
		settings.inputVoxelSize = Utils.getCalibration( imagePlus );
		settings.imagePlusCalibration = imagePlus.getCalibration();
		settings.inputDataSetName = imagePlus.getTitle();
	}

	protected void saveOutputImageAndAddImagePathsToMeasurements( ImagePlus imagePlus )
	{
		final Path parentPath = inputImageFilesParentDirectory.toPath();

		final File outputImageFile = new File( getOutputDirectory() + imageName + "-out.zip" );

		addImagePathToMeasurements( parentPath, outputImageFile, objectMeasurements, "Path_OutputImage" );

		IJ.log( "Saving output image:\n" + outputImageFile );
		IJ.saveAs( imagePlus, "ZIP", outputImageFile.toString() );
	}

	protected static void addImagePathToMeasurements(
			Path parentPath,
			File inputImageFile,
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			String path_inputImage )
	{
		Path relativeInputImagePath;
		try
		{
			relativeInputImagePath = parentPath.relativize( inputImageFile.toPath() );
		}
		catch ( Exception e )
		{
			relativeInputImagePath = inputImageFile.toPath();
		}

		Measurements.addMeasurement(
				objectMeasurements,
				0,
				path_inputImage,
				relativeInputImagePath );
	}
}
