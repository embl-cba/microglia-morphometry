package develop;

import loci.common.DebugTools;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TestSpindleAxialExtendUnderestimation
{
	public static < R extends RealType< R > > void main( String[] args )
	{
		DebugTools.setRootLevel("OFF"); // Bio-Formats

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final Spindle3DFileProcessorCommand command = new Spindle3DFileProcessorCommand();
		command.opService = ij.op();

//		command.inputImageFile = new File( "/Users/tischer/Downloads/Incorrect-t82-crop.tif" );
//		command.spindleChannelIndexOneBased = 1;
//		command.dnaChannelIndexOneBased = 2;

		command.inputImageFile = new File( "/Users/tischer/Downloads/191003-cow44th-8003-M4MTBD488-H2B568-Gauss-Scene-01-t92-1.tif" );
		command.spindleChannelIndexOneBased = 1;
		command.dnaChannelIndexOneBased = 2;

		command.showIntermediateImages = true;
		command.saveResults = false;
		command.settings.showOutputImage = true;
		command.outputDirectory = new File("/Users/tischer/Downloads" );
		command.run();

		final HashMap< Integer, Map< String, Object > > measurements =
				command.getObjectMeasurements();
	}
}


