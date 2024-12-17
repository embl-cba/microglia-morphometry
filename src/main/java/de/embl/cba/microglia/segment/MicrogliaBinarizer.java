package de.embl.cba.microglia.segment;

import anisotropic_diffusion.Anisotropic_Diffusion_2D;
import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.Algorithms;
import de.embl.cba.microglia.morphometry.CoordinateAndValue;
import de.embl.cba.microglia.morphometry.IntensityHistogram;
import de.embl.cba.microglia.morphometry.regions.Regions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import static de.embl.cba.microglia.Transforms.getScalingFactors;
import static de.embl.cba.microglia.Utils.createRescaledArrayImg;
import static de.embl.cba.microglia.Utils.show;

public class MicrogliaBinarizer< T extends RealType< T > & NativeType< T > >
{
	final MicrogliaSettings settings;
	private RandomAccessibleInterval< BitType > mask;
	final private RandomAccessibleInterval< T > intensity;
	final private boolean showIntermediateResults;

	public MicrogliaBinarizer(
			RandomAccessibleInterval< T > intensity,
			MicrogliaSettings settings )
	{
		this.intensity = intensity;
		this.settings = settings;
		this.showIntermediateResults = settings.showIntermediateResults;
	}

	public void run()
	{
		/**
		 *  Create working image
		 */
		final double[] workingCalibration = Utils.as2dDoubleArray( settings.workingVoxelSize );

		// Below rescaling currently is doing nothing because
		// settings.workingVoxelSize = settings.calibration2D
		RandomAccessibleInterval< T > image = createRescaledArrayImg( intensity, getScalingFactors( settings.calibration2D, settings.workingVoxelSize ) );

		if ( showIntermediateResults )
		{
			// this was from imagej-utils the bdvViewer
			show( image, "rescaled image", null, workingCalibration, false );
		}


		/**
		 *  Smooth
		 */

		final ImagePlus wrap = ImageJFunctions.wrap( image, "" );
		final Anisotropic_Diffusion_2D diffusion2D = new Anisotropic_Diffusion_2D();
		diffusion2D.setup( "", wrap );
		final ImagePlus imagePlus = diffusion2D.runTD( wrap.getProcessor() );
		image = ImageJFunctions.wrapReal( imagePlus );

		if ( showIntermediateResults ) ImageJFunctions.show( image, "smoothed image" );

		/**
		 *  Compute offset and threshold
		 */

		final IntensityHistogram intensityHistogram = new IntensityHistogram( image, 65535, 2 );

		CoordinateAndValue mode = intensityHistogram.getMode();

		final CoordinateAndValue rightHandHalfMode = intensityHistogram.getRightHandHalfMode();

		double offset = mode.coordinate;
		double threshold = offset + ( rightHandHalfMode.coordinate - mode.coordinate ) * settings.thresholdInUnitsOfBackgroundPeakHalfWidth;

		IJ.log( "Intensity offset: " + offset );
		IJ.log( "Threshold: " + threshold );

		/**
		 * Create mask
		 */
		mask = Algorithms.createMask( image, threshold );

		if ( showIntermediateResults )
		{
			show( mask, "mask", null, workingCalibration, false );
		}


		/**
		 * Remove small objects from mask
		 */
		IJ.log( "Removing objects of an area less than " + settings.minimalObjectSize + " um^2..." );
		Regions.removeSmallRegionsInMask( mask, settings.minimalObjectSize, settings.workingVoxelSize );

		if ( showIntermediateResults )
		{
			show( mask, "size filtered mask", null, workingCalibration, false );
		}

	}

	public RandomAccessibleInterval< BitType > getMask()
	{
		return mask;
	}


}
