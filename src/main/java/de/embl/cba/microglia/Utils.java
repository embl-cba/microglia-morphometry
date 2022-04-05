package de.embl.cba.microglia;

import de.embl.cba.morphometry.Logger;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

import static de.embl.cba.morphometry.Utils.labelingsAsImagePlus;

public abstract class Utils
{
	@NotNull
	public static ArrayList< RandomAccessibleInterval< IntType > > openLabels( File segmentationFile )
	{
		final ImagePlus labelsImp = de.embl.cba.morphometry.Utils.openWithBioFormats( segmentationFile.getAbsolutePath() );

		final ArrayList< RandomAccessibleInterval< UnsignedShortType > > unsignedShorts
				= de.embl.cba.morphometry.Utils.get2DImagePlusMovieAsFrameList(
				labelsImp,
				1 );

		return asIntType( unsignedShorts );
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

	public static < T extends RealType<T> & NativeType< T > > void saveLabels( ArrayList< RandomAccessibleInterval< T > > labels, Calibration calibration, String path )
	{
		final ImagePlus labelsImp = labelingsAsImagePlus( labels );
		labelsImp.setCalibration( calibration );
		IJ.run(labelsImp, "Enhance Contrast", "saturated=0.35");
		new FileSaver( labelsImp ).saveAsTiff( path );
		Logger.log( "Label images saved: " + path );
	}
}
