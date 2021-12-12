package develop;

import net.imagej.ImageJ;
import net.imglib2.type.numeric.RealType;

public class TrySpindleMorphometryWithCATS
{
	public static < R extends RealType< R > > void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

//		final Spindle3DCommand< R > command = new Spindle3DCommand<>();
//		command.opService = ij.op();
//
//		command.inputImageFile = new File(
//				"/Users/tischer/Documents/fiji-plugin-morphometry/src/test/resources/test-data/spindle/SpindleLength9.5.zip" );
//
//		command.spindleChannelIndexOneBased = 1;
//		command.dnaChannelIndexOneBased = 2;
//		command.voxelSpacingDuringAnalysis = 0.25;
//		command.settings.showMetaphaseClassification = true;
//		command.useCATS = true;
//		command.classifier = new File("/Users/tischer/Documents/spindle-feedback-kletter-knime/CATS/3D_iso0.25um_ch1Tub_ch2DNA_8Bit.classifier" );
//		command.showIntermediateResults = false;
//		command.saveResults = false;
//		command.settings.showOutputImage = true;
//
//		command.run();
	}
}
