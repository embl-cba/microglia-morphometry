package issues;

import de.embl.cba.microglia.command.MicrogliaMorphometryCommand;
import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;

import java.io.File;

public class Issue11
{
    public static void main( String[] args )
    {
        ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

//        MicrogliaSegmentationAndTrackingCommand< ? > command = new MicrogliaSegmentationAndTrackingCommand<>();
//        command.opService = imageJ.op();
//        command.intensityFile = new File("/Users/tischer/Desktop/microglia-data/testPluginFiji.tif");
//        command.intensityFile = new File("/Users/tischer/Desktop/microglia-data/test/test-crop-8bit-ds2.tif");
//        command.outputDirectory = new File("/Users/tischer/Desktop/microglia-data/test");
//        command.minimalMicrogliaSize = 300;
//        //command.relativeIntensityThreshold = 1.5;
//        command.run();

        /**
         * setOption("ScaleConversions", true);
         * run("8-bit");
         * run("Scale...", "x=0.5 y=0.5 width=1383 height=1383 interpolation=Bilinear average create title=scale05");
         * run("Grays");
         * saveAs("Tiff", "/Users/tischer/Desktop/microglia-data/scale05.tif");
         */

        MicrogliaMorphometryCommand morphometryCommand = new MicrogliaMorphometryCommand();
        morphometryCommand.opService = imageJ.op();
        morphometryCommand.intensityFile = new File("/Users/tischer/Desktop/microglia-data/test/test-crop-8bit-ds2.tif");
        morphometryCommand.labelMaskFile = new File("/Users/tischer/Desktop/microglia-data/test/test-crop-8bit-ds2-labelMasks.tif");
        morphometryCommand.outputDirectory = new File("/Users/tischer/Desktop/microglia-data/test");
        morphometryCommand.run();
    }
}
