package test;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestNewSegmentationCommandSingleFrame
{
    @Test
	public void run()
	{
		final ImageJ ij = new ImageJ();

		final MicrogliaSegmentationAndTrackingCommand command = new MicrogliaSegmentationAndTrackingCommand();
		command.opService = ij.op();
		command.intensityFile = new File("src/test/resources/data/MAX_pg22_1C8hF1--t1.tif");
		command.outputDirectory = new File( "src/test/resources/data/output" );
		command.relativeIntensityThreshold = 1.5;
		command.run();
	}
}
