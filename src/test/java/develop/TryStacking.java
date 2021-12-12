package develop;

import clojure.main;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.view.Views;

import java.util.ArrayList;

public class TryStacking
{
	public static < R extends RealType< R > >  void main( String[] args )
	{
		final ArrayImg< BitType, LongArray > bits = ArrayImgs.bits( 100, 100 );
		final ArrayImg< ShortType, ShortArray > shorts = ArrayImgs.shorts( 100, 100 );

		final ArrayList< RandomAccessibleInterval< R > > list = new ArrayList<>();
		list.add( (RandomAccessibleInterval) shorts );
		list.add( (RandomAccessibleInterval) bits );

		final RandomAccessibleInterval< R > stack = Views.stack( list );
		final ImagePlus wrap = ImageJFunctions.wrap( stack, "" );
		wrap.show();
	}
}
