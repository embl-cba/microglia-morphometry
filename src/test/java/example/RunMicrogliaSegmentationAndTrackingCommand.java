package example;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RunMicrogliaSegmentationAndTrackingCommand
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final MicrogliaSegmentationAndTrackingCommand command = new MicrogliaSegmentationAndTrackingCommand();
		command.opService = ij.op();
		command.intensityFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif");
		command.outputDirectory = new File( "src/test/resources/data" );
		command.relativeIntensityThreshold = 1.5;
		command.run();
	}
}
