package de.embl.cba.microglia;

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
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static de.embl.cba.microglia.morphometry.Constants.SEGMENTATION;
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

	public static long[] asLongs( double[] doubles )
	{
		final long[] longs = new long[ doubles.length ];

		for ( int i = 0; i < doubles.length; ++i )
		{
			longs[ i ] = (long) doubles[ i ];
		}

		return longs;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > copyAsArrayImg( RandomAccessibleInterval< T > orig )
	{
		final RandomAccessibleInterval< T > copy =
				Views.translate(
						new ArrayImgFactory( Util.getTypeFromInterval( orig ) ).create( orig ),
						Intervals.minAsLongArray( orig ) );

		LoopBuilder.setImages( copy, orig ).forEachPixel( Type::set );

		return copy;
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
