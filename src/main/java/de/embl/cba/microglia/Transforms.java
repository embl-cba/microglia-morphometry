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
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
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
import java.util.List;

public abstract class Transforms
		< T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
{
    public static RealTransform translationAsRealTransform( double[] translation )
    {
        if ( translation.length == 1 ) return null;

        if ( translation.length == 2 ) return new Translation2D( translation );

        if ( translation.length == 3 ) return new Translation3D( translation );

        return new Translation( translation );
    }

	public static AffineTransform3D rotationAroundCenterTransform(
			double angle,
			int axis,
			double[] center )
	{
		double[] translationFromCenterToOrigin = new double[ 3 ];
		double[] translationFromOriginToCenter = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationFromCenterToOrigin[ d ] = - center[ d ];
			translationFromOriginToCenter[ d ] = + center[ d ];
		}

		final AffineTransform3D transform3D = new AffineTransform3D();
		transform3D.translate( translationFromCenterToOrigin );

		transform3D.rotate( axis, angle );

		final AffineTransform3D transformOriginToCenter = new AffineTransform3D();
		transformOriginToCenter.translate( translationFromOriginToCenter );

		transform3D.preConcatenate( transformOriginToCenter );
		return transform3D;
	}

    public static < T extends InvertibleRealTransform & Concatenable< T > & PreConcatenable< T > >
    RealTransform createIdentityAffineTransformation( int numDimensions )
    {
        if ( numDimensions == 2 )
        {
            return (T) new AffineTransform2D();
        }
        else if ( numDimensions == 3 )
        {
            return (T) new AffineTransform3D();
        }
        else
        {
            return (T) new AffineTransform( numDimensions );
        }
    }

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

	public static < T extends NumericType< T > >
	RandomAccessible createTransformedRaView(
			RandomAccessibleInterval< T > rai,
			InvertibleRealTransform combinedTransform,
			InterpolatorFactory interpolatorFactory,
			BorderExtension borderExtension )
	{
		ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > source = createExtendedRai( rai, borderExtension );
		RealRandomAccessible rra = Views.interpolate( source, interpolatorFactory );
		rra = RealViews.transform( rra, combinedTransform );
		return Views.raster( rra );
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

	private static void adjustBoundingRange( long[] min, long[] max, double[] transformedCorner )
	{
		for ( int d = 0; d < transformedCorner.length; ++d )
		{
			if ( transformedCorner[ d ] > max[ d ] )
			{
				max[ d ] = (long) transformedCorner[ d ];
			}

			if ( transformedCorner[ d ] < min[ d ] )
			{
				min[ d ] = (long) transformedCorner[ d ];
			}
		}
	}

	private static double[] transformedCorner( InvertibleRealTransform transform, long[] corner )
	{
		double[] cornerAsDouble = Arrays.stream( corner ).mapToDouble( x -> x ).toArray();
		double[] transformedCorner = new double[ corner.length ];
		transform.apply( cornerAsDouble, transformedCorner );
		return transformedCorner;
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

	public static FinalInterval scaleIntervalInXY( Interval interval, Scale scale )
	{
		int n = interval.numDimensions();

		long[] min = new long[ n ];
		long[] max = new long[ n ];
		interval.min( min );
		interval.max( max );

		for ( int d = 0; d < 2; ++d )
		{
			min[ d ] *= scale.getScale( d );
			max[ d ] *= scale.getScale( d );
		}

		return new FinalInterval( min, max );
	}

	public static FinalInterval getRealIntervalAsIntegerInterval( FinalRealInterval realInterval )
	{
		int n = realInterval.numDimensions();

		double[] realMin = new double[ n ];
		double[] realMax = new double[ n ];
		realInterval.realMin( realMin );
		realInterval.realMax( realMax );

		long[] min = new long[ n ];
		long[] max = new long[ n ];

		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = (long) realMin[ d ];
			max[ d ] = (long) realMax[ d ];
		}

		return new FinalInterval( min, max );
	}

	public static AffineTransform3D getTransformToIsotropicRegistrationResolution( double binning, double[] calibration )
	{
		double[] downScaling = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			downScaling[ d ] = calibration[ d ] / binning;
		}

		final AffineTransform3D scalingTransform = createScalingTransform( downScaling );

		return scalingTransform;
	}


	public static AffineTransform3D getScalingTransform( double calibration, double targetResolution )
	{

		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d = 0; d < 3; ++d )
		{
			scaling.set( calibration / targetResolution, d, d );
		}

		return scaling;
	}


	public static <T extends RealType<T> & NativeType< T > >
	ArrayList< RandomAccessibleInterval< T > > transformAllChannels(
			RandomAccessibleInterval< T > images,
			AffineTransform3D registrationTransform,
			FinalInterval outputImageInterval )
	{
		ArrayList< RandomAccessibleInterval< T > > transformedChannels = new ArrayList<>(  );

		long numChannels = images.dimension( 3 );

		for ( int c = 0; c < numChannels; ++c )
		{
			final RandomAccessibleInterval< T > channel = Views.hyperSlice( images, 3, c );
			transformedChannels.add(  createTransformedView( channel, registrationTransform, outputImageInterval ) );
		}

		return transformedChannels;
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


	public static < T extends NumericType< T > & NativeType< T > >
	RandomAccessibleInterval createTransformedView( RandomAccessibleInterval< T > rai,
													InvertibleRealTransform combinedTransform,
													FinalInterval interval,
													InterpolatorFactory interpolatorFactory)
	{
		final RandomAccessible transformedRA = createTransformedRaView( rai, combinedTransform, interpolatorFactory );
		final RandomAccessibleInterval< T > transformedIntervalView = Views.interval( transformedRA, interval );

		return transformedIntervalView;
	}

	public static ArrayList< RealPoint > origin()
	{
		final ArrayList< RealPoint > origin = new ArrayList<>();
		origin.add( new RealPoint( new double[]{ 0, 0, 0 } ) );
		return origin;
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

	public static AffineTransform3D getScalingTransform( double[] calibration, double targetResolution )
	{

		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d = 0; d < 3; ++d )
		{
			scaling.set( calibration[ d ] / targetResolution, d, d );
		}

		return scaling;
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
	RandomAccessibleInterval< T > scaledView( RandomAccessibleInterval< T > input,
											  double[] scalingFactors )
	{
		// Convert to RealRandomAccessible such that we can obtain values at (infinite) non-integer coordinates
		RealRandomAccessible< T > rra = Views.interpolate( Views.extendBorder( input ), new NLinearInterpolatorFactory<>() );

		// Change scale such that we can sample from integer coordinates (for raster function below)
		Scale scale = new Scale( scalingFactors );
		RealRandomAccessible< T > rescaledRRA  = RealViews.transform( rra, scale );

		// Create view sampled at integer coordinates
		final RandomAccessible< T > rastered = Views.raster( rescaledRRA );

		// Put an interval to make it a finite "normal" image again
		final RandomAccessibleInterval< T > finiteRastered = Views.interval( rastered, createScaledInterval( input, scale ) );

		return finiteRastered;
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


	public static double[] getScale( AffineTransform3D sourceTransform )
	{
		// https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati

		final double[] calibration = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			final double[] vector = new double[ 3 ];
			for ( int i = 0; i < 3 ; i++ )
			{
				vector[ i ] = sourceTransform.get( d, i );
			}

			calibration[ d ] = LinAlgHelpers.length( vector );
		}
		return calibration;
	}

	public static AffineTransform3D rotationAroundIntervalCenterTransform(
			double angle,
			int axis,
			RealInterval interval )
	{

		final double[] center = getCenter( interval );

		double[] translationFromCenterToOrigin = new double[ 3 ];
		double[] translationFromOriginToCenter = new double[ 3 ];

		for ( int d = 0; d < 3; ++d )
		{
			translationFromCenterToOrigin[ d ] = - center[ d ];
			translationFromOriginToCenter[ d ] = + center[ d ];
		}

		final AffineTransform3D transform3D = new AffineTransform3D();
		transform3D.translate( translationFromCenterToOrigin );

		transform3D.rotate( axis, angle );

		final AffineTransform3D transformOriginToCenter = new AffineTransform3D();
		transformOriginToCenter.translate( translationFromOriginToCenter );

		transform3D.preConcatenate( transformOriginToCenter );
		return transform3D;
	}




}
