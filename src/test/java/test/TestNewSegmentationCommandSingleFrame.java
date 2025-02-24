package test;

import de.embl.cba.microglia.command.MicrogliaSegmentationAndTrackingCommand;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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
		command.outputDirectory = new File( "src/test/resources/tmp-data" );
		command.relativeIntensityThreshold = 1.5;
		command.minimalMicrogliaSize = 200; // area in um
		command.skeletonMaxLength = 200; // length in um
		command.headless = true;
		command.run();

		ImagePlus refLabels = IJ.openImage( new File( "src/test/resources/data/MAX_pg22_1C8hF1--t1-labelMasks.tif" ).toString() );
		ImagePlus testLabels = IJ.openImage( new File( "src/test/resources/tmp-data/MAX_pg22_1C8hF1--t1-labelMasks.tif" ).toString() );

		assert areImagesIdentical( refLabels, testLabels );
	}

	public static boolean areImagesIdentical(ImagePlus img1, ImagePlus img2)
	{

		if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
			return false;
		}

		ImageProcessor ip1 = img1.getProcessor();
		ImageProcessor ip2 = img2.getProcessor();

		for (int y = 0; y < img1.getHeight(); y++) {
			for (int x = 0; x < img1.getWidth(); x++) {
				if (ip1.getPixel(x, y) != ip2.getPixel(x, y)) {
					return false;
				}
			}
		}
		return true;
	}
}
