package de.embl.cba.microglia.track;

import de.embl.cba.microglia.morphometry.regions.Regions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import java.util.ArrayList;

public class MaximalOverlapTracker< T extends RealType< T > & NativeType< T > >
{

	ArrayList< RandomAccessibleInterval< T > > masks;

	private Integer maxIndex;

	private ArrayList< RandomAccessibleInterval< IntType > > labelings;

	public MaximalOverlapTracker( ArrayList< RandomAccessibleInterval< T > > masks )
	{
		this.masks = masks;
	}

	public void run()
	{
		int tMin = 0;
		int tMax = masks.size() - 1;

		int t = tMin;

		maxIndex = Utils.getNumObjects( masks.get( t ) );

		RandomAccessibleInterval< IntType > previousLabeling = Regions.asImgLabeling( masks.get( tMin ), ConnectedComponents.StructuringElement.FOUR_CONNECTED ).getSource();

		labelings = initLabelings( masks.get( tMin )  );

		for ( t = tMin + 1; t <= tMax; ++t )
		{
			final LabelingAndMaxIndex labelingAndMaxIndex = TrackingUtils.getMaximalOverlapBasedLabeling(
					previousLabeling,
					masks.get( t ),
					maxIndex );

			labelings.add( labelingAndMaxIndex.labeling );
			maxIndex = labelingAndMaxIndex.maxIndex;
			previousLabeling = labelingAndMaxIndex.labeling;
		}
	}


	public ArrayList< RandomAccessibleInterval< IntType > > getLabelings()
	{
		return labelings;
	}


	public ArrayList< RandomAccessibleInterval< IntType > > initLabelings( RandomAccessibleInterval< T > mask )
	{
		final ArrayList< RandomAccessibleInterval< IntType > > updatedLabelings = new ArrayList<>();
		updatedLabelings.add( Regions.asImgLabeling( mask, ConnectedComponents.StructuringElement.FOUR_CONNECTED ).getIndexImg() );
		return updatedLabelings;
	}

}
