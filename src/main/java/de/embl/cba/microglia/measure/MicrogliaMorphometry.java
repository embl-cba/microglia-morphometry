package de.embl.cba.microglia.measure;

import de.embl.cba.microglia.measure.Measurements;
import de.embl.cba.morphometry.Logger;
import de.embl.cba.morphometry.Utils;
import de.embl.cba.morphometry.skeleton.SkeletonCreator;
import ij.measure.Calibration;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MicrogliaMorphometry < T extends RealType< T > & NativeType< T > >
{
	private final ArrayList< RandomAccessibleInterval< T > > labelMaps;
	private final ArrayList< RandomAccessibleInterval< T > > intensities;
	private final OpService opService;
	private final Calibration calibration;
	private ArrayList< HashMap< Integer, Map< String, Object > > > measurementsTimepointList;
	private ArrayList< RandomAccessibleInterval< BitType > > skeletons;
	private ArrayList< RandomAccessibleInterval< BitType > > annotations;

	public MicrogliaMorphometry( ArrayList< RandomAccessibleInterval< T > > labelMasks,
								 ArrayList< RandomAccessibleInterval< T > > intensities,
								 OpService opService,
								 Calibration calibration )
	{
		this.labelMaps = labelMasks;
		this.intensities = intensities;
		this.opService = opService;
		this.calibration = calibration;
	}

	public void run()
	{
		createSkeletons();
		annotations = new ArrayList<>(  );
		measurementsTimepointList = Measurements.initMeasurements( labelMaps.size() );
		performMeasurements();
	}

	private void performMeasurements( )
	{
		final int nt = labelMaps.size();

		for ( int t = 0; t < nt; ++t )
		{
			Logger.log( "Measuring morphometries, frame " + ( t + 1 ) + " / " + nt );

			final HashMap< Integer, Map< String, Object > > measurements =
					measurementsTimepointList.get( t );

			final ImgLabeling< Integer, IntType > imgLabeling =
					Utils.labelMapAsImgLabelingRobert( labelMaps.get( t ) );

			annotations.add( ArrayImgs.bits( Intervals.dimensionsAsLongArray( imgLabeling ) ) );

			Measurements.measureCentroids(
					measurements,
					imgLabeling,
					null,
					annotations.get( t ),
					Measurements.PIXEL_UNIT );

			// add calibrated position (needed for interactive display in segmentation annotator)
//			final double[] calibration = new double[ 3 ];
//			calibration[ 0 ] = this.calibration.pixelWidth;
//			calibration[ 1 ] = this.calibration.pixelHeight;
//			calibration[ 2 ] = this.calibration.pixelDepth;
//			Measurements.measureCentroids(
//					measurements,
//					imgLabeling,
//					calibration,
//					annotations.get( t ),
//					this.calibration.getUnit());

			Measurements.measureBrightestPoints(
					measurements,
					imgLabeling,
					intensities.get( t ),
					null,
					annotations.get( t ),
					3 );

			Measurements.measureCentroidsToBrightestPointsDistances(
					measurements
			);

			// Volumes ( = areas )
			Measurements.measureVolumes(
					measurements,
					imgLabeling);

			// Surfaces ( = perimeters )
			Measurements.measureSurface(
					measurements,
					imgLabeling,
					opService );

			// Surfaces ( = perimeters )
			Measurements.measureImageBoundaryContact(
					measurements,
					imgLabeling );

			Measurements.measureSkeletons(
					measurements,
					imgLabeling,
					skeletons.get( t ),
					opService );

			Measurements.measureMorpholibJFeatures(
					measurements,
					imgLabeling );
		}
	}

	private void createSkeletons( )
	{
		final SkeletonCreator skeletonCreator = new SkeletonCreator(
				Utils.labelMapsAsMasks( labelMaps ),
				opService );
		skeletonCreator.setClosingRadius( 3 );
		skeletonCreator.run();
		skeletons = skeletonCreator.getSkeletons();
	}

	public ArrayList< HashMap< Integer, Map< String, Object > > > getMeasurementsTimepointList()
	{
		return measurementsTimepointList;
	}

	public ArrayList< RandomAccessibleInterval< BitType > > getSkeletons()
	{
		return skeletons;
	}

	public ArrayList< RandomAccessibleInterval< BitType > > getAnnotations()
	{
		return annotations;
	}
}
