package test;

import de.embl.cba.microglia.command.MicrogliaMorphometryCommand;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestMorphometryMeasurements
{
	@Test
	public void run( )
	{
		DebugTools.setRootLevel( "off" );
		final ImageJ ij = new ImageJ();

		final MicrogliaMorphometryCommand command = new MicrogliaMorphometryCommand();
		command.opService = ij.op();
		command.intensityFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif");
		command.labelMaskFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3-labelMasks.tif");
		command.outputDirectory = new File( "src/test/resources/data/" );
		command.showIntermediateResults = false;
		command.run();
	}
}
