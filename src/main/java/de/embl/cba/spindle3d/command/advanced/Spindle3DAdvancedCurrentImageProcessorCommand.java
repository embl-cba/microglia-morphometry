package de.embl.cba.spindle3d.command.advanced;

import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>Spindle3D>Spindle3D Process Current Image (Advanced)..." )
public class Spindle3DAdvancedCurrentImageProcessorCommand extends Spindle3DAdvancedProcessor implements Command
{
	@Parameter ( label = "Input Image" )
	public ImagePlus inputImagePlus;

	public void run()
	{
		setAdvancedSettings();
		processImagePlus( inputImagePlus, null );
	}
}
