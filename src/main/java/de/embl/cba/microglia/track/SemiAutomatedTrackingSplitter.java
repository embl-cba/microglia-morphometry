package de.embl.cba.microglia.track;

import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.measure.Measurements;
import de.embl.cba.microglia.morphometry.Algorithms;
import de.embl.cba.microglia.morphometry.Constants;
import de.embl.cba.microglia.morphometry.regions.Regions;
import de.embl.cba.microglia.segment.MicrogliaSettings;
import de.embl.cba.microglia.segment.MicrogliaShapeAndIntensitySplitter;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class SemiAutomatedTrackingSplitter< T extends RealType< T > & NativeType< T > >
{

	final ArrayList< RandomAccessibleInterval< BitType > > masks;
	final ArrayList< RandomAccessibleInterval< T > > intensities;
	private ArrayList< RandomAccessibleInterval< IntType > > labelings;
	final MicrogliaSettings settings;
	private final ImagePlus intensitiesImp;
	private long minimalObjectSizeInPixels;
	private static Point intensitiesImpLocation;
	private TrackingSplitterManualCorrectionUI trackingSplitterManualCorrectionUI;

	public SemiAutomatedTrackingSplitter(
			ArrayList< RandomAccessibleInterval< BitType > > masks,
			ArrayList< RandomAccessibleInterval< T > > intensities,
			MicrogliaSettings settings )
	{
		this.masks = masks;
		this.intensities = intensities;
		this.settings = settings;

		setMinimalObjectSize( settings, masks.get( 0 ).numDimensions() );

		this.labelings = new ArrayList();
		this.intensitiesImp = Utils.getAsImagePlusMovie( intensities, Constants.INTENSITIES );
		Calibration cal = new Calibration();
		cal.setUnit( "micrometer" );
		cal.pixelHeight = settings.workingVoxelSize;
		cal.pixelWidth = settings.workingVoxelSize;
		this.intensitiesImp.setCalibration( cal );
	}

	public void setMinimalObjectSize( MicrogliaSettings settings, int numDimensions )
	{
		minimalObjectSizeInPixels = ( long ) ( settings.minimalTrackingSplittingObjectArea
				/ Math.pow( settings.workingVoxelSize, numDimensions ) );
	}


	public void run()
	{
		final int tMaxExistingLabeling = labelings.size() - 1;
		int tMin = Math.max( 0, tMaxExistingLabeling );
		int tMax = masks.size() - 1;

		for ( int t = tMin; t <= tMax; ++t )
		{
			if ( t > tMaxExistingLabeling )
			{
				createAndAddNewLabeling( t );
			}

			if ( settings.manualSegmentationCorrection )
			{
				manuallyCorrectLabelings( t );

				if( trackingSplitterManualCorrectionUI.isStopped() )
				{
					break; // saving will happen in the command
				}
			}
		}
	}

	private void createAndAddNewLabeling(int t )
	{
		IJ.log( "Instance segmentation of frame " + t );

		if ( t == 0 )
		{
			final RandomAccessibleInterval splitMask = getSplitMask( t );

			final ImgLabeling imgLabeling = Regions.asImgLabeling( splitMask, ConnectedComponents.StructuringElement.FOUR_CONNECTED );

			labelings.add( imgLabeling.getIndexImg() );
		}
		else
		{
			RandomAccessibleInterval< IntType > previousLabeling = labelings.get( t - 1 );
			Integer maxIndex = Algorithms.getMaximumValue( previousLabeling ).intValue();

			RandomAccessibleInterval< BitType > mask = splitCurrentMaskBasedOnPreviousLabeling( t, previousLabeling );

			final LabelingAndMaxIndex labelingAndMaxIndex =
					TrackingUtils.getMaximalOverlapBasedLabeling(
							previousLabeling,
							mask,
							maxIndex );

			labelings.add( labelingAndMaxIndex.labeling );
		}
	}

	public RandomAccessibleInterval getSplitMask( int t )
	{
		final MicrogliaShapeAndIntensitySplitter splitter =
				new MicrogliaShapeAndIntensitySplitter( masks.get( t ), intensities.get( t ), settings );
		splitter.run();
		return splitter.getSplitMask();
	}

	public RandomAccessibleInterval< BitType > splitCurrentMaskBasedOnPreviousLabeling(
			int currentTimePoint,
			RandomAccessibleInterval< IntType > previousLabeling )
	{
		final ImgLabeling< Integer, IntType > currentImgLabeling =
				Regions.asImgLabeling(
						masks.get( currentTimePoint ),
						ConnectedComponents.StructuringElement.FOUR_CONNECTED );

		RandomAccessibleInterval< IntType > currentLabeling = currentImgLabeling.getIndexImg();

		HashMap< Integer, ArrayList< Integer > > overlappingObjectsLabelsMap =
				getOverlappingObjectLabelsMap(
						currentTimePoint,
						previousLabeling,
						currentImgLabeling,
						currentLabeling );

		RandomAccessibleInterval< BitType > splitMask =
				Utils.copyAsArrayImg( masks.get( currentTimePoint ) );

		Algorithms.splitCurrentObjectsBasedOnOverlapWithPreviousObjects(
				splitMask,
				overlappingObjectsLabelsMap,
				currentImgLabeling,
				intensities.get( currentTimePoint ),
				previousLabeling,
				minimalObjectSizeInPixels,
				( int ) ( settings.minimalObjectCenterDistance / settings.workingVoxelSize ),
				settings.opService,
				false);

		return splitMask;
	}

	public void manuallyCorrectLabelings( int t )
	{
		showIntensities( t );

		trackingSplitterManualCorrectionUI =
				new TrackingSplitterManualCorrectionUI(
						labelings,
						minimalObjectSizeInPixels,
						settings.outputLabelingsPath,
						settings.calibration );

		while ( ! trackingSplitterManualCorrectionUI.isThisFrameFinished() )
		{
			IJ.wait( 100 );
		}

		labelings = trackingSplitterManualCorrectionUI.getLabelings();

		hideIntensities();
	}

	public void hideIntensities()
	{
		intensitiesImpLocation = intensitiesImp.getWindow().getLocation();
		intensitiesImp.hide();
	}

	public void showIntensities( int t )
	{
		intensitiesImp.show();
		if ( intensitiesImpLocation != null ) intensitiesImp.getWindow().setLocation( intensitiesImpLocation );
		intensitiesImp.setT( t + 1 );
		intensitiesImp.updateImage();
		IJ.run( intensitiesImp, "Enhance Contrast", "saturated=0.01");
	}


	public HashMap< Integer, ArrayList< Integer > > getOverlappingObjectLabelsMap( int t, RandomAccessibleInterval< IntType > previousLabeling, ImgLabeling< Integer, IntType > currentImgLabeling, RandomAccessibleInterval< IntType > currentLabeling )
	{
		HashMap< Integer, ArrayList< Integer > > overlappingObjectsLabelsMap = new HashMap<>(  );

		LabelRegions< Integer > labelRegions = new LabelRegions( currentImgLabeling );

		for ( LabelRegion< Integer > region : labelRegions )
		{
			final HashMap< Integer, Long > overlapSizes = computeOverlaps( previousLabeling, region );

			if ( overlapSizes.isEmpty() )
			{
				overlappingObjectsLabelsMap.put( region.getLabel(), new ArrayList<>() );
			}
			else
			{
				if ( overlapSizes.size() > 1 )
				{
//					Utils.log( "Object at "
//							+ " x = " + ( int ) region.getCenterOfMass().getDoublePosition( 0 )
//							+ " y = " + ( int ) region.getCenterOfMass().getDoublePosition( 1 )
//							+ " overlaps with " + overlapSizes.size() + " objects in previous frame." );
				}

				final ArrayList< Integer > trulyOverlappingObjectLabels
						= getTrulyOverlappingObjectLabels(
								intensities.get( t - 1 ),
								intensities.get( t ),
								previousLabeling,
								currentLabeling,
								region,
								overlapSizes );

				overlappingObjectsLabelsMap.put( region.getLabel(), trulyOverlappingObjectLabels );

			}

		}
		return overlappingObjectsLabelsMap;
	}


	public ArrayList< Integer > getTrulyOverlappingObjectLabels( RandomAccessibleInterval< T > previousIntensityImage,
												RandomAccessibleInterval< T > currentIntensityImage,
												RandomAccessibleInterval< IntType > previousLabeling,
												RandomAccessibleInterval< IntType > currentLabeling,
												LabelRegion< Integer > currentObjectRegion,
												HashMap< Integer, Long > overlapSizes )
	{

		final ArrayList< Integer > trulyOverlappingObjectLabels = new ArrayList<>();

		if ( overlapSizes.keySet().size() == 1 )
		{
			trulyOverlappingObjectLabels.add( overlapSizes.keySet().iterator().next() );
		}
		else
		{
			for ( int previousLabel : overlapSizes.keySet() )
			{
				final long previousObjectSize = Measurements.measureSizeInPixels( previousLabeling, previousLabel );

				final double overlapFraction = 1.0 * overlapSizes.get( previousLabel ).longValue() / previousObjectSize;

				// Utils.log( "Previous object size / overlap size: " + overlapFraction );

				if ( overlapFraction >= settings.minimalOverlapFraction )
				{
					trulyOverlappingObjectLabels.add( previousLabel );
				}
			}

			// Utils.log( "Corrected number of overlapping objects: " + trulyOverlappingObjectLabels.size() );

		}

		return trulyOverlappingObjectLabels;
	}

	private double getPreviousIntensitySum( HashMap< Integer, Double > previousIntensities )
	{
		double previousIntensitySum = 0;

		for ( double intensity : previousIntensities.values() )
		{
			previousIntensitySum += intensity;

		}
		return previousIntensitySum;
	}

	public < I extends IntegerType< I > >  HashMap< Integer, Long > computeOverlaps(
			RandomAccessibleInterval< I > previousLabeling,
			LabelRegion region )
	{
		final HashMap< Integer, Long > overlaps = new HashMap<>();

		final RandomAccess< I > previousLabelsAccess = previousLabeling.randomAccess();
		Cursor< I > cursor = region.inside().cursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			previousLabelsAccess.setPosition( cursor );
			final int previousLabel = previousLabelsAccess.get().getInteger();

			if ( previousLabel != 0 )
			{
				addOverlap( overlaps, previousLabel );
			}
		}

		return overlaps;
	}


	public void addOverlap( HashMap< Integer, Long > overlaps, int integer )
	{
		if ( overlaps.containsKey( integer ) )
		{
			overlaps.put( integer,  overlaps.get( integer ).longValue() + 1 );
		}
		else
		{
			overlaps.put( integer,  1L );
		}
	}

	public ArrayList< RandomAccessibleInterval< IntType > > getLabelings()
	{
		return labelings;
	}

	public void setLabelings( ArrayList< RandomAccessibleInterval< IntType > > labelings )
	{
		this.labelings = labelings;
	}
}
