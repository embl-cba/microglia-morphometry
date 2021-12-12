package test;

import de.embl.cba.spindle3d.command.Spindle3DFileProcessorCommand;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;

public class TestWithCellMask
{
	public static void main( String[] args )
	{
		new TestWithCellMask().test();
	}

	@Test
	public void test()
	{
		DebugTools.setRootLevel("OFF");

		final ImageJ ij = new ImageJ();

		final Spindle3DFileProcessorCommand command = new Spindle3DFileProcessorCommand();
		command.opService = ij.op();
		command.scriptService = ij.script();

		// Spindle touching objects
		command.inputImageFile = new File("src/test/resources/test/with-cell-mask/20210204_HeLa_MCB309_006-1.tif" );

		command.outputDirectory = new File( "src/test/resources/test/output" );
		command.spindleChannelIndexOneBased = 1;
		command.dnaChannelIndexOneBased = 2;
		command.showIntermediateImages = false;
		command.showIntermediatePlots = false;
		command.saveResults = false;
		command.run();
	}
}
