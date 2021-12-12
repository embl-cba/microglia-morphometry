package de.embl.cba.spindle3d.util;

import net.imglib2.util.LinAlgHelpers;

import java.util.ArrayList;

public abstract class Vectors
{
	public static double[] middle( ArrayList< double[] > vectors )
	{
		double[] middle = new double[ 3 ];
		LinAlgHelpers.add( vectors.get( 0 ), vectors.get( 1 ), middle );
		for ( int d = 0; d < 3; d++ )
		{
			middle[ d ] = 0.5 * middle[ d ];
		}
		return middle;
	}

	public static double[] vector( double[] pos0, double[] pos1 )
	{
		final double[] vector = new double[ 3 ];

		LinAlgHelpers.subtract( pos0, pos1, vector );

		return vector;
	}

	public static double[] calibrate( double[] location, double voxelSize )
	{
		final double[] clone = location.clone();
		for ( int i = 0; i < clone.length; ++i )
			clone[ i ] *= voxelSize;
		return clone;
	}

	public static double[] componentWiseMultiplication( double[] v0, double[] v1 )
	{
		final double[] clone = v0.clone();
		for ( int i = 0; i < clone.length; ++i )
			clone[ i ] *= v1[ i ];
		return clone;
	}

	public static double[] componentWiseDivision( double[] v0, double[] v1 )
	{
		final double[] clone = v0.clone();
		for ( int i = 0; i < clone.length; ++i )
			clone[ i ] *= v1[ i ];
		return clone;
	}
}
