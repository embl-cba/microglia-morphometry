/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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
package de.embl.cba.microglia.measure;

import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.Algorithms;
import de.embl.cba.microglia.morphometry.regions.Regions;
import de.embl.cba.microglia.morphometry.skeleton.SkeletonAnalyzer;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import inra.ijpb.geometry.Ellipse;
import inra.ijpb.measure.region2d.Convexity;
import inra.ijpb.measure.region2d.GeodesicDiameter;
import inra.ijpb.measure.region2d.InertiaEllipse;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.microglia.morphometry.Algorithms.getMaximumLocation;

public class Measurements
{
	public static final String CENTROID = "Centroid";

	public static final String VOLUME = "Volume";
	public static final String AREA = "Area";
	public static final String LENGTH = "Length";

	public static final String PERIMETER = "Perimeter";
	public static final String SURFACE = "Surface";

	public static final String PIXEL_UNIT = "Pixel";
	public static final String POW = ""; // the ^ character felt to risky

	public static final String SUM_INTENSITY = "SumIntensity";
	public static final String IMAGE_BOUNDARY_CONTACT = "ImageBoundaryContact";

	public static final String GLOBAL_BACKGROUND_INTENSITY = "GlobalBackgroundIntensity";
	public static final String SKELETON_TOTAL_LENGTH = "SkeletonTotalLength";
	public static final String SKELETON_NUMBER_BRANCH_POINTS = "SkeletonNumBranchPoints";
	public static final String SKELETON_AVG_BRANCH_LENGTH = "SkeletonAvgBranchLength";
	public static final String SKELETON_LONGEST_BRANCH_LENGTH = "SkeletonLongestBranchLength";

	public static final String SEP = "_";
	public static final String FRAME_UNITS = "Frames";
	public static final String TIME = "Time";
	public static final String VOXEL_SPACING = "VoxelSpacing";
	public static final String FRAME_INTERVAL = "FrameInterval";
	public static final String BRIGHTEST_POINT = "BrightestPoint";
	public static final String RADIUS_AT_BRIGHTEST_POINT = "RadiusAtBrightestPoint";
	public static final String CONVEX_AREA = "ConvexArea";

	public static final String[] XYZ = new String[]{"X","Y","Z"};

	public static String getVolumeName( int numDimensions )
	{
		if ( numDimensions == 1 ) return LENGTH;
		if ( numDimensions == 2 ) return AREA;
		if ( numDimensions == 3 ) return VOLUME;

		return null;
	}

	public static String getSurfaceName( int numDimensions )
	{
		if ( numDimensions == 1 ) return LENGTH;
		if ( numDimensions == 2 ) return PERIMETER;
		if ( numDimensions == 3 ) return SURFACE;

		return null;
	}


	public static void measureCentroids(
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			ImgLabeling< Integer, IntType > imgLabeling,
			double[] calibration,
			RandomAccessibleInterval< BitType > annotation,
			String unit )
	{
		String[] XYZ = new String[]{"X","Y","Z"};

		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		for ( LabelRegion labelRegion : labelRegions )
		{
			final int label = ( int ) ( labelRegion.getLabel() );

			final double[] position = new double[ 3 ];

			labelRegion.getCenterOfMass().localize( position );

			for ( int d = 0; d < position.length; ++d )
			{
				if ( calibration != null ) position[ d ] *= calibration[ d ];

				addMeasurement(
						objectMeasurements,
						label,
						CENTROID + SEP + XYZ[ d ] + SEP + unit,
						position[ d ] );

				if ( annotation != null )
					drawPosition( annotation, Utils.asLongs( position ) );
			}
		}
	}


	public static < T extends RealType< T > & NativeType< T > >
	void measureBrightestPoints(
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			ImgLabeling< Integer, IntType > imgLabeling,
			RandomAccessibleInterval< T > intensity,
			double[] calibration,
			RandomAccessibleInterval< BitType > annotation,
			int gaussianBlurSigma )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		for ( LabelRegion labelRegion : labelRegions )
		{
			final RandomAccessibleInterval< T > blur =
					getBlurredIntensityImage( intensity, labelRegion, gaussianBlurSigma );

			final RealPoint maximumLocation = getMaximumLocation( blur, null );
			final double[] pixelUnitsPosition = new double[ maximumLocation.numDimensions() ];
			maximumLocation.localize( pixelUnitsPosition );

			for ( int d = 0; d < pixelUnitsPosition.length; ++d )
			{
				double calibratedPosition = pixelUnitsPosition[ d ];

				if ( calibration != null ) calibratedPosition *= calibration[ d ];

				addMeasurement(
						objectMeasurements,
						( int ) ( labelRegion.getLabel() ),
						getCoordinateName( BRIGHTEST_POINT, d ),
						calibratedPosition  );
			}

			drawPosition( annotation, Utils.asLongs( pixelUnitsPosition ) );

			final RandomAccessibleInterval< DoubleType > squaredDistances
					= Algorithms.computeSquaredDistances( Regions.asMask( labelRegion ) );

			final RandomAccess< DoubleType > access = squaredDistances.randomAccess();

			access.setPosition( Utils.asLongs( pixelUnitsPosition ) );
			final double distanceAtPosition = Math.sqrt( access.get().getRealDouble() );

			addMeasurement(
					objectMeasurements,
					( int ) ( labelRegion.getLabel() ),
					RADIUS_AT_BRIGHTEST_POINT + SEP + PIXEL_UNIT,
					distanceAtPosition );

		}
	}

	public static String getCoordinateName( String brightestPoint, int dimension )
	{
		return brightestPoint + SEP + XYZ[ dimension ] + SEP + PIXEL_UNIT;
	}

	public static void drawPosition(
			RandomAccessibleInterval< BitType > annotation,
			long[] position )
	{
		if ( annotation != null )
		{
			final RandomAccess< BitType > access = annotation.randomAccess();
			access.setPosition( position );
			access.get().set( true );
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > getBlurredIntensityImage(
			RandomAccessibleInterval< T > intensity,
			LabelRegion labelRegion,
			int gaussianBlurSigma )
	{
		final RandomAccessibleInterval< T > crop =
				Utils.copyAsArrayImg( Views.interval( intensity, labelRegion ) );
		final RandomAccessibleInterval mask = Utils.asMask( labelRegion );
		Utils.applyMask( crop, mask );
		final RandomAccessibleInterval< T > blur =
				Utils.copyAsArrayImg( crop );
		Gauss3.gauss( gaussianBlurSigma, Views.extendBorder( crop ), blur ) ;
//		ImageJFunctions.show( blur );
		return blur;
	}

	public static void measureVolumes( HashMap<Integer, Map<String, Object>> objectMeasurements,
									   ImgLabeling<Integer, IntType> imgLabeling )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );
		for ( LabelRegion labelRegion : labelRegions )
		{
			final int label = ( int ) ( labelRegion.getLabel() );
			addMeasurement(
					objectMeasurements,
					label,
					getVolumeName( labelRegion.numDimensions() )
							+ SEP + PIXEL_UNIT + POW + labelRegion.numDimensions(),
					labelRegion.inside().size() );
		}
	}

	public static void measureSurface( HashMap<Integer, Map<String, Object>> objectMeasurements,
									   ImgLabeling<Integer, IntType> imgLabeling,
									   OpService opService )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );
		for ( LabelRegion labelRegion : labelRegions )
		{
			final int label = ( int ) ( labelRegion.getLabel() );

			final RandomAccessibleInterval< BitType > mask = Regions.asMask( labelRegion );

			// See: https://forum.image.sc/t/measure-surface-perimeter-in-imglib2/21213

			final Polygon2D contour = opService.geom().contour( mask, true );
			final double boundarySize = opService.geom().boundarySize( contour ).getRealDouble();

			addMeasurement( objectMeasurements,
					label,
					getSurfaceName( labelRegion.numDimensions() ) + SEP + PIXEL_UNIT,
					boundarySize );
		}
	}


	public static void measureSkeletons( HashMap<Integer, Map<String, Object>> objectMeasurements,
										 ImgLabeling<Integer, IntType> imgLabeling,
										 RandomAccessibleInterval< BitType > skeleton,
										 OpService opService )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		for ( LabelRegion labelRegion : labelRegions )
			measureSkeleton( objectMeasurements, skeleton, opService, labelRegion );

	}

	public static void measureSkeleton(
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			RandomAccessibleInterval< BitType > skeleton,
			OpService opService,
			LabelRegion labelRegion )
	{
		final RandomAccessibleInterval< BitType > regionSkeleton =
				Regions.getMaskedAndCropped( skeleton, labelRegion );

		final SkeletonAnalyzer skeletonAnalyzer =
				new SkeletonAnalyzer( regionSkeleton, opService );

		final int label = ( int ) ( labelRegion.getLabel() );

		addMeasurement( objectMeasurements,
				label,
				 SKELETON_TOTAL_LENGTH + SEP + PIXEL_UNIT,
				skeletonAnalyzer.getTotalSkeletonLength() );

		addMeasurement( objectMeasurements,
				label,
				SKELETON_NUMBER_BRANCH_POINTS,
				skeletonAnalyzer.getNumBranchPoints() );

		addMeasurement( objectMeasurements,
				label,
				SKELETON_AVG_BRANCH_LENGTH + SEP + PIXEL_UNIT,
				skeletonAnalyzer.getAverageBranchLength() );

		addMeasurement( objectMeasurements,
				label,
				SKELETON_LONGEST_BRANCH_LENGTH + SEP + PIXEL_UNIT,
				skeletonAnalyzer.getLongestBranchLength() );
	}

	public static void measureMorpholibJFeatures(
			HashMap<Integer, Map<String, Object>> objectMeasurements,
			ImgLabeling<Integer, IntType> imgLabeling )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		for ( LabelRegion labelRegion : labelRegions )
			measureMorpholibJFeatures( objectMeasurements, labelRegion );

	}

	public static void measureMorpholibJFeatures(
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			LabelRegion labelRegion )
	{
		final int label = ( int ) ( labelRegion.getLabel() );

		final RandomAccessibleInterval< BitType > mask =
				Regions.asMask( labelRegion );

		final Calibration calibration = new Calibration();
		final ImageProcessor maskProcessor = ImageJFunctions.wrap( mask, "" ).getProcessor();
		final int[] labels = { 255 };

		final GeodesicDiameter.Result[] geodesicDiameters = GeodesicDiameter.geodesicDiameters(
				maskProcessor,
				labels,
				calibration );

		addMeasurement( objectMeasurements,
				label,
				"GeodesicDiameter" + SEP + PIXEL_UNIT,
				geodesicDiameters[ 0 ].diameter );

		addMeasurement( objectMeasurements,
				label,
				"LargestInscribedCircleRadius" + SEP + PIXEL_UNIT,
				geodesicDiameters[ 0 ].innerRadius );

		final Convexity.Result[] convexity
				= new Convexity().analyzeRegions( maskProcessor, labels, calibration );

		addMeasurement( objectMeasurements,
				label,
				CONVEX_AREA + SEP + PIXEL_UNIT + POW + 2,
				convexity[ 0 ].convexArea );

		final Ellipse[] ellipses =
				new InertiaEllipse().analyzeRegions( maskProcessor, labels, calibration );

		addMeasurement( objectMeasurements,
				label,
				"EllipsoidLongestAxisRadius" + SEP + PIXEL_UNIT,
				ellipses[ 0 ].radius1()  );

		addMeasurement( objectMeasurements,
				label,
				"EllipsoidShortestAxisRadius" + SEP + PIXEL_UNIT,
				ellipses[ 0 ].radius2()  );

	}

	public static void addMeasurement(
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			int objectLabel, String name, Object value )
	{
		if ( ! objectMeasurements.containsKey( objectLabel ) )
			objectMeasurements.put( objectLabel, new HashMap<>(  ) );

		objectMeasurements.get( objectLabel ).put( name, value );
	}

	private static < T extends RealType< T > & NativeType< T > >
	long measureSumIntensity( RandomAccess< T > imageRandomAccess, LabelRegion labelRegion )
	{
		Cursor cursor = labelRegion.inside().cursor();

		long sum = 0;

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			imageRandomAccess.setPosition( cursor );
			sum += imageRandomAccess.get().getRealDouble();
		}
		return sum;
	}

	private static < T extends RealType< T > & NativeType< T > >
	long measureImageBoundaryContact( LabelRegion labelRegion, long[] min, long[] max )
	{
		Cursor cursor = labelRegion.inside().cursor();
		long numBoundaryPixels = 0;

		final int numDimensions = min.length;
		final long[] position = new long[ numDimensions ];
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( position );
			for ( int d = 0; d < numDimensions ; d++ )
			{
				if ( position[ d ] == min[ d ] || position[ d ] == max[ d ])
				{
					numBoundaryPixels++;
					break;
				}
			}
		}
		return numBoundaryPixels;
	}

	public static < I extends IntegerType< I >  >
	long measureSizeInPixels( RandomAccessibleInterval< I > labeling,
							  int label )
	{
		final Cursor< I > labelCursor = Views.iterable( labeling ).localizingCursor();
		long size = 0;

		while ( labelCursor.hasNext() )
		{
			long value = labelCursor.next().getInteger();

			if( value == label )
			{
				size++;
			}
		}

		return size;
	}

	public static < T extends RealType< T > & NativeType< T > >
	double measureBgCorrectedSumIntensity( RandomAccessibleInterval< IntType > labeling,
										   int label,
										   RandomAccessibleInterval< T > image )
	{

		final Cursor< IntType > labelCursor = Views.iterable( labeling ).localizingCursor();
		final RandomAccess< T > intensityAccess = image.randomAccess();

		double sum = 0;
		double sumBg = 0;
		long nObject = 0;
		long nBackground = 0;
		int value;

		while ( labelCursor.hasNext() )
		{

			value = labelCursor.next().getInteger();

			if( value == label )
			{
				intensityAccess.setPosition( labelCursor );
				sum += intensityAccess.get().getRealDouble();
				nObject++;
			}
			else if ( value == 0 )
			{
				intensityAccess.setPosition( labelCursor );
				sumBg += intensityAccess.get().getRealDouble();
				nBackground++;
			}

		}

		final double meanBg = sumBg / nBackground;
		return ( sum - nObject * meanBg );

	}

	public static JTable asTable(
			ArrayList< HashMap< Integer, Map< String, Object > > > timepoints )
	{
		return Utils.createJTableFromStringList(
				measurementsAsTableRowsStringList( timepoints, "\t" ),
				"\t" );
	}

	public static ArrayList< String > measurementsAsTableRowsStringList(
			ArrayList< HashMap< Integer,
			Map< String, Object > > > measurementsTimePointList,
			String delim )
	{

		final Set< Integer > objectLabelsFirstTimePoint =
				measurementsTimePointList.get( 0 ).keySet();

		final Set< String > measurementSet =
				measurementsTimePointList.get( 0 ).get(
						objectLabelsFirstTimePoint.iterator().next() ).keySet();

		List< String  > measurementNames = new ArrayList< String >( measurementSet );
		Collections.sort( measurementNames );

		final ArrayList< String > lines = new ArrayList<>();

		String header = "Object_Label";
		header += delim + CENTROID + SEP + TIME + SEP + FRAME_UNITS;
		for ( String measurementName : measurementNames )
			header += delim + measurementName ;

		lines.add( header );

		for ( int t = 0; t < measurementsTimePointList.size(); ++t )
		{
			final HashMap< Integer, Map< String, Object > > measurements
					= measurementsTimePointList.get( t );

			final Set< Integer > objectLabels = measurements.keySet();

			for ( int label : objectLabels )
			{
				final Map< String, Object > measurementsMap = measurements.get( label );

				String values = String.format( "%05d", label );

				values += delim + String.format( "%05d", t + 1 ); // convert to one-based time points

				for ( String measurementName : measurementNames )
					values += delim + measurementsMap.get( measurementName );

				lines.add( values );
			}
		}

		return lines;
	}

	public static ArrayList< HashMap< Integer, Map< String, Object > > >  initMeasurements(
			int numTimepoints )
	{
		ArrayList< HashMap< Integer, Map< String, Object > > > measurementsTimepointList
				= new ArrayList<>();

		for ( int t = 0; t < numTimepoints; ++t )
		{
			final HashMap< Integer, Map< String, Object > > objectMeasurements = new HashMap<>();
			measurementsTimepointList.add( objectMeasurements );
		}

		return measurementsTimepointList;
	}


	public static < T extends RealType< T > & NativeType< T > >
	void measureSumIntensities( HashMap< Integer, Map< String, Object > > objectMeasurements,
								ImgLabeling< Integer, IntType > imgLabeling,
								RandomAccessibleInterval< T > image,
								String channel )
	{
		final RandomAccess< T > imageRandomAccess = image.randomAccess();

		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		for ( LabelRegion labelRegion : labelRegions )
		{
			long sum = measureSumIntensity( imageRandomAccess, labelRegion );
			addMeasurement( objectMeasurements,
					(int) labelRegion.getLabel(),
					SUM_INTENSITY + SEP + channel, sum );
		}
	}


	public static < T extends RealType< T > & NativeType< T > >
	void measureImageBoundaryContact(
			HashMap< Integer, Map< String, Object > > objectMeasurements,
			ImgLabeling< Integer, IntType > imgLabeling )
	{
		final LabelRegions< Integer > labelRegions = new LabelRegions<>( imgLabeling );

		final int numDimensions = imgLabeling.numDimensions();
		final long[] imgBoundaryMin = new long[ numDimensions ];
		final long[] imgBoundaryMax = new long[ numDimensions ];
		imgLabeling.min( imgBoundaryMin );
		imgLabeling.max( imgBoundaryMax );

		for ( LabelRegion labelRegion : labelRegions )
		{
			long numBoundaryPixels =
					measureImageBoundaryContact( labelRegion, imgBoundaryMin, imgBoundaryMax );

			addMeasurement( objectMeasurements,
					(int) labelRegion.getLabel(),
					IMAGE_BOUNDARY_CONTACT + SEP + PIXEL_UNIT,
					numBoundaryPixels );
		}
	}

	public static void addCalibration(
			ArrayList< HashMap< Integer, Map< String, Object > > > measurementsTimepointList,
			ImagePlus imagePlus )
	{
		final Calibration calibration = imagePlus.getCalibration();
		final double pixelWidth = calibration.pixelWidth;
		final double pixelHeight = calibration.pixelHeight;
		final double pixelDepth = calibration.pixelDepth;
		final String unit = calibration.getUnit();
		final double frameInterval = calibration.frameInterval;
		final String timeUnit = calibration.getTimeUnit();

		for ( int t = 0; t < measurementsTimepointList.size(); ++t )
		{
			final HashMap< Integer, Map< String, Object > > measurements =
					measurementsTimepointList.get( t );

			final Set< Integer > labels = measurements.keySet();

			for( int label : labels )
			{
				measurements.get( label ).put(
						VOXEL_SPACING + SEP + "X",
						pixelWidth );

				measurements.get( label ).put(
						VOXEL_SPACING + SEP + "Y",
						pixelHeight );

				measurements.get( label ).put(
						VOXEL_SPACING + SEP + "Z",
						pixelDepth );

				measurements.get( label ).put(
						VOXEL_SPACING + SEP + "Unit",
						unit );

				measurements.get( label ).put(
						FRAME_INTERVAL,
						frameInterval );

				measurements.get( label ).put(
						FRAME_INTERVAL + SEP + "Unit",
						timeUnit );
			}

		}
	}

	public static void measureCentroidsToBrightestPointsDistances(
			HashMap< Integer, Map< String, Object> > measurements )
	{
		final Set< Integer > objectLabels = measurements.keySet();

		for ( int objectLabel : objectLabels )
		{
			final double bx =
					(double) measurements.get( objectLabel )
							.get( getCoordinateName( BRIGHTEST_POINT, 0 ) );
			final double by =
					(double) measurements.get( objectLabel )
							.get( getCoordinateName( BRIGHTEST_POINT, 1 ) );
			final double cx =
					(double) measurements.get( objectLabel )
							.get( getCoordinateName( CENTROID, 0 ) );
			final double cy =
					(double) measurements.get( objectLabel )
							.get( getCoordinateName( CENTROID, 1 ) );

			double distance = Math.sqrt( 1.0 * ( bx - cx ) * ( bx - cx ) + 1.0 * ( by - cy ) * ( by - cy ) );

			addMeasurement( measurements,
					objectLabel,
					"BrightestPointToCentroidDistance" + SEP + PIXEL_UNIT,
					distance );
		}
	}
}
