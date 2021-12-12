package de.embl.cba.spindle3d.command.samples;

import ij.IJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>Spindle3D>Spindle3D Download Example Image..." )
public class Spindle3DDownloadSampleImageCommand implements Command
{
	public static final String GITHUB_LOCATION = "https://github.com/tischi/spindle3d/raw/master/src/test/resources/test/references/";

	private final static String[] fileNames = new String[]{
			"NikonSD_40x_Ptk2_01.tif",
			"NikonSD_60x_HEK293SiRTubulin_01.tif",
			"NikonSD_60x_HeLa_02.tif",
			"NikonSD_60x_R1EmESC_01.tif",
			"NikonSD_100x_HeLa_02.tif",
			"NikonSD_100x_R1EmESC_01.tif",
			//"ZeissLSM_40x_CowZygote_01.tif",
			"ZeissLSM_40x_R1EmESC_01.tif"};

	@Parameter ( label = "Image", choices = {
			"NikonSD_40x_Ptk2_01.tif",
			"NikonSD_60x_HEK293SiRTubulin_01.tif",
			"NikonSD_60x_HeLa_02.tif",
			"NikonSD_60x_R1EmESC_01.tif",
			"NikonSD_100x_HeLa_02.tif",
			"NikonSD_100x_R1EmESC_01.tif",
			//"ZeissLSM_40x_CowZygote_01.tif",
			"ZeissLSM_40x_R1EmESC_01.tif"} )
	String fileName;

	public void run()
	{
		IJ.openImage( GITHUB_LOCATION +fileName ).show();
	}
}
