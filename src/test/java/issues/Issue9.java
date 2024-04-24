package issues;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;

import java.io.File;

public class Issue9
{
    public static void main( String[] args )
    {
        ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        MicrogliaSegmentationAndTrackingCommand< ? > command = new MicrogliaSegmentationAndTrackingCommand<>();
        command.opService = imageJ.op();
        command.intensityFile = new File("/Users/tischer/Desktop/microglia/downscaled-calibration.tif");
        //command.intensityFile = new File("/Users/tischer/Desktop/microglia/MAX_pg6-3CF1_20--t1-3.tif");
        command.outputDirectory = new File("/Users/tischer/Desktop/microglia");
        command.run();
    }
}
