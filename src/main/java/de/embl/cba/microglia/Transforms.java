/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.embl.cba.microglia;

import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.concatenate.PreConcatenable;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Transforms
		< T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{

	public enum BorderExtension
	{
		ExtendZero,
		ExtendBorder,
		ExtendMirror
	}

	public static < T extends Type< T > >
	RandomAccessibleInterval< T > getWithAdjustedOrigin(
			Interval interval,
			RandomAccessibleInterval< T > rai )
	{
		long[] offset = new long[ interval.numDimensions() ];
		interval.min( offset );
		RandomAccessibleInterval translated = Views.translate( rai, offset );
		return translated;
	}

	private static < T extends NumericType< T > > ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > createExtendedRai( RandomAccessibleInterval< T > rai, BorderExtension borderExtension )
	{
		switch ( borderExtension )
		{
			case ExtendZero:
				return Views.extendZero( rai );
			case ExtendBorder:
				return Views.extendBorder( rai );
			case ExtendMirror:
				return Views.extendMirrorDouble( rai );
			default:
				return Views.extendZero( rai );
		}
	}

	public static < T extends NumericType< T > >
	RandomAccessible createTransformedRaView(
			RandomAccessibleInterval< T > rai,
			InvertibleRealTransform combinedTransform,
			InterpolatorFactory interpolatorFactory )
	{
		RealRandomAccessible rra = Views.interpolate( Views.extendZero( rai ), interpolatorFactory );
		rra = RealViews.transform( rra, combinedTransform );
		return Views.raster( rra );
	}


	public static FinalInterval createScaledInterval( Interval interval, Scale scale )
	{
		int n = interval.numDimensions();

		long[] min = new long[ n ];
		long[] max = new long[ n ];
		interval.min( min );
		interval.max( max );

		for ( int d = 0; d < n; ++d )
		{
			min[ d ] *= scale.getScale( d );
			max[ d ] *= scale.getScale( d );
		}

		return new FinalInterval( min, max );
	}


	public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval createTransformedView( RandomAccessibleInterval< T > rai,
													InvertibleRealTransform transform,
													FinalInterval interval )
	{
		final RandomAccessible transformedRA = createTransformedRaView( rai, transform, new NLinearInterpolatorFactory() );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, interval );

		return transformedIntervalView;
	}

	public static < T extends NumericType< T > >
	FinalInterval createTransformedInterval( RandomAccessibleInterval< T > rai, InvertibleRealTransform transform )
	{
		final FinalInterval transformedInterval;

		if ( transform instanceof AffineTransform3D )
		{
			FinalRealInterval transformedRealInterval = ( ( AffineTransform3D ) transform ).estimateBounds( rai );
			transformedInterval = asIntegerInterval( transformedRealInterval );
		}
		else if ( transform instanceof Scale )
		{
			transformedInterval = createScaledInterval( rai, ( Scale ) transform );
		}
		else
		{
			transformedInterval = null;
		}

		return transformedInterval;
	}

	public static FinalInterval asIntegerInterval( FinalRealInterval realInterval )
	{
		double[] realMin = new double[ 3 ];
		double[] realMax = new double[ 3 ];
		realInterval.realMin( realMin );
		realInterval.realMax( realMax );

		long[] min = new long[ 3 ];
		long[] max = new long[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			min[ d ] = (long) realMin[ d ];
			max[ d ] = (long) realMax[ d ];
		}

		return new FinalInterval( min, max );
	}


	public static double[] getScalingFactors( double[] calibration, double targetResolution )
	{

		double[] downScaling = new double[ calibration.length ];

		for ( int d = 0; d < calibration.length; ++d )
		{
			downScaling[ d ] = calibration[ d ] / targetResolution;
		}

		return downScaling;
	}


	public static AffineTransform3D createScalingTransform( double[] calibration )
	{
		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d = 0; d < 3; ++d )
		{
			scaling.set( calibration[ d ], d, d );
		}

		return scaling;
	}


	public static < T extends RealType< T > & NativeType< T > >
	double[] getCenter( RealInterval rai )
	{
		int numDimensions = rai.numDimensions();

		double[] center = new double[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
		{
			center[ d ] = ( rai.realMax( d ) - rai.realMin( d ) ) / 2 + rai.realMin( d );
		}

		return center;
	}


}
