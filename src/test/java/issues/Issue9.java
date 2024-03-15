package issues;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import net.imagej.ImageJ;

import java.io.File;

public class Issue9
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();

        MicrogliaSegmentationAndTrackingCommand< ? > command = new MicrogliaSegmentationAndTrackingCommand<>();
        command.intensityFile = new File("/Users/tischer/Desktop/microglia/image.png");
        command.intensityFile = new File("/Users/tischer/Desktop/microglia/MAX_pg6-3CF1_20--t1-3.tif");
        command.outputDirectory = new File("/Users/tischer/Desktop/microglia");
        command.run();
    }
}
