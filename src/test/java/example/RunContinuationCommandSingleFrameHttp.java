package example;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingContinuationCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RunContinuationCommandSingleFrameHttp
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final MicrogliaSegmentationAndTrackingContinuationCommand command = new MicrogliaSegmentationAndTrackingContinuationCommand();
		command.opService = ij.op();
		command.intensityFile = new File("https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg22_1C8hF1--t1.tif");
		command.segmentationFile =  new File("https://github.com/embl-cba/microglia-morphometry/raw/main/src/test/resources/data/MAX_pg22_1C8hF1-labelMasks--t1.tif");
		command.outputDirectory = new File( "src/test/resources/data" );
		command.relativeIntensityThreshold = 1.5;
		command.run();
	}
}
