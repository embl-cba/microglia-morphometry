package de.embl.cba.microglia.command;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Microglia>Continue Microglia Segmentation And Tracking" )
public class MicrogliaSegmentationAndTrackingContinuationCommand< T extends RealType<T> & NativeType< T > > extends MicrogliaSegmentationAndTrackingCommand
{
	@Parameter( label = "Label mask time series (single channel 2D+t)", required = false )
	public File segmentationFile;

	@Override
	public void run()
	{
		setSettings();
		processFile( intensityFile, segmentationFile );
	}
}
