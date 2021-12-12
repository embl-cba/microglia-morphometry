package de.embl.cba.spindle3d;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.measure.Calibration;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.io.File;

public class Spindle3DSettings<T extends RealType<T> & NativeType< T > >
{
	public String version;

	/**
	 * Spatial
	 */
	public double voxelSizeForAnalysis = 0.25; // um
	public double metaphasePlateWidthDerivativeDelta = 1.0; // um
	public double metaphasePlateLengthDerivativeDelta = 2.0; // um
	public double spindleFragmentInclusionZone = 3.0; // um;
	public double axialPoleRefinementRadius = 1.0; // um
	public double lateralPoleRefinementRadius = 2.0; // um
	public double voxelSizeForInitialDNAThreshold = 1.5; // um

	/**
	 * Intensity
	 */
	public double initialDnaThresholdFactor = 0.5;
	public int minimalDynamicRange = 7;

	/**
	 * Other
	 */
	public transient boolean showIntermediateImages = false;
	public transient boolean showIntermediatePlots = false;
	public double[] inputVoxelSize;
	public transient File outputDirectory;
	public transient String inputDataSetName;
	public transient Calibration imagePlusCalibration;
	public long dnaChannelIndex;
	public long tubulinChannelIndex;
	public transient boolean showOutputImage = false;
	public transient File roiDetectionMacro;
	public transient boolean smoothSpindle = false;
	public double[][] manualSpindleAxisPositions;
	public transient RandomAccessibleInterval< BitType > cellMask;

	public String toString()
	{
		String s = "\n## Spindle3D Settings\n";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		s += gson.toJson( this );
		s += "\n";
		return s;
	}
}
