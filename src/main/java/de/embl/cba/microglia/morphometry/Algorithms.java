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
package de.embl.cba.microglia.morphometry;

import de.embl.cba.microglia.Transforms;
import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.geometry.CoordinateToValue;
import de.embl.cba.microglia.morphometry.geometry.CoordinatesAndValues;
import de.embl.cba.microglia.morphometry.regions.Regions;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imagej.ops.threshold.huang.ComputeHuangThreshold;
import net.imagej.ops.threshold.otsu.ComputeOtsuThreshold;
import net.imagej.ops.threshold.yen.ComputeYenThreshold;
import net.imglib2.RandomAccess;
import net.imglib2.*;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.morphology.Closing;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.morphology.Opening;
import net.imglib2.algorithm.morphology.distance.DistanceTransform;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.converter.Converters;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.*;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.*;

import static de.embl.cba.microglia.Transforms.createTransformedInterval;
import static de.embl.cba.microglia.Utils.copyAsArrayImg;

public class Algorithms
{
	public static final int WATERSHED = -1;
	private static int closingRadius;

	public static < T extends RealType< T > & NativeType< T > >
	RealPoint getMaximumLocation(
			RandomAccessibleInterval< T > rai,
			double[] calibration )
	{
		return getMaximumLocation( Views.iterable( rai ), calibration );
	}

	public static < T extends RealType< T > & NativeType< T > >
	RealPoint getMaximumLocation(
			IterableInterval< T > ii,
			double[] calibration )
	{
		Cursor< T > cursor = ii.localizingCursor();

		double maxValue = - Double.MAX_VALUE;

		long[] maxLoc = new long[ cursor.numDimensions() ];
		cursor.localize( maxLoc );

		while ( cursor.hasNext() )
		{
			final double value = cursor.next().getRealDouble();
			if ( value > maxValue )
			{
				maxValue = value;
				cursor.localize( maxLoc );
			}
		}

		double[] calibratedMaxLoc = new double[ maxLoc.length ];
		for ( int d = 0; d < ii.numDimensions(); ++d )
			if ( calibration != null )
				calibratedMaxLoc[ d ] = maxLoc[ d ] * calibration[ d ];
			else
				calibratedMaxLoc[ d ] = maxLoc[ d ];

		RealPoint point = new RealPoint( calibratedMaxLoc );

		return point;
	}

	public static < T extends RealType< T > >
	Double getMaximumValue( RandomAccessibleInterval< T > rai )
	{
		Cursor< T > cursor = Views.iterable( rai ).cursor();

		double maxValue = Double.MIN_VALUE;

		double value;
		while ( cursor.hasNext() )
		{
			value = cursor.next().getRealDouble();
			if ( value > maxValue )
				maxValue = value;
		}

		return maxValue;
	}

	public static < T extends RealType< T > >
	Pair< Double, Double > getMinMaxValues( RandomAccessibleInterval< T > rai )
	{
		Cursor< T > cursor = Views.iterable( rai ).localizingCursor();

		double maxValue = - Double.MAX_VALUE;
		double minValue = Double.MAX_VALUE;

		while ( cursor.hasNext() )
		{
			final double value = cursor.next().getRealDouble();

			if ( value > maxValue ) maxValue = value;
			if ( value < minValue ) minValue = value;
		}

		return new ValuePair<>( minValue, maxValue );
	}


	public static < T extends RealType< T > & NativeType< T > >
	boolean isCenterLargest( T center, Neighborhood< T > neighborhood )
	{
		boolean centerIsLargest = true;

		for( T neighbor : neighborhood ) {
			if( neighbor.compareTo( center ) > 0 )
			{
				centerIsLargest = false;
				break;
			}
		}

		return centerIsLargest;
	}

	public static < T extends RealType< T > & NativeType< T > >
	List< RealPoint > findLocalMaximumValues( RandomAccessibleInterval< T > rai, Shape shape )
	{
		List< RealPoint > points = new ArrayList<>();

		RandomAccessible<Neighborhood<T>> neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendBorder( rai ) );
		RandomAccessibleInterval<Neighborhood<T>> neighborhoodsInterval = Views.interval( neighborhoods, rai );

		LoopBuilder.setImages( neighborhoodsInterval, rai ).forEachPixel(
				(neighborhood, center) -> {
					if( isCenterLargest( center, neighborhood ) )
					{
						points.add( new RealPoint( neighborhood ) );
					}
				}
		);

		return points;
	}

	public static Set< Integer > getCentralLabels(
			ImgLabeling< Integer, IntType > labeling,
			double[] center,
			long maxCenterDistance )
	{

		if ( labeling.getMapping().numSets() == 0 )
			return new HashSet<>(  );

		final HyperSphereShape sphere = new HyperSphereShape( maxCenterDistance );

		final RandomAccessible< Neighborhood< IntType > > nra =
				sphere.neighborhoodsRandomAccessible( Views.extendZero( labeling.getIndexImg() ) );

		final RandomAccess< Neighborhood< IntType > > neighborhoodRandomAccess =
				nra.randomAccess();

		neighborhoodRandomAccess.setPosition( Utils.asLongs( center ) );

		final Cursor< IntType > cursor = neighborhoodRandomAccess.get().cursor();

		final Set< Integer > centralIndices = new HashSet<>();
		while( cursor.hasNext() )
		{
			if ( cursor.next().get() != 0 )
			{
				centralIndices.add( cursor.get().getInteger() );
			}
		}

		final Set< Integer > centralLabels = new HashSet<>();
		for ( int index : centralIndices )
		{
			final ArrayList< Integer > labels =
					new ArrayList<>( labeling.getMapping().labelsAtIndex( index ) );
			centralLabels.add( labels.get( 0 ) );
		}

		return centralLabels;
	}


	public static Img< UnsignedByteType > createUnsignedByteTypeMaskFromLabelRegion( LabelRegion< Integer > centralObjectRegion, long[] dimensions )
	{
		final Img< UnsignedByteType > centralObjectImg = ArrayImgs.unsignedBytes( dimensions );

		final Cursor< Void > regionCursor = centralObjectRegion.inside().cursor();
		final RandomAccess< UnsignedByteType > access = centralObjectImg.randomAccess();
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().set( 255 );
		}
		return centralObjectImg;
	}


	public static Img< BitType > createMaskFromLabelRegion( LabelRegion< Integer > region, long[] dimensions )
	{
		final Img< BitType > centralObjectImg = ArrayImgs.bits( dimensions );

		final Cursor< Void > regionCursor = region.inside().cursor();
		final RandomAccess< BitType > access = centralObjectImg.randomAccess();
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().set( true );
		}
		return centralObjectImg;
	}


	public static ArrayList< RealPoint > origin()
	{
		final ArrayList< RealPoint > origin = new ArrayList<>();
		origin.add( new RealPoint( new double[]{ 0, 0, 0 } ) );
		return origin;
	}


	public static < T extends RealType< T > & NativeType< T > >
	void copy( RandomAccessibleInterval< T > source, RandomAccessibleInterval< T > target )
	{
		final Cursor< T > cursor = Views.iterable( source ).cursor();
		final RandomAccess< T > randomAccess = target.randomAccess();

		while( cursor.hasNext() )
		{
			cursor.fwd();
			randomAccess.setPosition( cursor );
			randomAccess.get().set( cursor.get() );
		}

	}


	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > createMask(
			RandomAccessibleInterval< T > rai,
			double threshold )
	{
		RandomAccessibleInterval< BitType > mask =
				Converters.convert( rai, ( i, o )
						-> o.set( i.getRealDouble() > threshold ? true : false ), new BitType() );

		return copyAsArrayImg( mask );
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > fillHoles3Din2D(
			RandomAccessibleInterval< BitType > mask,
			int axis,
			OpService opService )
	{
		final ArrayList< RandomAccessibleInterval< BitType > > holesFilled = new ArrayList<>();

		final IntervalView< BitType > rotated = Views.rotate( mask, axis, 2 );

		for ( long coordinate = rotated.min( 2 ); coordinate <= rotated.max( 2 ); ++coordinate )
		{
			RandomAccessibleInterval< BitType > maskSlice = Views.hyperSlice( rotated, 2, coordinate );
			holesFilled.add( opService.morphology().fillHoles( maskSlice ) );
		}

		RandomAccessibleInterval< BitType > stack =  Views.stack( holesFilled );

		stack = Views.zeroMin( Views.rotate( stack, 2, axis ) );

		stack = Transforms.getWithAdjustedOrigin( mask, stack );

		return stack;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > createWatershedSeeds(
			RandomAccessibleInterval< T > distance,
			Shape shape,
			double globalThreshold,
			double localThreshold )
	{
		RandomAccessibleInterval< BitType > seeds = ArrayImgs.bits( Intervals.dimensionsAsLongArray( distance ) );
		seeds = Transforms.getWithAdjustedOrigin( distance, seeds );

		RandomAccessible< Neighborhood< T > > neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendPeriodic( distance ) );
		RandomAccessibleInterval< Neighborhood< T > > neighborhoodsInterval = Views.interval( neighborhoods, distance );

		final Cursor< Neighborhood< T > > neighborhoodCursor = Views.iterable( neighborhoodsInterval ).cursor();
		final RandomAccess< T > distanceRandomAccess = distance.randomAccess();
		final RandomAccess< BitType > seedsRandomAccess = seeds.randomAccess();

		double[] currentPosition = new double[ distance.numDimensions() ];

		while ( neighborhoodCursor.hasNext() )
		{
			final Neighborhood< T > neighborhood = neighborhoodCursor.next();
			neighborhood.localize( currentPosition );
			seedsRandomAccess.setPosition( neighborhood );
			distanceRandomAccess.setPosition( neighborhood );

			T centerValue = distanceRandomAccess.get();

			if ( centerValue.getRealDouble() > globalThreshold )
			{
				seedsRandomAccess.get().set( true );
			}
			else if ( Utils.isLateralBoundaryPixel( neighborhood, distance ) && distanceRandomAccess.get().getRealDouble() >  0 )
			{
				seedsRandomAccess.get().set( true );
			}
			else if ( isCenterLargestOrEqual( centerValue, neighborhood ) )
			{
				if ( centerValue.getRealDouble() > localThreshold )
				{
					seedsRandomAccess.get().set( true );
				}
			}

		}

		return seeds;
	}


	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > createLocalMaximaMask(
			RandomAccessibleInterval< T > input,
			Shape shape,
			double minimumValue )
	{
		RandomAccessibleInterval< BitType > localMaxima = ArrayImgs.bits( Intervals.dimensionsAsLongArray( input ) );
		localMaxima = Transforms.getWithAdjustedOrigin( input, localMaxima );

		RandomAccessible< Neighborhood< T > > neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendPeriodic( input ) );
		RandomAccessibleInterval< Neighborhood< T > > neighborhoodsInterval = Views.interval( neighborhoods, input );

		final Cursor< Neighborhood< T > > neighborhoodCursor = Views.iterable( neighborhoodsInterval ).cursor();
		final RandomAccess< T > inputAccess = input.randomAccess();
		final RandomAccess< BitType > maximaAccess = localMaxima.randomAccess();

		while ( neighborhoodCursor.hasNext() )
		{
			final Neighborhood< T > neighborhood = neighborhoodCursor.next();
			maximaAccess.setPosition( neighborhood );
			inputAccess.setPosition( neighborhood );

			T centerValue = inputAccess.get();

			if ( isCenterLargestOrEqual( centerValue, neighborhood ) )
			{
				if ( centerValue.getRealDouble() > minimumValue )
				{
					maximaAccess.get().set( true );
				}
			}
			else
			{
				maximaAccess.get().set( false );
			}

		}

		return localMaxima;
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > computeGradient(
			RandomAccessibleInterval< R > input,
			Shape shape )
	{
		RandomAccessible< Neighborhood< R > > neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendBorder( input ) );
		RandomAccessibleInterval< Neighborhood< R > > neighborhoodsInterval = Views.interval( neighborhoods, input );

		final Cursor< Neighborhood< R > > neighborhoodCursor = Views.iterable( neighborhoodsInterval ).cursor();
		final RandomAccess< R > inputAccess = input.randomAccess();

		final RandomAccessibleInterval< R > gradient = copyAsArrayImg( input );
		final RandomAccess< R > gradientAccess = gradient.randomAccess();

		while ( neighborhoodCursor.hasNext() )
		{
			final Neighborhood< R > neighborhood = neighborhoodCursor.next();

			inputAccess.setPosition( neighborhood );
			gradientAccess.setPosition( neighborhood );

			double centerValue = inputAccess.get().getRealDouble();
			final double minimum = computeMinimum( neighborhood );

			gradientAccess.get().setReal( centerValue - minimum );
		}

		return gradient;
	}



	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > createCenterAndBoundarySeeds( RandomAccessibleInterval< T > distance, Shape shape, double globalThreshold, double localThreshold )
	{
		RandomAccessibleInterval< BitType > seeds = ArrayImgs.bits( Intervals.dimensionsAsLongArray( distance ) );
		seeds = Transforms.getWithAdjustedOrigin( distance, seeds );

		RandomAccessible< Neighborhood< T > > neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendPeriodic( distance ) );
		RandomAccessibleInterval< Neighborhood< T > > neighborhoodsInterval = Views.interval( neighborhoods, distance );

		final Cursor< Neighborhood< T > > neighborhoodCursor = Views.iterable( neighborhoodsInterval ).cursor();
		final RandomAccess< T > distanceRandomAccess = distance.randomAccess();
		final RandomAccess< BitType > seedsRandomAccess = seeds.randomAccess();

		double[] centerPosition = new double[ distance.numDimensions() ];
		double[] currentPosition = new double[ distance.numDimensions() ];

		for ( int d = 0; d < centerPosition.length; ++d )
		{
			centerPosition[ d ] = (long) (distance.dimension( d ) / 2);
		}

		while ( neighborhoodCursor.hasNext() )
		{
			final Neighborhood< T > neighborhood = neighborhoodCursor.next();
			neighborhood.localize( currentPosition );
			seedsRandomAccess.setPosition( neighborhood );
			distanceRandomAccess.setPosition( neighborhood );

			T centerValue = distanceRandomAccess.get();

			if ( centerValue.getRealDouble() > globalThreshold )
			{
				// maximaRandomAccess.get().set( true );
			}
			else if ( LinAlgHelpers.distance( currentPosition, centerPosition  ) < 3 )
			{
				seedsRandomAccess.get().set( true );
			}
			else if ( Utils.isLateralBoundaryPixel( neighborhood, distance ) && distanceRandomAccess.get().getRealDouble() >  0 )
			{
				seedsRandomAccess.get().set( true );
			}
//			else if ( isCenterLargestOrEqual( centerValue, neighborhood ) )
//			{
//				if ( centerValue.getRealDouble() > localThreshold )
//				{
//					// local maximum and larger than local Threshold
//					// maximaRandomAccess.get().set( true );
//				}
//			}

		}

		return seeds;
	}


	public static < T extends RealType< T > & NativeType< T > >
	ArrayList< PositionAndValue > getLocalMaxima(
			RandomAccessibleInterval< T > rai,
			double minimalDistanceBetweenMaxima,
			double threshold )
	{
		Shape shape = new HyperSphereShape( (long) minimalDistanceBetweenMaxima );
		RandomAccessible< Neighborhood< T > > neighborhoods = shape.neighborhoodsRandomAccessible( Views.extendPeriodic( rai ) );
		final Cursor< Neighborhood< T > > neighborhoodCursor = Views.iterable( Views.interval( neighborhoods, rai ) ).cursor();
		final RandomAccess< T > randomAccess = rai.randomAccess();

		final ArrayList< PositionAndValue > maxima = new ArrayList<>();

		while ( neighborhoodCursor.hasNext() )
		{
			final Neighborhood< T > neighborhood = neighborhoodCursor.next();
			randomAccess.setPosition( neighborhood );

			T centerValue = randomAccess.get();

			if ( isCenterLargestOrEqual( centerValue, neighborhood ) )
			{
				if ( centerValue.getRealDouble() > threshold )
				{
					final PositionAndValue positionAndValue = new PositionAndValue();
					positionAndValue.position = new double[ rai.numDimensions() ];
					neighborhood.localize( positionAndValue.position );
					positionAndValue.value = centerValue.getRealDouble();

					boolean isMaximumFarEnoughAwayFromOtherMaxima = true;

					for ( int i = 0; i < maxima.size(); ++i )
					{
						double distance = LinAlgHelpers.distance( maxima.get( i ).position, positionAndValue.position );
						if ( distance < minimalDistanceBetweenMaxima )
						{
							isMaximumFarEnoughAwayFromOtherMaxima = false;
							break;
						}
					}

					if ( isMaximumFarEnoughAwayFromOtherMaxima )
					{
						maxima.add( positionAndValue );
					}

				}
			}
		}

		maxima.sort( Comparator.comparing( PositionAndValue::getValue ).reversed() );

		return maxima;
	}

	private static < T extends RealType< T > & NativeType< T > >
	boolean isCenterLargestOrEqual( T center, Neighborhood< T > neighborhood )
	{
		for( T neighbor : neighborhood )
		{
			if( neighbor.compareTo( center ) > 0 )
			{
				return false;
			}
		}
		return true;
	}

	private static < T extends RealType< T > & NativeType< T > >
	double computeMinimum(Neighborhood< T > neighborhood )
	{
		double minimum = Double.MAX_VALUE;

		for( T neighbor : neighborhood )
		{
			if( neighbor.getRealDouble() < minimum )
			{
				minimum = neighbor.getRealDouble();
			}
		}
		return minimum;
	}


	public static < T extends RealType< T > & NativeType< T > >
	void splitCurrentObjectsBasedOnOverlapWithPreviousObjects(
			RandomAccessibleInterval< BitType > outputMask,
			HashMap< Integer, ArrayList< Integer > > overlappingObjectsLabelsMap,
			ImgLabeling< Integer, IntType > currentImgLabeling,
			RandomAccessibleInterval< T > currentIntensities,
			RandomAccessibleInterval< IntType > previousLabeling,
			long minimalObjectSize,
			long minimalObjectWidth,
			OpService opService,
			boolean showSplits )
	{

		final LabelRegions currentRegions = new LabelRegions( currentImgLabeling );

		for ( int currentObjectLabel : overlappingObjectsLabelsMap.keySet() )
		{
			final ArrayList< Integer > overlappingPreviousObjectLabels
					= overlappingObjectsLabelsMap.get( currentObjectLabel );

			if ( overlappingPreviousObjectLabels.size() > 1 )
			{
				RandomAccessibleInterval< BitType > currentObjectMask = Regions.asMask( currentRegions.getLabelRegion( currentObjectLabel ) );
				RandomAccessibleInterval< IntType > previousLabelingCrop =  Views.interval( previousLabeling, currentObjectMask );

				currentObjectMask = Views.zeroMin( currentObjectMask );
				previousLabelingCrop = Views.zeroMin( previousLabelingCrop );

				final RandomAccessibleInterval< T > maskedAndCroppedIntensities = Views.zeroMin( Regions.getMaskedAndCropped( currentIntensities, currentRegions.getLabelRegion( currentObjectLabel ) ) );

				final RandomAccessibleInterval< IntType > overlapLabeling =
						createOverlapLabeling(
								currentObjectMask,
								previousLabelingCrop,
								overlappingPreviousObjectLabels );

				final Set< Long > uniqueValues = Utils.computeUniqueValues( overlapLabeling );

				if ( uniqueValues.size() <= 2 )
				{
					// "<=2" because it includes 0, thus for two objects there should be at least 3 unique values
					// Normally a split should always be found, but
					// due to a bug in the watershed algorithm, seed
					// points cannot be at the border of objects.
					// Thus, in createOverlapLabelling, the overlapping objects are eroded,
					// such that it can happen that there are less than two seed points left
					// such that no splitting will happen..

					continue;
				}


//				final ArrayList< PositionAndValue > localMaxima =
//						computeSortedLocalIntensityMaxima(
//								2 * minimalObjectWidth,
//								maskedAndCroppedIntensities,
//								false );
//
//
//				final RandomAccessibleInterval< BitType > seeds =
//						positionsAsBinaryImage( overlappingPreviousObjectLabels.size(),
//								maskedAndCroppedIntensities,
//								localMaxima );

				final ImgLabeling< Integer, IntType > watershedImgLabeling = createEmptyImgLabeling( currentObjectMask );

				final ImgLabeling< Integer, IntType > seedsImgLabeling = createImgLabelingFromLabeling( overlappingPreviousObjectLabels, overlapLabeling );

				final RandomAccessibleInterval< T > invertedView = Utils.invertedView( maskedAndCroppedIntensities );

				opService.image().watershed(
						watershedImgLabeling,
						invertedView,
						seedsImgLabeling,
						true,
						true,
						currentObjectMask );


				LabelRegions< Integer > splitObjects = new LabelRegions( watershedImgLabeling );


				if ( splitObjects.getExistingLabels().contains( -1 ) )
				{
					// a watershed was found
					drawWatershedIntoMask( outputMask, currentRegions, currentObjectLabel, splitObjects );
					// sometimes the watershed is weirdly placed such that very small (single pixel) objects can occur
					Regions.removeSmallRegionsInMask( outputMask, minimalObjectSize, 1 );
					if ( showSplits )
					{
						ImageJFunctions.show( watershedImgLabeling.getSource(), "" + currentObjectLabel );
					}
				}
				else
				{
					IJ.log( "\n\nERROR DURING OBJECT SPLITTING\n\n" );
					ImageJFunctions.show( overlapLabeling ).setTitle( currentObjectLabel+"overlap" );
					ImageJFunctions.show( watershedImgLabeling.getIndexImg() ).setTitle( currentObjectLabel+"watershed" );
					ImageJFunctions.show( previousLabelingCrop ).setTitle( currentObjectLabel+"previousLabeling" );
					// TODO: examine these cases
				}
			}
		}

	}

	public static ImgLabeling< Integer, IntType > createImgLabelingFromLabeling( ArrayList< Integer > overlappingPreviousObjectLabels, RandomAccessibleInterval< IntType > seeds )
	{
		final ImgLabeling< Integer, IntType > seedsImgLabeling = new ImgLabeling<>( seeds );

		final ArrayList< Set< Integer > > labelSets = new ArrayList< Set< Integer > >();
		labelSets.add( new HashSet< Integer >() );
		for ( int i = 1; i <= overlappingPreviousObjectLabels.size(); ++i )
		{
			final HashSet< Integer > set = new HashSet< Integer >();
			set.add( i );
			labelSets.add( set );
		}

		new LabelingMapping.SerialisationAccess< Integer >( seedsImgLabeling.getMapping() )
		{
			{
				super.setLabelSets( labelSets );
			}
		};
		return seedsImgLabeling;
	}

	public static < T extends RealType< T > & NativeType< T > >
	void splitTouchingObjects(
			ImgLabeling< Integer, IntType > imgLabeling,
			RandomAccessibleInterval< T > intensity,
			RandomAccessibleInterval< BitType > mask, // This will be changed, i.e. the split(s) will be drawn into it
			HashMap< Integer, Integer > numObjectsPerRegion,
			long minimalObjectWidth,
			long minimalObjectSize,
			long maximalWatershedBoundaryLength,
			OpService opService,
			boolean forceSplit,
			boolean showSplittingAttempts )
	{

		final LabelRegions labelRegions = new LabelRegions( imgLabeling );

		for ( int label : numObjectsPerRegion.keySet() )
		{
			if ( numObjectsPerRegion.get( label ) > 1 )
			{

				final RandomAccessibleInterval< T > maskedAndCroppedIntensities = Views.zeroMin( Regions.getMaskedAndCropped( intensity, labelRegions.getLabelRegion( label ) ) );
				final RandomAccessibleInterval< BitType > labelRegionMask = Views.zeroMin( Regions.asMask( labelRegions.getLabelRegion( label ) ) );

				final ArrayList< PositionAndValue > localMaxima =
						computeSortedLocalIntensityMaxima(
								2 * minimalObjectWidth,
								maskedAndCroppedIntensities,
								showSplittingAttempts );

				if ( localMaxima.size() < numObjectsPerRegion.get( label ) )
				{
					IJ.log( "\n\nERROR: Not enough local maxima found for object: " + label + "\n\n");
					continue; // TODO: check these cases
				}

				final RandomAccessibleInterval< BitType > seeds =
						positionsAsBinaryImage( numObjectsPerRegion.get( label ),
									maskedAndCroppedIntensities,
									localMaxima );

				final ImgLabeling< Integer, IntType > watershedImgLabeling = createEmptyImgLabeling( labelRegionMask );
				final ImgLabeling< Integer, IntType > seedsImgLabeling = Regions.asImgLabeling( seeds, ConnectedComponents.StructuringElement.FOUR_CONNECTED );

				opService.image().watershed(
						watershedImgLabeling,
						Utils.invertedView( maskedAndCroppedIntensities ),
						seedsImgLabeling,
						true,
						true,
						labelRegionMask );


				LabelRegions< Integer > splitObjects = new LabelRegions( watershedImgLabeling );

				if ( ! splitObjects.getExistingLabels().contains( -1 ) )
				{
					IJ.log( "\n\nERROR DURING OBJECT SPLITTING\n\n" );
					continue; // TODO: examine these cases
				}


				boolean isValidSplit;

				if ( forceSplit )
				{
					isValidSplit = true;
				}
				else
				{
					// TODO: add integrated intensity along watershed as criterium
					isValidSplit = checkSplittingValidity(
							splitObjects,
							minimalObjectSize,
							maximalWatershedBoundaryLength );
				}

				//Utils.log( "Valid split found: " + isValidSplit );

				if ( showSplittingAttempts )
				{
					ImageJFunctions.show( watershedImgLabeling.getSource(), "" + label + "-" + isValidSplit );
				}

				if ( isValidSplit )
				{
					drawWatershedIntoMask( mask, labelRegions, label, splitObjects );
					// sometimes the watershed is weirdly placed such that very small (single pixel) objects can occur
					Regions.removeSmallRegionsInMask( mask, minimalObjectSize, 1 );
				}

			}
		}

	}


	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > createObjectSkeletons(
			ImgLabeling< Integer, IntType > imgLabeling,
			int closingRadius,
			OpService opService )
	{

		RandomAccessibleInterval< BitType > skeletons =
				ArrayImgs.bits( Intervals.dimensionsAsLongArray( imgLabeling ) );

		skeletons = Transforms.getWithAdjustedOrigin( imgLabeling.getSource(), skeletons );

		final LabelRegions< IntType > labelRegions = new LabelRegions( imgLabeling );

		for ( LabelRegion< IntType > labelRegion : labelRegions )
		{
			RandomAccessibleInterval< BitType > labelRegionMask =
					Views.zeroMin( Regions.asMask( labelRegion ) );

			labelRegionMask = Algorithms.close(  labelRegionMask, closingRadius );

			final RandomAccessibleInterval skeleton =
					opService.morphology().thinGuoHall( labelRegionMask );

			drawSkeleton( skeletons, skeleton, Intervals.minAsLongArray( labelRegion ) );
		}

		return skeletons;
	}


	public static < T extends RealType< T > & NativeType< T > >
	ArrayList< PositionAndValue > computeSortedLocalIntensityMaxima(
			long minimalObjectWidth,
			RandomAccessibleInterval< T > maskedAndCropped,
			boolean showSplittingAttempts )
	{
		double blurSimga = minimalObjectWidth / 2.0;
		final RandomAccessibleInterval< T > blurred = Utils.createBlurredRai( maskedAndCropped, blurSimga, 1.0 );
		//if ( showSplittingAttempts ) ImageJFunctions.show( blurred, "blurred" );
		final ArrayList< PositionAndValue > sortedLocalMaxima =
				Algorithms.getLocalMaxima(
						blurred,
						 minimalObjectWidth,
						0.0 );

		return sortedLocalMaxima;
	}


	public static < I extends IntegerType< I > >
	RandomAccessibleInterval< IntType > createOverlapLabeling(
			RandomAccessibleInterval< BitType > currentObjectMask,
			RandomAccessibleInterval< I > previousLabeling,
			ArrayList< Integer > previousLabels )
	{
		RandomAccessibleInterval< IntType > overlapLabeling =
				ArrayImgs.ints( Intervals.dimensionsAsLongArray( currentObjectMask ) );
		overlapLabeling = Transforms.getWithAdjustedOrigin( currentObjectMask, overlapLabeling );

		final RandomAccess< IntType > overlapLabelingAccess = overlapLabeling.randomAccess();
		final RandomAccess< I > previousLabelingAccess = previousLabeling.randomAccess();
		final Cursor< BitType > maskCursor = Views.iterable( currentObjectMask ).cursor();

//		overlapLabelingAccess.setPosition( new int[]{28,3} );
//		overlapLabelingAccess.get().setOne();
//		overlapLabelingAccess.setPosition( new int[]{67,109} );
//		overlapLabelingAccess.get().setOne();

		int previousLabel;
		while ( maskCursor.hasNext() )
		{
			if ( maskCursor.next().get() == true )
			{
				previousLabelingAccess.setPosition( maskCursor );
				previousLabel = previousLabelingAccess.get().getInteger();
				overlapLabelingAccess.setPosition( maskCursor );
				if ( previousLabels.contains( previousLabel ) )
				{
					overlapLabelingAccess.get().set( previousLabels.indexOf( previousLabel ) + 1 );
				}
			}
		}

//		ImageJFunctions.show( currentObjectMask, "mask" );
//		ImageJFunctions.show( previousLabeling, "previous labeling" );
//		ImageJFunctions.show( overlapLabeling, "overlap labeling" );

		// below is necessary because watershed seeds are not allowed to touch the mask boundary
		Utils.applyMask( overlapLabeling, Algorithms.erode( currentObjectMask, 2 ) );

		return overlapLabeling;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > positionsAsBinaryImage(
			int numPositions,
			RandomAccessibleInterval< T > maskedAndCropped,
			ArrayList< PositionAndValue > positions )
	{
		RandomAccessibleInterval< BitType > binaryImage =
				ArrayImgs.bits( Intervals.dimensionsAsLongArray(  maskedAndCropped ) );
		binaryImage = Transforms.getWithAdjustedOrigin( maskedAndCropped, binaryImage );

		final RandomAccess< BitType > randomAccess = binaryImage.randomAccess();
		for ( int i = 0; i < numPositions; ++i )
		{
			randomAccess.setPosition( Utils.asLongs( positions.get( i ).position ) );
			randomAccess.get().setOne();
		}
		return binaryImage;
	}

	public static boolean checkSplittingValidity(
			LabelRegions< Integer > splitObjects,
			long minimumObjectSize,
			long maximalWatershedLength )
	{

		if ( ! isWatershedValid( splitObjects, maximalWatershedLength ) ) return false;

		ArrayList< Long > regionSizes = new ArrayList<>(  );

		for( LabelRegion region : splitObjects )
		{
			regionSizes.add( region.size() );
		}


		if ( regionSizes.size() >=2 )
		{
			Collections.sort( regionSizes );
			Collections.reverse( regionSizes );

			if ( regionSizes.get( 1 ) < minimumObjectSize )
			{
				// 2nd largest object too small
				return false;
			}
		}

		return true;
	}

	public static boolean isWatershedValid( LabelRegions< Integer > splitObjects, long maximalWatershedLength )
	{
		boolean isValidSplit = true;

		for( LabelRegion region : splitObjects )
		{
			int splitObjectLabel = ( int ) region.getLabel();

			if ( splitObjectLabel == WATERSHED )
			{
				final ImgLabeling< Integer, IntType > imgLabeling = Regions.asImgLabeling( Regions.asMask( region ), ConnectedComponents.StructuringElement.FOUR_CONNECTED );
				final LabelRegions< Integer > splitRegions = new LabelRegions( imgLabeling );

				long maximalLength = 0;
				for ( LabelRegion splitRegion : splitRegions )
				{
					if ( splitRegion.size() > maximalLength )
					{
						maximalLength = splitRegion.size();
					}
				}

				if ( maximalLength > maximalWatershedLength )
				{
					isValidSplit = false;
					break;
				}
			}
		}
		return isValidSplit;
	}

	public static void drawSkeleton( RandomAccessibleInterval< BitType > output,
									 RandomAccessibleInterval< BitType > skeleton,
									 long[] regionOffset )
	{
		final Cursor< BitType > skeletonCursor = Views.iterable( skeleton ).cursor();
		final RandomAccess< BitType > outputAccess = output.randomAccess();

		long[] position = new long[ output.numDimensions() ];

		while( skeletonCursor.hasNext() )
		{
			if ( skeletonCursor.next().get() == true )
			{
				skeletonCursor.localize( position );
				addOffset( regionOffset, position );
				outputAccess.setPosition( position );
				outputAccess.get().set( true );
			}
		}

	}

	public static void addOffset( long[] regionOffset, long[] position )
	{
		for ( int d = 0; d < position.length; ++d )
		{
			position[ d ] += regionOffset[ d ];
		}
	}

	public static void drawWatershedIntoMask( RandomAccessibleInterval< BitType > mask,
											  LabelRegions labelRegions,
											  int label,
											  LabelRegions< Integer > splitObjects )
	{
		final long[] regionOffset = Intervals.minAsLongArray( labelRegions.getLabelRegion( label ) );
		LabelRegion watershed = splitObjects.getLabelRegion( -1 );
		final Cursor cursor = watershed.inside().cursor();
		final RandomAccess< BitType > maskRandomAccess = mask.randomAccess();
		long[] position = new long[ watershed.numDimensions() ];
		while( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( position );
			addOffset( regionOffset, position );
			maskRandomAccess.setPosition( position );
			maskRandomAccess.get().set( false );
		}
	}

	public static RandomAccessibleInterval< BitType > close(
			RandomAccessibleInterval< BitType > mask,
			int closingRadius )
	{
		if ( closingRadius <= 0 ) return mask;

		// TODO: Bug(?!) in imglib2 Closing.close makes enlargement necessary, otherwise one gets weird results at boundaries

		RandomAccessibleInterval< BitType > morphed =
				ArrayImgs.bits( Intervals.dimensionsAsLongArray( mask ) );

		morphed = Views.translate( morphed, Intervals.minAsLongArray( mask ) );

		final RandomAccessibleInterval< BitType > enlargedMask =
				Utils.getEnlargedRai( mask, closingRadius );
		final RandomAccessibleInterval< BitType > enlargedMorphed =
				Utils.getEnlargedRai( morphed, closingRadius );

		Shape closingShape = new HyperSphereShape( closingRadius );

		Closing.close(
				Views.extendZero( enlargedMask ),
				Views.iterable( enlargedMorphed ),
				closingShape,
				1 );


		return Views.interval( enlargedMorphed, mask );
	}


	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createNearestNeighborResampledArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		// Convert to RealRandomAccessible such that we can obtain values at (infinite) non-integer coordinates
		RealRandomAccessible< T > rra =
				Views.interpolate( Views.extendBorder( input ),
						new NearestNeighborInterpolatorFactory<>() );

		// Change scale such that we can sample from integer coordinates (for raster function below)
		Scale scale = new Scale( scalingFactors );
		RealRandomAccessible< T > rescaledRRA  = RealViews.transform( rra, scale );

		// Create view sampled at integer coordinates
		final RandomAccessible< T > rastered = Views.raster( rescaledRRA );

		// Put an interval to make it a finite "normal" image again
		final RandomAccessibleInterval< T > finiteRastered =
				Views.interval( rastered, createTransformedInterval( input, scale ) );

		// Convert from View to a "conventional" image in RAM
		// - Above code would also run on, e.g. 8 TB image, within ms
		// - Now, we really force it to create the image
		// (we actually might now have to, depends...)
		final RandomAccessibleInterval< T > output = copyAsArrayImg( finiteRastered );

		return output;
	}

	/**
	 *
	 * very slow...
	 *
	 * @param mask
	 * @param radius
	 * @return
	 */
	public static RandomAccessibleInterval< BitType > open(
			RandomAccessibleInterval< BitType > mask,
			int radius )
	{
		if ( radius <= 0 ) return mask;

		// TODO: Bug(?!) in imglib2 makes enlargement necessary,
		//  otherwise one gets weird results at boundaries

		RandomAccessibleInterval< BitType > morphed =
				ArrayImgs.bits( Intervals.dimensionsAsLongArray( mask ) );

		morphed = Views.translate( morphed, Intervals.minAsLongArray( mask ) );

		final RandomAccessibleInterval< BitType > enlargedMask =
				Utils.getEnlargedRai( mask, radius );
		final RandomAccessibleInterval< BitType > enlargedMorphed =
				Utils.getEnlargedRai( morphed, radius );

		Shape shape = new HyperSphereShape( radius );

		Opening.open(
				Views.extendZero( enlargedMask ),
				Views.iterable( enlargedMorphed ),
				shape,
				4 );

		return Views.interval( enlargedMorphed, mask );
	}



	public static ImgLabeling< Integer, IntType > createEmptyImgLabeling( RandomAccessibleInterval< BitType > mask )
	{
		RandomAccessibleInterval< IntType > watershedLabelImg = ArrayImgs.ints( Intervals.dimensionsAsLongArray( mask ) );
		watershedLabelImg = Transforms.getWithAdjustedOrigin( mask, watershedLabelImg );
		return new ImgLabeling<>( watershedLabelImg );
	}

	public static < T extends RealType< T > & NativeType< T > > ArrayList< RandomAccessibleInterval< T > >
	createMaximumIntensityProjectionsAssumingImagePlusDimensionOrder(
			RandomAccessibleInterval< T > inputImages,
			long channel,
			long tMin, long tMax )
	{
		ArrayList<  RandomAccessibleInterval< T > > intensities = new ArrayList<>();

		for ( long t = tMin; t <= tMax; ++t )
		{
			final IntervalView< T > channelView = Views.hyperSlice( inputImages, 2, channel );
			final IntervalView< T > timePointView = Views.hyperSlice( channelView, 3, t );
			final RandomAccessibleInterval maximum = new Projection( timePointView, 2 ).maximum();
			intensities.add( maximum );
		}

		return intensities;
	}

	public static RandomAccessibleInterval< DoubleType > computeSquaredDistances( RandomAccessibleInterval< BitType > mask )
	{
		final RandomAccessibleInterval< DoubleType > doubleBinary =
				Converters.convert( mask, ( i, o ) -> o.set( i.get() ? Double.MAX_VALUE : 0 ), new DoubleType() );

		RandomAccessibleInterval< DoubleType > distance = ArrayImgs.doubles( Intervals.dimensionsAsLongArray( doubleBinary ) );
		distance = Views.translate( distance, Intervals.minAsLongArray( mask ) );

		DistanceTransform.transform( doubleBinary, distance, DistanceTransform.DISTANCE_TYPE.EUCLIDIAN, 1.0D );
		return distance;
	}


	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > erode(
			RandomAccessibleInterval< R > image,
			int radiusPixels )
	{
		final RandomAccessibleInterval< R > morphed = Utils.createEmptyCopy( image );

		if ( radiusPixels > 0 )
		{
			Shape shape = new HyperSphereShape( radiusPixels );
			Erosion.erode( Views.extendBorder( image ), Views.iterable( morphed ), shape, 1 );
		}

		return morphed;
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > dilate(
			RandomAccessibleInterval< R > image,
			int radius )
	{
		final RandomAccessibleInterval< R > morphed = Utils.createEmptyCopy( image );

		if ( radius > 0 )
		{
			Shape shape = new HyperSphereShape( radius );
			Dilation.dilate( Views.extendBorder( image ), Views.iterable( morphed ), shape, 1 );
		}

		return morphed;
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > median(
			RandomAccessibleInterval< R > intensity,
			int radius,
			OpService opService )
	{
		RandomAccessibleInterval< R > median = Utils.createEmptyCopy( intensity );

		opService.filter().median(
				Views.iterable( Views.zeroMin( median ) ),
				Views.zeroMin( intensity ),
				new HyperSphereShape( radius ));

		median = Views.translate( median, Intervals.minAsLongArray( intensity ) );

		return median;
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > median2dUsingIJ1(
			RandomAccessibleInterval< R > intensity,
			int radius )
	{

		final ImagePlus imagePlus = ImageJFunctions.wrap( intensity, "" ).duplicate();

		IJ.run( imagePlus, "Median...", "radius=" + radius );

		RandomAccessibleInterval< R > median = ImageJFunctions.wrapReal( imagePlus );

		//median = Views.translate( median, Intervals.minAsLongArray( intensity ) );

		return median;
	}

	public static < T extends RealType< T > & NativeType< T > >
	CoordinatesAndValues computeRadialProfile(
			RandomAccessibleInterval< T > image,
			double[] center,
			double spacing,
			double maxDistanceInMicrometer )
	{
		final Cursor< T > cursor = Views.iterable( image ).cursor();
		final int numDimensions = image.numDimensions();

		final double[] position = new double[ numDimensions ];

		int maxBin = ( int ) ( maxDistanceInMicrometer / spacing );
		double[] counts = new double[ maxBin ];
		double[] values = new double[ maxBin ];

		while ( cursor.hasNext() )
		{
			cursor.localize( position );
			final double distance = LinAlgHelpers.distance( center, position ) * spacing;
			final double value = cursor.next().getRealDouble();

			final int bin = ( int ) ( distance / spacing );
			if ( bin < maxBin )
			{
				counts[ bin ] += 1;
				values[ bin ] += value;
			}
		}

		final CoordinatesAndValues radialProfileOld = new CoordinatesAndValues();
		final CoordinateToValue radialProfile = new CoordinateToValue();
		for ( int bin = 0; bin < maxBin; bin++ )
		{
			radialProfileOld.values.add( values[ bin ] / counts[ bin ] );
			radialProfileOld.coordinates.add( bin * spacing );
			radialProfile.put( bin * spacing, values[ bin ] / counts[ bin ] );
		}

		return radialProfileOld;
	}

	public static < T extends RealType< T > > Histogram1d< T >
	histogram( RandomAccessibleInterval< T > rai, int numBins )
	{
		final Pair< Double, Double > minMaxValues = getMinMaxValues( rai );

		final Real1dBinMapper< T > tReal1dBinMapper =
				new Real1dBinMapper<>( minMaxValues.getA(),
						minMaxValues.getB(),
						numBins,
						false );

		final Histogram1d<T> histogram1d = new Histogram1d<>( tReal1dBinMapper );

		histogram1d.countData( Views.iterable(  rai ) );

		return histogram1d;
	}

	public static < T extends RealType< T > >
	double thresholdOtsu( RandomAccessibleInterval< T > rai )
	{
		final Histogram1d< T > histogram =
				histogram( rai, 256 );

		final T type = Views.iterable( rai ).firstElement().createVariable();
		final ComputeOtsuThreshold< T > threshold = new ComputeOtsuThreshold<>();
		final long bin = threshold.computeBin( histogram );
		histogram.getCenterValue( bin, type );

		return type.getRealDouble();
	}

	public static < T extends RealType< T > >
	double thresholdHuang( RandomAccessibleInterval< T > rai )
	{
		final Histogram1d< T > histogram =
				histogram( rai, 256 );

		final T type = Views.iterable( rai ).firstElement().createVariable();
		final ComputeHuangThreshold< T > threshold = new ComputeHuangThreshold<>();
		final long bin = threshold.computeBin( histogram );
		histogram.getCenterValue( bin, type );

		return type.getRealDouble();
	}

	public static < T extends RealType< T > >
	double thresholdYen( RandomAccessibleInterval< T > rai )
	{
		final Histogram1d< T > histogram = histogram( rai, 256 );
		final T type = Views.iterable( rai ).firstElement().createVariable();
		final ComputeYenThreshold< T > threshold = new ComputeYenThreshold<>();
		final long bin = threshold.computeBin( histogram );
		histogram.getCenterValue( bin, type );

		return type.getRealDouble();
	}

}
