package de.embl.cba.microglia.command;

import de.embl.cba.microglia.MicrogliaSegmentationAndTracking;
import de.embl.cba.microglia.MicrogliaSettings;
import de.embl.cba.morphometry.Logger;
import de.embl.cba.morphometry.Utils;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;


@Plugin(type = Command.class, menuPath = "Plugins>Tracking>Microglia Segmentation And Tracking" )
public class MicrogliaSegmentationAndTrackingCommand< T extends RealType<T> & NativeType< T > > implements Command
{
	private MicrogliaSettings settings = new MicrogliaSettings();

	@Parameter
	public OpService opService;

	@Parameter( label = "Intensity image time series (single channel 2D+t)")
	public File intensityFile;

	@Parameter( label = "Intensity threshold [relative]")
	public double relativeIntensityThreshold = 1.5;

	@Parameter( label = "Proceed from existing segmentation")
	public boolean proceedFromExisting = false;

	@Parameter( label = "Label mask time series (single channel 2D+t)", required = false )
	public File segmentationFile = null;

	@Parameter( label = "Output directory", style = "directory" )
	public File outputDirectory = new File("src/test/resources/data");

//	@Parameter( label = "Minimal time frame to be processed", min = "1" )
	public long tMinOneBased = 1;

//	@Parameter( label = "Maximal time frame to be processed", min = "1" )
	public long tMaxOneBased = 1000000000L;


	@Parameter
	public boolean showIntermediateResults = settings.showIntermediateResults;

	private ImagePlus imagePlus;
	private ArrayList< RandomAccessibleInterval< T > > intensities;

	public void run()
	{
		setSettings();
		processFile( intensityFile );
	}

	public void setSettings()
	{
		settings.outputLabelingsPath = outputDirectory + File.separator
			+ intensityFile.getName().split( "\\." )[ 0 ] + "-labelMasks.tif";
		settings.showIntermediateResults = showIntermediateResults;
		settings.outputDirectory = outputDirectory;
		settings.opService = opService;
		settings.thresholdInUnitsOfBackgroundPeakHalfWidth = relativeIntensityThreshold;
	}

	private void processFile( File file )
	{
		openIntensitiesAsFrameList( file );

		final ArrayList< RandomAccessibleInterval< T > > labelings = computeLabels();

		Utils.saveLabelings(
				labelings,
				imagePlus.getCalibration(),
				settings.outputLabelingsPath );
	}

	private void openIntensitiesAsFrameList( File file )
	{
		imagePlus = Utils.openWithBioFormats( file.getAbsolutePath() );

		if ( imagePlus == null )
		{
			Logger.error( "Could not open image: " + file );
			return;
		}

		if ( imagePlus.getNChannels() > 1 )
		{
			Logger.error( "Only single channel files are supported. " +
					"Please use [ Image > Color > Split Channels ] and [ File > Save as..] to " +
					"save the channel that you want to segment and track as a single file.");
			return;
		}

		settings.calibration = imagePlus.getCalibration();

		intensities = Utils.get2DImagePlusMovieAsFrameList(
				imagePlus,
				1,
				tMinOneBased,
				Math.min( tMaxOneBased, imagePlus.getNFrames() ) );
	}

	private ArrayList< RandomAccessibleInterval< T > > computeLabels()
	{
		final MicrogliaSegmentationAndTracking segmentationAndTracking =
				new MicrogliaSegmentationAndTracking( intensities, settings );

		if ( proceedFromExisting )
		{
			final ImagePlus labelsImp
					= Utils.openWithBioFormats( segmentationFile.getAbsolutePath() );

			final ArrayList< RandomAccessibleInterval< T > > labelings
					= Utils.get2DImagePlusMovieAsFrameList(
					labelsImp,
					1 );

			segmentationAndTracking.setLabelings( labelings );
		}

		segmentationAndTracking.run();

		final ArrayList< RandomAccessibleInterval< T > > labelings
				= segmentationAndTracking.getLabelings();

		return labelings;
	}

}
