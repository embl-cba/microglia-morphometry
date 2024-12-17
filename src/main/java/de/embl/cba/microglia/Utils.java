package de.embl.cba.microglia;

import bdv.util.*;
import de.embl.cba.microglia.morphometry.Algorithms;
import de.embl.cba.microglia.morphometry.regions.Regions;
import de.embl.cba.microglia.table.ColumnClassAwareTableModel;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.process.LUT;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import net.imglib2.*;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.converter.Converters;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.embl.cba.microglia.Transforms.createTransformedInterval;
import static de.embl.cba.microglia.morphometry.Constants.*;
import static inra.ijpb.color.ColorMaps.createGoldenAngleLut;

public abstract class Utils
{
	@NotNull
	public static ArrayList< RandomAccessibleInterval< IntType > > openLabels( File segmentationFile )
	{
		final ImagePlus labelsImp = Utils.openAsImagePlus( segmentationFile );

		final ArrayList< RandomAccessibleInterval< UnsignedShortType > > unsignedShorts
				= Utils.get2DImagePlusMovieAsFrameList( labelsImp, 1 );

		return asIntType( unsignedShorts );
	}

	public static < T extends RealType< T > & NativeType< T > >
	ArrayList< RandomAccessibleInterval< T > >
	get2DImagePlusMovieAsFrameList(
			ImagePlus imagePlus,
			long channelOneBased )
	{
		return get2DImagePlusMovieAsFrameList(
				imagePlus,
				channelOneBased,
				1,
				imagePlus.getNFrames() );
	}

	public static < T extends RealType< T > & NativeType< T > >
	void saveRAIListAsMovie(
			ArrayList< RandomAccessibleInterval< T > > rais,
			Calibration calibration,
			String outputPath, String title )
	{

		final ImagePlus imp = getAsImagePlusMovie( rais, title );
		imp.setCalibration( calibration );
		new FileSaver( imp ).saveAsTiff( outputPath );
		IJ.log( "Movie saved: " + outputPath );
	}

	public static < T extends RealType< T > & NativeType< T > >
	ArrayList< RandomAccessibleInterval< BitType > >
	labelMapsAsMasks( ArrayList< RandomAccessibleInterval< T > > labelMaps )
	{
		final ArrayList< RandomAccessibleInterval< BitType > > masks = new ArrayList<>();

		long numTimePoints = labelMaps.size();

		for ( int t = 0; t < numTimePoints; ++t )
		{
			final RandomAccessibleInterval< BitType > mask = Utils.asMask( labelMaps.get( t ) );
			masks.add( mask );
		}

		return masks;
	}

	public static < T extends RealType< T > > ImgLabeling< Integer, IntType > labelMapAsImgLabelingRobert( RandomAccessibleInterval< T > labelMap )
	{
		final RandomAccessibleInterval< IntType > indexImg = ArrayImgs.ints( Intervals.dimensionsAsLongArray( labelMap ) );
		final ImgLabeling< Integer, IntType > imgLabeling = new ImgLabeling<>( indexImg );

		final Cursor< LabelingType< Integer > > labelCursor = Views.flatIterable( imgLabeling ).cursor();

		for ( final RealType input : Views.flatIterable( labelMap ) ) {

			final LabelingType< Integer > element = labelCursor.next();

			if ( input.getRealFloat() != 0 )
			{
				element.add( (int) input.getRealFloat() );
			}
		}

		return imgLabeling;
	}

	public static void addColumn( TableModel model, String column, Object defaultValue )
	{
		if ( model instanceof ColumnClassAwareTableModel )
		{
			( (ColumnClassAwareTableModel) model ).addColumnClass( defaultValue );
		}

		if ( model instanceof DefaultTableModel )
		{
			final Object[] rows = new Object[ model.getRowCount() ];
			Arrays.fill( rows, defaultValue );
			( (DefaultTableModel) model ).addColumn( column, rows );
		}
	}

	
	public static < T extends RealType< T > & NativeType< T > >
	ArrayList< RandomAccessibleInterval< T > > get2DImagePlusMovieAsFrameList(
			ImagePlus imagePlus,
			long channelOneBased,
			long tMinOneBased,
			long tMaxOneBased )
	{
		if ( imagePlus.getNSlices() != 1 )
		{
			IJ.error( "Only 2D images (one z-slice) are supported.");
			return null;
		}

		final Img< T > wrap = ImageJFunctions.wrap( imagePlus );

		ArrayList<  RandomAccessibleInterval< T > > frames = new ArrayList<>();

		for ( long t = tMinOneBased - 1; t < tMaxOneBased; ++t )
		{
			RandomAccessibleInterval< T > channel = extractChannel( imagePlus, channelOneBased, wrap );

			RandomAccessibleInterval< T > timepoint = extractTimePoint( imagePlus, t, channel );

			frames.add( copyAsArrayImg( timepoint ) );
		}

		return frames;
	}

	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T >
	extractTimePoint( ImagePlus imagePlus, long t, RandomAccessibleInterval< T > channel )
	{
		RandomAccessibleInterval< T > timepoint;

		if ( imagePlus.getNFrames() != 1 )
		{
			timepoint = Views.hyperSlice( channel, 2, t );
		}
		else
		{
			timepoint = channel;
		}

		return timepoint;
	}

	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > extractChannel( ImagePlus imagePlus, long channelOneBased, Img< T > wrap )
	{
		RandomAccessibleInterval< T > channel;

		if ( imagePlus.getNChannels() != 1 )
		{
			channel = Views.hyperSlice( wrap, 2, channelOneBased - 1);
		}
		else
		{
			channel = wrap;
		}
		return channel;
	}

	@NotNull
	public static ArrayList< RandomAccessibleInterval< IntType > > asIntType( ArrayList< RandomAccessibleInterval< UnsignedShortType > > unsignedShorts )
	{
		// the images are opened as UnsignedShortType
		// but in the code we are using IntType
		final ArrayList< RandomAccessibleInterval< IntType > > ints = new ArrayList<>();
		for ( RandomAccessibleInterval< UnsignedShortType > unsignedShort : unsignedShorts )
		{
			final RandomAccessibleInterval< IntType > intType =
					Converters.convert( unsignedShort, ( i, o ) -> o.setReal(  i.getRealDouble() ), new IntType() );
			ints.add( intType );
		}
		return ints;
	}

	public static String autoDelim( String delim, List< String > strings )
	{
		if ( delim == null )
		{
			if ( strings.get( 0 ).contains( "\t" ) )
			{
				delim = "\t";
			} else if ( strings.get( 0 ).contains( "," ) )
			{
				delim = ",";
			} else if ( strings.get( 0 ).contains( ";" ) )
			{
				delim = ";";
			} else
			{
				throw new RuntimeException( "Could not identify table delimiter." );
			}

		}
		return delim;
	}

	public static JTable createJTableFromStringList( List< String > strings, String delim )
	{
		delim = autoDelim( delim, strings );

		StringTokenizer st = new StringTokenizer( strings.get( 0 ), delim );

		List< String > colNames = new ArrayList<>();

		while ( st.hasMoreTokens() )
			colNames.add( st.nextToken() );

		/**
		 * Init model and columns
		 */

		ColumnClassAwareTableModel model = new ColumnClassAwareTableModel();

		for ( String colName : colNames )
			model.addColumn( colName );

		int numCols = colNames.size();

		/**
		 * Add tablerow entries
		 */

		for ( int iString = 1; iString < strings.size(); ++iString )
		{
			model.addRow( new Object[ numCols ] );

			st = new StringTokenizer( strings.get( iString ), delim );

			for ( int iCol = 0; iCol < numCols; iCol++ )
			{
				String stringValue = st.nextToken();

				try
				{
					final Double numericValue = Utils.parseDouble( stringValue );
					model.setValueAt( numericValue, iString - 1, iCol );
				} catch ( Exception e )
				{
					model.setValueAt( stringValue, iString - 1, iCol );
				}
			}

		}

		model.refreshColumnClassesFromObjectColumns();

		return new JTable( model );
	}

	public static Double parseDouble( String cell )
	{
		if ( cell.equalsIgnoreCase( "nan" )
				|| cell.equalsIgnoreCase( "na" )
				|| cell.equals( "" ) )
			return Double.NaN;
		else if ( cell.equalsIgnoreCase( "inf" ) )
			return Double.POSITIVE_INFINITY;
		else if ( cell.equalsIgnoreCase( "-inf" ) )
			return Double.NEGATIVE_INFINITY;
		else
			return Double.parseDouble( cell );
	}

	public static < T extends RealType<T> & NativeType< T > > void saveLabels( ArrayList< RandomAccessibleInterval< T > > labels, Calibration calibration, String path )
	{
		final ImagePlus labelsImp = labelingsAsImagePlus( labels );
		labelsImp.setCalibration( calibration );
		IJ.run(labelsImp, "Enhance Contrast", "saturated=0.35");
		new FileSaver( labelsImp ).saveAsTiff( path );
		IJ.log( "Label images saved: " + path );
	}

	public static < T extends RealType< T > & NativeType< T > >
	ImagePlus getAsImagePlusMovie(
			ArrayList< RandomAccessibleInterval< T > > rais2D,
			String title )
	{
		RandomAccessibleInterval movie = Views.stack( rais2D );
		movie = Views.addDimension( movie, 0, 0);
		movie = Views.addDimension( movie, 0, 0);
		movie = Views.permute( movie, 2,4 );
		final ImagePlus imp = new Duplicator().run( ImageJFunctions.wrap( movie, title ) );
		imp.setTitle( title );
		return imp;
	}

	public static double[] as2dDoubleArray( double value )
	{
		double[] array = new double[ 2 ];
		Arrays.fill( array, value );
		return array;
	}

	public static double[] as3dDoubleArray( double value )
	{
		double[] array = new double[ 3 ];
		Arrays.fill( array, value );
		return array;
	}

	public static LUT getGoldenAngleLUT()
	{
		byte[][] bytes = createGoldenAngleLut( 256 );
		final byte[][] rgb = new byte[ 3 ][ 256 ];

		for ( int c = 0; c < 3; ++c )
		{
			rgb[ c ][ 0 ] = 0; // Black background
		}

		for ( int c = 0; c < 3; ++c )
		{
			for ( int i = 1; i < 256; ++i )
			{
				rgb[ c ][ i ] = bytes[ i ][ c ];
			}
		}

		LUT lut = new LUT( rgb[ 0 ], rgb[ 1 ], rgb[ 2 ] );
		return lut;
	}

	public static void addColumn( JTable table, String column, Object[] values )
	{
		addColumn( table.getModel(), column, values );
	}

	public static void addColumn( TableModel model, String column, Object[] values )
	{
		if ( model instanceof ColumnClassAwareTableModel )
			( (ColumnClassAwareTableModel) model ).addColumnClass( values[ 0 ] );

		if ( model instanceof DefaultTableModel )
			( (DefaultTableModel) model ).addColumn( column, values );
	}

	public static void addColumn( JTable table, String column, Object defaultValue )
	{
		addColumn( table.getModel(), column, defaultValue );
	}

	public static < T extends RealType< T > & NativeType< T > >
	ImagePlus labelingsAsImagePlus( ArrayList< RandomAccessibleInterval< T > > labelings )
	{
		ImagePlus segmentationImp = getAsImagePlusMovie( labelings, SEGMENTATION );
		segmentationImp.setLut( getGoldenAngleLUT() );
		segmentationImp.setTitle( SEGMENTATION );
		return segmentationImp;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createRescaledArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		assert scalingFactors.length == input.numDimensions();

		/**
		 * - In principle, writing a function that computes weighted averages
		 *   of an appropriate number of neighboring (not only nearest) pixels
		 *   around each requested (real) position in the new image appears to me
		 *   the most straight-forward way of rescaling.
		 * - However, in practice, blurring and subsequent re-sampling seems to be more commonly done,
		 *   maybe for implementation efficiency?
		 * - http://imagej.1557.x6.nabble.com/downsampling-methods-td3690444.html
		 * - https://github.com/axtimwalde/mpicbg/blob/050bc9110a186394ea15190fd326b3e32829e018/mpicbg/src/main/java/mpicbg/ij/util/Filter.java#L424
		 * - https://imagej.net/Downsample
		 */

		/*
		 * Blur image
		 */

		final RandomAccessibleInterval< T > blurred =
				createOptimallyBlurredArrayImg( input, scalingFactors );

		/*
		 * Sample values from blurred image
		 */

		final RandomAccessibleInterval< T > resampled =
				createResampledArrayImg( blurred, scalingFactors );

		return resampled;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createRescaledCellImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		assert scalingFactors.length == input.numDimensions();

		/**
		 * - In principle, writing a function that computes weighted averages
		 *   of an appropriate number of neighboring (not only nearest) pixels
		 *   around each requested (real) position in the new image appears to me
		 *   the most straight-forward way of rescaling.
		 * - However, in practice, blurring and subsequent re-sampling seems to be more commonly done,
		 *   maybe for implementation efficiency?
		 * - http://imagej.1557.x6.nabble.com/downsampling-methods-td3690444.html
		 * - https://github.com/axtimwalde/mpicbg/blob/050bc9110a186394ea15190fd326b3e32829e018/mpicbg/src/main/java/mpicbg/ij/util/Filter.java#L424
		 * - https://imagej.net/Downsample
		 */

		/*
		 * Blur image
		 */

		final RandomAccessibleInterval< T > blurred =
				createOptimallyBlurredCellImg( input, scalingFactors );

		/*
		 * Sample values from blurred image
		 */

		final RandomAccessibleInterval< T > resampled =
				createResampledArrayImg( blurred, scalingFactors );

		return resampled;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createResampledArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		// Convert to RealRandomAccessible such that we can obtain values at (infinite) non-integer coordinates
		RealRandomAccessible< T > rra =
				Views.interpolate( Views.extendBorder( input ),
						new ClampingNLinearInterpolatorFactory<>() );

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

	public static double[] get2dCalibration( Calibration calibration )
	{
		double[] calibration2D = new double[ 2 ];

		calibration2D[ X ] = calibration.pixelWidth;
		calibration2D[ Y ] = calibration.pixelHeight;

		return calibration2D;
	}

	public static < T extends RealType< T > & NativeType< T > >
	boolean isLateralBoundaryPixel( Neighborhood< T > cursor, RandomAccessibleInterval< T > rai )
	{
		int numDimensions = rai.numDimensions();
		final long[] position = new long[ numDimensions ];
		cursor.localize( position );

		for ( int d = 0; d < numDimensions - 1; ++d )
		{
			if ( position[ d ] == rai.min( d ) ) return true;
			if ( position[ d ] == rai.max( d ) ) return true;
		}

		return false;

	}

	public static < T extends AbstractIntegerType< T > >
	Set< Long > computeUniqueValues( RandomAccessibleInterval< T > rai )
	{
		final Set< Long > unique = new HashSet<>(  );

		final Cursor< T > cursor = Views.iterable( rai ).cursor();

		while ( cursor.hasNext() )
		{
			unique.add( cursor.next().getIntegerLong() );
		}

		return unique;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > invertedView( RandomAccessibleInterval< T > input )
	{
		final double maximum = Algorithms.getMaximumValue( input );
		IJ.log( "Inverting image; maximum value is: " + maximum );

		final RandomAccessibleInterval< T > inverted = Converters.convert( input, ( i, o ) -> {
			o.setReal( ( int ) ( maximum - i.getRealDouble() ) );
		},  Views.iterable( input ).firstElement() );

		return inverted;
	}

	public static void addRelativeImagePathColumn(
			JTable table,
			String imagePath,
			String rootPath,
			String imageName )
	{
		if ( rootPath == null ) return;
		final Path relativeImagePath = getRelativePath( imagePath, rootPath );
		addColumn( table, "Path_" + imageName, relativeImagePath );
	}

	public static void openURI( String uri )
	{
		try
		{
			java.awt.Desktop.getDesktop().browse( new URI( uri ));
		} catch ( IOException e )
		{
			e.printStackTrace();
		} catch ( URISyntaxException e )
		{
			e.printStackTrace();
		}
	}

	public static void drawObject( RandomAccessibleInterval< IntType > img,
								   LabelRegion labelRegion,
								   int value )
	{
		final Cursor< Void > regionCursor = labelRegion.inside().cursor();
		final RandomAccess< IntType > access = img.randomAccess();
		BitType bitTypeTrue = new BitType( true );
		while ( regionCursor.hasNext() )
		{
			regionCursor.fwd();
			access.setPosition( regionCursor );
			access.get().set( value );
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	int getNumObjects( RandomAccessibleInterval< T > mask )
	{
		final LabelRegions labelRegions = new LabelRegions( Regions.asImgLabeling( mask, ConnectedComponents.StructuringElement.FOUR_CONNECTED )  );
		return labelRegions.getExistingLabels().size() - 1;
	}

	public static Path getRelativePath( String pathA, String pathB )
	{
		final Path imagePath = Paths.get( pathA );
		final Path tablePath = Paths.get( pathB );

		return tablePath.relativize( imagePath );
	}


	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > getEnlargedRai( RandomAccessibleInterval< T > rai, int border )
	{
		int n = rai.numDimensions();

		long[] min = new long[ n ];
		long[] max = new long[ n ];

		rai.min( min );
		rai.max( max );

		for ( int d = 0; d < n; ++d )
		{
			min[ d ] -= border;
			max[ d ] += border;

		}

		final FinalInterval interval = new FinalInterval( min, max );
		return Views.interval( Views.extendZero( rai ), interval );
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > createEmptyCopy( RandomAccessibleInterval< R > image )
	{
		RandomAccessibleInterval< R > copy =
				new ArrayImgFactory( Util.getTypeFromInterval( image ) ).create( image );
		copy = Views.translate( copy, Intervals.minAsLongArray( image ) );
		return copy;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createBlurredRai(
			RandomAccessibleInterval< T > rai,
			double sigma,
			double scaling )
	{
		ImgFactory< T > imgFactory = new ArrayImgFactory( rai.randomAccess().get()  );

		RandomAccessibleInterval< T > blurred = imgFactory.create( Intervals.dimensionsAsLongArray( rai ) );

		blurred = Views.translate( blurred, Intervals.minAsLongArray( rai ) );

		Gauss3.gauss( sigma / scaling, Views.extendBorder( rai ), blurred ) ;

		return blurred;
	}

	public static < T extends RealType< T > & NativeType< T > >
	void setValues( RandomAccessibleInterval< T > rai, double value )
	{
		Cursor< T > cursor = Views.iterable( rai ).localizingCursor();

		while ( cursor.hasNext() )
		{
			cursor.next().setReal( value );
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	void show( RandomAccessibleInterval rai,
			   String title,
			   List< RealPoint > points,
			   double[] calibration,
			   boolean resetViewTransform )
	{
		final Bdv bdv;

		BdvStackSource bdvSource;
		if ( rai.numDimensions() ==  2 )
			bdvSource = BdvFunctions.show( rai, title, BdvOptions.options().is2D().sourceTransform( calibration ) );
		else
			bdvSource = BdvFunctions.show( rai, title, BdvOptions.options().sourceTransform( calibration ) );

		bdv = bdvSource.getBdvHandle();


		if ( points != null )
		{
			BdvOverlay bdvOverlay = new BdvPointListOverlay( points, 5.0 );
			BdvFunctions.showOverlay( bdvOverlay, "overlay", BdvOptions.options().addTo( bdv ) );
		}

		if ( resetViewTransform )
		{
			resetViewTransform( bdv );
		}

		bdvSource.setDisplayRange( 0, Algorithms.getMaximumValue( rai ) );

	}

	private static void resetViewTransform( Bdv bdv )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		affineTransform3D.scale( 2.5D );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );

	}


	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createOptimallyBlurredArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		/**
		 * - https://en.wikipedia.org/wiki/Decimation_(signal_processing)
		 * - Optimal blurring is 0.5 / M, where M is the downsampling factor
		 */

		final double[] sigmas = new double[input.numDimensions() ];

		for ( int d = 0; d < input.numDimensions(); ++d )
			sigmas[ d ] = 0.5 / scalingFactors[ d ];

		// allocate output image
		RandomAccessibleInterval< T > output = createEmptyArrayImg( input );

		// blur input image and write into output image
		Gauss3.gauss( sigmas, Views.extendBorder( input ), output ) ;

		return output;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createOptimallyBlurredCellImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		/**
		 * - https://en.wikipedia.org/wiki/Decimation_(signal_processing)
		 * - Optimal blurring is 0.5 / M, where M is the downsampling factor
		 */

		final double[] sigmas = new double[input.numDimensions() ];

		for ( int d = 0; d < input.numDimensions(); ++d )
			sigmas[ d ] = 0.5 / scalingFactors[ d ];

		// allocate output image
		RandomAccessibleInterval< T > output = createEmptyCellImg( input );

		// blur input image and write into output image
		Gauss3.gauss( sigmas, Views.extendBorder( input ), output ) ;

		return output;
	}

	public static ImagePlus openAsImagePlus( File file )
	{
		if ( file.toString().startsWith( "http:/" ) || file.toString().startsWith( "https:/" ) )
		{
			String url = file.toString();
			url = url.replace( "http:/", "http://" );
			url = url.replace( "https:/", "https://" );
			return openAsImagePlus( url );
		}
		else
		{
			return openAsImagePlus( file.getPath() );
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > copyAsArrayImg( RandomAccessibleInterval< T > orig )
	{
		RandomAccessibleInterval< T > copy = new ArrayImgFactory( orig.randomAccess().get() ).create( orig );
		copy = Transforms.getWithAdjustedOrigin( orig, copy );
		LoopBuilder.setImages( copy, orig ).forEachPixel( ( c, o ) -> c.set( o ) );

		return copy;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createEmptyArrayImg( RandomAccessibleInterval< T > rai )
	{
		RandomAccessibleInterval< T > newImage = new ArrayImgFactory( rai.randomAccess().get() ).create( rai );
		newImage = Transforms.getWithAdjustedOrigin( rai, newImage );
		return newImage;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createEmptyCellImg( RandomAccessibleInterval< T > volume )
	{
		final int dimensionX = ( int ) volume.dimension( 0 );
		final int dimensionY = ( int ) volume.dimension( 1 );
		final int dimensionZ = ( int ) volume.dimension( 2 );

		int nz = dimensionZ;
		if ( AbstractImg.numElements( Intervals.dimensionsAsLongArray( volume ) ) >  Integer.MAX_VALUE - 1 )
			nz  = ( Integer.MAX_VALUE / 2 ) / ( dimensionX * dimensionY );

		final int[] cellSize = {
				dimensionX,
				dimensionY,
				nz };

		RandomAccessibleInterval< T > newImage = new CellImgFactory<>(
				volume.randomAccess().get(),
				cellSize ).create( volume );

		newImage = Transforms.getWithAdjustedOrigin( volume, newImage );
		return newImage;
	}

	public static long[] asLongs( double[] doubles )
	{
		final long[] longs = new long[ doubles.length ];

		for ( int i = 0; i < doubles.length; ++i )
		{
			longs[ i ] = (long) doubles[ i ];
		}

		return longs;
	}

	public static  < T extends RealType< T > & NativeType< T > >
	void applyMask(
			RandomAccessibleInterval< T > rai,
			RandomAccessibleInterval< BitType > mask )
	{
		final Cursor< T > cursor = Views.iterable( rai ).cursor();
		final OutOfBounds< BitType > maskAccess =
				Views.extendZero( mask ).randomAccess();

		while ( cursor.hasNext() )
		{
			cursor.fwd();
			maskAccess.setPosition( cursor );
			if ( ! maskAccess.get().get() )
				cursor.get().setZero();
		}
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< BitType > asMask( RandomAccessibleInterval< T > rai )
	{
		RandomAccessibleInterval< BitType > mask =
				ArrayImgs.bits( Intervals.dimensionsAsLongArray( rai ) );
		mask = Transforms.getWithAdjustedOrigin( rai, mask  );
		final RandomAccess< BitType > maskAccess = mask.randomAccess();

		final Cursor< T > cursor = Views.iterable( rai ).cursor();

		while ( cursor.hasNext() )
		{
			cursor.fwd();

			if ( cursor.get().getRealDouble() > 0 )
			{
				maskAccess.setPosition( cursor );
				maskAccess.get().set( true );
			}
		}

		return mask;
	}

	public static ImagePlus openAsImagePlus( String path )
	{
		if ( path.contains( "http" ) )
		{
			return IJ.openImage( path );
		}
		else
		{
			try
			{
				ImporterOptions opts = new ImporterOptions();
				opts.setId( path );
				opts.setVirtual( true );

				ImportProcess process = new ImportProcess( opts );
				process.execute();

				ImagePlusReader impReader = new ImagePlusReader( process );

				ImagePlus[] imps = impReader.openImagePlus();
				return imps[ 0 ];
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				throw new RuntimeException( e );
			}
		}
	}

}
