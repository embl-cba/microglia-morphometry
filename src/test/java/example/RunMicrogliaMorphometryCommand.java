package example;

import de.embl.cba.microglia.command.MicrogliaMorphometryCommand;
import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RunMicrogliaMorphometryCommand
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final MicrogliaMorphometryCommand command = new MicrogliaMorphometryCommand();
		command.opService = ij.op();
		command.intensityFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif");
		command.labelMaskFile = new File("src/test/resources/data/MAX_pg6-3CF1_20-labelMasks--t1-3.tif");
		command.outputDirectory = new File( "src/test/resources/data/output" );
		command.showIntermediateResults = false;
		command.run();
	}
}
