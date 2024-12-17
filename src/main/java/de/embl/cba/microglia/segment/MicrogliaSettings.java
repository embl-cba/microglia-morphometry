package de.embl.cba.microglia.segment;

import de.embl.cba.microglia.Utils;
import ij.measure.Calibration;
import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.File;

public class MicrogliaSettings<T extends RealType<T> & NativeType< T > >
{
	public static final String MANUAL_THRESHOLD = "Manual threshold";
	public static final String HUANG_AUTO_THRESHOLD = "Huang auto threshold";

	// all spatial values are in micrometer
	// morphometry length: 420
	// morphometry width: 160

	public OpService opService;

	public boolean showIntermediateResults = false;
	public double workingVoxelSize;
	public double outputResolution = 2.0;

	public double watershedSeedsGlobalDistanceThreshold = Double.MAX_VALUE;
	public double watershedSeedsLocalMaximaDistanceThreshold = 3 * workingVoxelSize; // at least 3 pixels

	public String thresholdModality = MANUAL_THRESHOLD;
	public double thresholdInUnitsOfBackgroundPeakHalfWidth = 1.5;
	public double closingRadius = 3.0;

	public double[] calibration2D;
	public Calibration calibration;

	public double maxShortAxisDist;
	public double interestPointsRadius;

	public File outputDirectory;
	public String inputDataSetName;
	public boolean returnEarly;
	public Double minimalObjectSize;
	public Double minimalTrackingSplittingObjectArea;
	public Double skeletonMaxLength;
	public boolean splitTouchingObjects = false;
	public double minimalObjectCenterDistance;
	public double maximalWatershedLength;
	public double minimalOverlapFraction = 0.05;
	public double minimalSumIntensityRatio = 0.5;
	public double maximalSumIntensityRatio = 1.5;
	public boolean manualSegmentationCorrection = true;

	public String outputLabelingsPath;

	/**
		// TODO: make a proper constructor with the missing settings...
	 * 		microgliaSettings.opService = ij.op();
	 microgliaSettings.calibration = imagePlus.getCalibration();
	 microgliaSettings.outputDirectory = new File( "" );
	 */
	public static MicrogliaSettings addMissingSettings( MicrogliaSettings settings )
	{
		if ( settings.skeletonMaxLength == null )
			settings.skeletonMaxLength = 450.0; // um
		if ( settings.minimalObjectSize == null )
			settings.minimalObjectSize = 200.0; // um2

		settings.calibration2D = Utils.get2dCalibration( settings.calibration );
		settings.workingVoxelSize = settings.calibration2D[ 0 ]; // TODO: why not use something standard here?
		settings.maxShortAxisDist = 6;
		settings.watershedSeedsLocalMaximaDistanceThreshold = Double.MAX_VALUE;
		settings.watershedSeedsGlobalDistanceThreshold = 2.5;
		settings.interestPointsRadius = 0.5;
		settings.outputDirectory = null; //new File( path ).getParentFile();
		settings.inputDataSetName = "test";
		settings.returnEarly = true;
		settings.minimalTrackingSplittingObjectArea = 20.0; // um2, this can be very small
		settings.minimalObjectCenterDistance = 6;
		settings.maximalWatershedLength = 10;
		settings.closingRadius = 3;

		return settings;
	}
}
