package de.embl.cba.microglia.track;

import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.regions.Regions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;

import java.util.HashMap;
import java.util.HashSet;

public class TrackingUtils
{
	public static void addOverlap( HashMap< Integer, Long > overlaps, int integer )
	{
		if ( overlaps.keySet().contains( integer ) )
		{
			overlaps.put( integer,  overlaps.get( integer ).longValue() + 1 );
		}
		else
		{
			overlaps.put( integer,  1L );
		}
	}


	/**
	 * TODO: maybe some area fraction overlap might be better?
	 *
	 *
	 * @param overlaps
	 * @return
	 */
	public static int getMaxOverlapLabel( HashMap< Integer, Long > overlaps )
	{
		int maxOverlapLabel = 0;
		long maxOverlap = Long.MIN_VALUE;

		for ( Integer label : overlaps.keySet() )
		{
			final long overlap = overlaps.get( label ).longValue();

			if ( overlap > maxOverlap )
			{
				maxOverlap = overlap;
				maxOverlapLabel = label;
			}
		}
		return maxOverlapLabel;
	}

	public static < T extends RealType< T > >
	HashMap< Integer, Long > computeRegionOverlaps(
			RandomAccessibleInterval< T > previousLabeling,
			LabelRegion region )
	{
		final HashMap< Integer, Long > overlaps = new HashMap<>();

		final RandomAccess< T > previousLabelsAccess = previousLabeling.randomAccess();
		Cursor cursor = region.inside().cursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			previousLabelsAccess.setPosition( cursor );

			final int label = (int) previousLabelsAccess.get().getRealDouble();

			if ( label != 0 )
			{
				addOverlap( overlaps, label );
			}
		}
		return overlaps;
	}


	public static < T extends RealType< T > & NativeType< T > >
	LabelingAndMaxIndex getMaximalOverlapBasedLabeling(
			RandomAccessibleInterval< IntType > referenceLabeling,
			RandomAccessibleInterval< T > currentMask,
			Integer maxIndex )
	{
		final HashSet< Integer > newObjectIds = new HashSet<>();

		final LabelRegions< Integer > currentRegions = new LabelRegions(
				Regions.asImgLabeling(
						currentMask,
						ConnectedComponents.StructuringElement.FOUR_CONNECTED ) );

		final LabelingAndMaxIndex labelingAndMaxIndex = new LabelingAndMaxIndex();

		labelingAndMaxIndex.labeling = ArrayImgs.ints( Intervals.dimensionsAsLongArray( currentMask ) );

		for ( LabelRegion< Integer > region : currentRegions )
		{
			final HashMap< Integer, Long > overlaps = computeRegionOverlaps( referenceLabeling, region );

			int objectId;

			if( overlaps.size() == 0 )
			{
				// no overlap => create new label
				objectId = ++maxIndex;
			}
			else
			{
				objectId = getMaxOverlapLabel( overlaps );

				if ( newObjectIds.contains( objectId ) )
				{
					objectId = ++maxIndex;
				}
			}

			newObjectIds.add( objectId );

			Utils.drawObject( labelingAndMaxIndex.labeling, region, objectId );
		}

		labelingAndMaxIndex.maxIndex = maxIndex;

		return labelingAndMaxIndex;
	}
}
