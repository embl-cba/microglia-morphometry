package de.embl.cba.microglia.command;

import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.segment.MicrogliaSegmentationAndTracking;
import de.embl.cba.microglia.segment.MicrogliaSettings;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;

import static de.embl.cba.microglia.Utils.*;


@Plugin(type = Command.class, menuPath = "Plugins>Microglia>New Microglia Segmentation And Tracking" )
public class MicrogliaSegmentationAndTrackingCommand< T extends RealType<T> & NativeType< T > > implements Command
{
	protected final MicrogliaSettings settings = new MicrogliaSettings();

	@Parameter
	public OpService opService;

	@Parameter( label = "Intensity image time series (single channel 2D+t)")
	public File intensityFile;

	@Parameter( label = "Intensity threshold [relative]", style = "format:#.00")
	public double relativeIntensityThreshold = 1.5;

	@Parameter( label = "Output directory", style = "directory" )
	public File outputDirectory = new File("src/test/resources/data");

	@Parameter( label = "Minimal cell size [um^2]" )
	public double minimalMicrogliaSize = 200;

	@Parameter( label = "Maximal cell skeleton length [um]" )
	public double skeletonMaxLength = 450;

	public final boolean showIntermediateResults = settings.showIntermediateResults;

	public final long tMinOneBased = 1;
	public final long tMaxOneBased = 1000000000L;

	protected ImagePlus imagePlus;
	protected ArrayList< RandomAccessibleInterval< T > > intensities;

	public void run()
	{
		setSettings();
		processFile( intensityFile, null );
	}

	public void setSettings()
	{
		settings.outputLabelingsPath = outputDirectory + File.separator
			+ intensityFile.getName().split( "\\." )[ 0 ] + "-labelMasks.tif";
		settings.showIntermediateResults = showIntermediateResults;
		settings.opService = opService;
		settings.thresholdInUnitsOfBackgroundPeakHalfWidth = relativeIntensityThreshold;
		settings.minimalObjectSize = minimalMicrogliaSize;
		settings.skeletonMaxLength = skeletonMaxLength;
	}

	protected void processFile( File intensityFile, File segmentationFile )
	{
		openIntensitiesAsFrameList( intensityFile );

		if ( intensities == null )
			return;

		final ArrayList< RandomAccessibleInterval< T > > labelings = computeLabels( segmentationFile );

		saveLabels( labelings, imagePlus.getCalibration(), settings.outputLabelingsPath );
	}

	private void openIntensitiesAsFrameList( File file )
	{
		imagePlus = openAsImagePlus( file );

		if ( imagePlus.getNChannels() > 1 )
		{
			IJ.error( "Only single channel files are supported. " +
					"Please use [ Image > Color > Split Channels ] and [ File > Save as..] to " +
					"save the channel that you want to segment and track as a single file.");
			return;
		}

		settings.calibration = imagePlus.getCalibration();

		if ( settings.calibration.getUnit() != "micrometer" && settings.calibration.getUnit() != "micron" )
		{
			boolean ok = IJ.showMessageWithCancel( "Wrong calibration?", "The calibration unit of your image is: " + settings.calibration.getUnit() +
					"\nThis plugin expects micrometer units." +
					"\nPlease cancel and use [ Image > Properties ] to configure the pixel size in micrometers." +
					"\nTo avoid that this error message pops up you must use \"micrometer\" as the unit." );
			if ( !ok )
				return;
		}

		intensities = Utils.get2DImagePlusMovieAsFrameList(
				imagePlus,
				1,
				tMinOneBased,
				Math.min( tMaxOneBased, imagePlus.getNFrames() ) );
	}

	private ArrayList< RandomAccessibleInterval< T > > computeLabels( File segmentationFile )
	{
		final MicrogliaSegmentationAndTracking segmentationAndTracking = new MicrogliaSegmentationAndTracking( intensities, settings );

		if ( segmentationFile != null )
		{
			final ArrayList< RandomAccessibleInterval< IntType > > labels = openLabels( segmentationFile );

			segmentationAndTracking.setLabelings( labels );
		}

		segmentationAndTracking.run();

		final ArrayList< RandomAccessibleInterval< T > > labelings = segmentationAndTracking.getLabelings();

		return labelings;
	}

}
