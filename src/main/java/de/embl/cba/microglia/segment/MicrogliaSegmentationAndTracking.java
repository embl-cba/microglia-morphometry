package de.embl.cba.microglia.segment;

import de.embl.cba.microglia.track.SemiAutomatedTrackingSplitter;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;

public class MicrogliaSegmentationAndTracking< T extends RealType< T > & NativeType< T > >
{
	private final MicrogliaSettings settings;
	private final ArrayList< RandomAccessibleInterval< T > > intensities;
	private ArrayList< RandomAccessibleInterval< T > > labelings;

	public MicrogliaSegmentationAndTracking(
			ArrayList< RandomAccessibleInterval< T  > > intensities,
			MicrogliaSettings settings )
	{
		this.intensities = intensities;
		this.settings = MicrogliaSettings.addMissingSettings( settings );
	}

	public void run()
	{
		ArrayList< RandomAccessibleInterval< T > > masks = createBinaryMasks( intensities );
		labelings = splitTouchingObjectsAndTrack( intensities, masks );
	}

	private ArrayList< RandomAccessibleInterval< T > > createBinaryMasks(
			ArrayList< RandomAccessibleInterval< T > > intensities )
	{
		ArrayList<  RandomAccessibleInterval< T > > masks = new ArrayList<>();
		final int numFrames = intensities.size();
		for ( long t = 0; t < numFrames; ++t )
		{
			IJ.log("Creating binary masks " + ( t + 1 ) + "/" + numFrames );
			final MicrogliaBinarizer microgliaBinarizer =
					new MicrogliaBinarizer( intensities.get( ( int ) t ), settings );
			microgliaBinarizer.run();
			masks.add( microgliaBinarizer.getMask() );
		}
		return masks;
	}

	private ArrayList< RandomAccessibleInterval< T > > splitTouchingObjectsAndTrack(
			ArrayList< RandomAccessibleInterval< T > > intensities,
			ArrayList< RandomAccessibleInterval< T > > masks )
	{
		final SemiAutomatedTrackingSplitter splitter =
				new SemiAutomatedTrackingSplitter( masks, intensities, settings );

		if ( labelings != null )
		{
			splitter.setLabelings( labelings );
		}

		splitter.run();

		return splitter.getLabelings();
	}

	public ArrayList< RandomAccessibleInterval< T > > getLabelings( )
	{
		return labelings;
	}

	public void setLabelings( ArrayList< RandomAccessibleInterval < T > > labelings )
	{
		this.labelings = labelings;
	}
}
