package test;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestNewSegmentationCommandMultipleFrames
{
    @Test
	public void run()
	{
		final ImageJ ij = new ImageJ();

		final MicrogliaSegmentationAndTrackingCommand command = new MicrogliaSegmentationAndTrackingCommand();
		command.opService = ij.op();
		command.intensityFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif");
		command.outputDirectory = new File( "src/test/resources/test-data" );
		command.relativeIntensityThreshold = 1.5;
		command.run();
	}
}
