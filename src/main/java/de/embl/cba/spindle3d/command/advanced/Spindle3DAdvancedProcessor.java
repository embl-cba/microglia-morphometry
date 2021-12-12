package de.embl.cba.spindle3d.command.advanced;

import de.embl.cba.spindle3d.command.Spindle3DProcessor;
import org.scijava.plugin.Parameter;

public class Spindle3DAdvancedProcessor extends Spindle3DProcessor
{
	@Parameter ( label = "Voxel size for analysis" )
	public double voxelSizeForAnalysis = settings.voxelSizeForAnalysis;

	@Parameter ( label = "Minimum dynamic range [gray value]" )
	public int minimalDynamicRange = settings.minimalDynamicRange;

	@Parameter ( label = "Axial spindle poles refinement search radius [um]" )
	public double axialPoleRefinementRadius = settings.axialPoleRefinementRadius;

	@Parameter ( label = "Lateral spindle poles refinement search radius [um]" )
	public double lateralPoleRefinementRadius = settings.lateralPoleRefinementRadius;

	@Parameter ( label = "Smooth spindle" )
	public boolean smoothSpindle = settings.smoothSpindle;

	@Parameter ( label = "Show intermediate images" )
	public boolean showIntermediateImages = false;

	@Parameter ( label = "Show intermediate plots" )
	public boolean showIntermediatePlots = false;

	public boolean saveResults = true;

	protected void setAdvancedSettings()
	{
		setSettings();

		settings.showIntermediatePlots = showIntermediatePlots;
		settings.showIntermediateImages = showIntermediateImages;
		settings.smoothSpindle = smoothSpindle;
		settings.voxelSizeForAnalysis = voxelSizeForAnalysis;
		settings.axialPoleRefinementRadius = axialPoleRefinementRadius;
		settings.lateralPoleRefinementRadius = lateralPoleRefinementRadius;
		settings.minimalDynamicRange = minimalDynamicRange;
	}
}
