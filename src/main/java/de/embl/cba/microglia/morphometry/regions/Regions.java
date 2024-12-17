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
package de.embl.cba.microglia.morphometry.regions;

import de.embl.cba.microglia.Transforms;
import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.Algorithms;
import ij.IJ;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.converter.Converters;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class Regions
{

	public static Set< LabelRegion< Integer > > getCentralRegions(
			ImgLabeling< Integer, IntType > labeling,
			double[] center,
			long radius )
	{
		final Set< Integer > centralLabels = Algorithms.getCentralLabels( labeling, center, radius );

		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );

		final HashSet< LabelRegion< Integer > > centralRegions = new HashSet< >();

		for ( int label : centralLabels )
		{
			centralRegions.add( regions.getLabelRegion( label ) );
		}

		return centralRegions;
	}

	public static LabelRegion< Integer > getLargestRegion( ImgLabeling< Integer, IntType > labeling )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( labeling );

		long maxSize = Long.MIN_VALUE;
		LabelRegion largestRegion = null;

		for ( LabelRegion labelRegion : labelRegions )
		{
			if ( labelRegion.size() > maxSize)
			{
				largestRegion = labelRegion;
				maxSize = labelRegion.size();
			}
		}

		return largestRegion;
	}

	public static RandomAccessibleInterval< BitType > asMask( LabelRegion labelRegion )
	{
		RandomAccessibleInterval< BitType > rai = ArrayImgs.bits( Intervals.dimensionsAsLongArray( labelRegion ) );
		rai = Transforms.getWithAdjustedOrigin( labelRegion, rai  );
		final RandomAccess< BitType > randomAccess = rai.randomAccess();

		final Cursor cursor = labelRegion.inside().cursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			randomAccess.setPosition( cursor );
			randomAccess.get().set( true );
		}

		return rai;
	}

	public static void drawRegionInMask(
			LabelRegion labelRegion,
			RandomAccessibleInterval< BitType > mask )
	{
		final RandomAccess< BitType > maskAccess = mask.randomAccess();

		final Cursor cursor = labelRegion.inside().cursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			maskAccess.setPosition( cursor );
			maskAccess.get().set( true );
		}
	}

	public static long size( LabelRegion labelRegion )
	{

		final Cursor cursor = labelRegion.inside().cursor();

		long size = 0;
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			size++;
		}

		return size;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > getMaskedAndCropped( RandomAccessibleInterval<T> image, LabelRegion labelRegion )
	{
		ImgFactory< T > imgFactory = new ArrayImgFactory( image.randomAccess().get()  );
		RandomAccessibleInterval< T > output = Views.translate( imgFactory.create( labelRegion ), Intervals.minAsLongArray( labelRegion )  ) ;

		final RandomAccess< T > imageRandomAccess = image.randomAccess();
		final RandomAccess< T > outputRandomAccess = output.randomAccess();
		final Cursor cursor = labelRegion.inside().cursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			imageRandomAccess.setPosition( cursor );
			outputRandomAccess.setPosition( cursor );
			outputRandomAccess.get().set( imageRandomAccess.get() );
		}

		return output;
	}

	public static < T extends RealType< T > & NativeType< T > >
	void removeSmallRegionsInMasks(
			ArrayList< RandomAccessibleInterval< T > > masks,
			double sizeInCalibratedUnits,
			double calibration )
	{

		for ( RandomAccessibleInterval mask : masks )
		{
			removeSmallRegionsInMask( mask, sizeInCalibratedUnits, calibration );
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	void removeSmallRegionsInMask(
			RandomAccessibleInterval< T > mask,
			double sizeInCalibratedUnits,
			double calibration )
	{
		final ImgLabeling< Integer, IntType > imgLabeling =
				asImgLabeling( mask, ConnectedComponents.StructuringElement.FOUR_CONNECTED );

		long minimalObjectSize = ( long ) ( sizeInCalibratedUnits / Math.pow( calibration, imgLabeling.numDimensions() ) );

		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );
		int numRegions = 0, numRemoved = 0;
		for ( LabelRegion labelRegion : labelRegions )
		{
			numRegions++;
			if ( labelRegion.size() < minimalObjectSize )
			{
				numRemoved ++;
				removeRegion( mask, labelRegion );
			}
		}

		IJ.log( "Removed " + numRemoved + " small regions of " + numRegions + " total regions." );
	}

	public static < T extends RealType< T > & NativeType< T > >
	void removeSmallRegionsInMasks(
			ArrayList< RandomAccessibleInterval< T > > masks,
			long minimalObjectSize )
	{

		for ( RandomAccessibleInterval mask : masks )
		{
			removeSmallRegionsInMask( mask, minimalObjectSize );
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	void removeSmallRegionsInMask(
			RandomAccessibleInterval< T > mask,
			long minimalObjectSize )
	{
		final ImgLabeling< Integer, IntType > imgLabeling = asImgLabeling( mask, ConnectedComponents.StructuringElement.FOUR_CONNECTED );

		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );
		for ( LabelRegion labelRegion : labelRegions )
		{
			if ( labelRegion.size() < minimalObjectSize )
			{
				removeRegion( mask, labelRegion );
			}
		}
	}

	public static < R extends RealType< R > & NativeType< R > >
	void onlyKeepLargestRegion( RandomAccessibleInterval< R > mask,
								ConnectedComponents.StructuringElement structuringElement )
	{
		final ArrayList< RegionAndSize > sortedRegions =
				Regions.getSizeSortedRegions(
						mask,
						structuringElement );
		Utils.setValues( mask, 0 );
		Regions.drawRegion( mask, sortedRegions.get( 0 ).getRegion(), 1.0 );
	}

	private static < R extends RealType< R > & NativeType< R > >
	void drawRegion( RandomAccessibleInterval< R > img,
					 LabelRegion labelRegion,
					 double value)
	{
		final Cursor< Void > regionCursor = labelRegion.inside().cursor();
		final RandomAccess< R > access = img.randomAccess();
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().setReal( value );
		}
	}

	private static < T extends RealType< T > & NativeType< T > >
	void removeRegion( RandomAccessibleInterval< T > img, LabelRegion labelRegion )
	{
		final Cursor regionCursor = labelRegion.inside().cursor();
		final RandomAccess< T > access = img.randomAccess();
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().setReal( 0 );
		}
	}

	public static < R extends RealType< R > & NativeType< R > >
	ArrayList< RegionAndSize > getSizeSortedRegions(
			RandomAccessibleInterval< R > invertedMembraneMask,
			ConnectedComponents.StructuringElement structuringElement )
	{
		final ImgLabeling< Integer, IntType > imgLabeling = asImgLabeling(
				invertedMembraneMask,
				structuringElement );

		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		final ArrayList< RegionAndSize > regionsAndSizes = new ArrayList<>();

		for ( LabelRegion labelRegion : labelRegions )
		{
			regionsAndSizes.add( new RegionAndSize( labelRegion, labelRegion.size() ) );
		}

		Collections.sort( regionsAndSizes );

		return regionsAndSizes;
	}

	public static < T extends RealType< T > & NativeType< T >  >
	ImgLabeling< Integer, IntType > asImgLabeling(
			RandomAccessibleInterval< T > masks,
			ConnectedComponents.StructuringElement structuringElement )
	{

		RandomAccessibleInterval< IntType > labelImg = ArrayImgs.ints( Intervals.dimensionsAsLongArray( masks ) );
		labelImg = Transforms.getWithAdjustedOrigin( masks, labelImg );
		final ImgLabeling< Integer, IntType > imgLabeling = new ImgLabeling<>( labelImg );

		final java.util.Iterator< Integer > labelCreator = new java.util.Iterator< Integer >()
		{
			int id = 1;

			@Override
			public boolean hasNext()
			{
				return true;
			}

			@Override
			public synchronized Integer next()
			{
				return id++;
			}
		};

		final RandomAccessibleInterval< UnsignedIntType > unsignedIntTypeRandomAccessibleInterval =
				Converters.convert(
						masks,
						( i, o ) -> o.set( i.getRealDouble() > 0 ? 1 : 0 ),
						new UnsignedIntType() );

		ConnectedComponents.labelAllConnectedComponents(
				Views.extendBorder( unsignedIntTypeRandomAccessibleInterval ),
				imgLabeling,
				labelCreator,
				structuringElement );

		return imgLabeling;
	}

	public static RandomAccessibleInterval< BitType > asMask(
			Set< LabelRegion< Integer > > regions,
			Interval interval )
	{
		RandomAccessibleInterval< BitType > regionsMask =  ArrayImgs.bits(
				Intervals.dimensionsAsLongArray( interval ) );

		regionsMask = Transforms.getWithAdjustedOrigin( interval, regionsMask );

		final RandomAccess< BitType > maskAccess = regionsMask.randomAccess();

		for ( LabelRegion region : regions )
		{
			final Cursor< Void > regionCursor = region.cursor();
			while ( regionCursor.hasNext() )
			{
				regionCursor.fwd();
				maskAccess.setPosition( regionCursor );
				maskAccess.get().set( true );
			}
		}

		return regionsMask;
	}

	public static RandomAccessibleInterval< BitType > asMask(
			Set< LabelRegion< Integer > > regions,
			long[] dimensions,
			long[] offset )
	{
		RandomAccessibleInterval< BitType > regionsMask = ArrayImgs.bits( dimensions );
		regionsMask = Views.translate( regionsMask, offset );

		final RandomAccess< BitType > maskAccess = regionsMask.randomAccess();

		for ( LabelRegion region : regions )
		{
			final Cursor< Void > regionCursor = region.cursor();
			while ( regionCursor.hasNext() )
			{
				regionCursor.fwd();
				maskAccess.setPosition( regionCursor );
				maskAccess.get().set( true );
			}
		}

		return regionsMask;
	}

}
