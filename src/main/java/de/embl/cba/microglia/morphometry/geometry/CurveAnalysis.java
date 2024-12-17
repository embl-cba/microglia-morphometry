/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package de.embl.cba.microglia.morphometry.geometry;

import de.embl.cba.microglia.morphometry.CoordinateAndValue;

import java.util.ArrayList;

import static java.lang.Math.abs;

public abstract class CurveAnalysis
{
	// TODO: instead of CoordinatesAndValues one should use a 1D RealRandomAccessible (or, in fact, a PhysicalImg...)
	public static CoordinatesAndValues derivative( CoordinatesAndValues coordinatesAndValues, int di )
	{
		final CoordinatesAndValues derivative = new CoordinatesAndValues();

		for ( int i = di / 2 + 1; i < coordinatesAndValues.values.size() - ( di / 2 + 1 ); ++i )
		{

			final int center = i;
			final int right = i + di / 2;
			final int left = i - di / 2;

			derivative.values.add(
					coordinatesAndValues.values.get( right )
							- coordinatesAndValues.values.get( left ) );

			derivative.coordinates.add( coordinatesAndValues.coordinates.get( center ) );
		}

		return derivative;
	}

	// TODO:
//	public static CoordinatesAndValues derivative( CoordinateToValue cv, int di )
//	{
//		final CoordinatesAndValues derivative = new CoordinatesAndValues();
//
//		for ( int i = di / 2 + 1; i < cv.size() - di / 2 - 1; ++i )
//		{
//			derivative.values.add( cv.values.get( i + di / 2 ) - cv.values.get( i - di / 2 ) );
//			derivative.coordinates.add( 0.5 * ( cv.coordinates.get( i + di / 2 ) + cv.coordinates.get( i - di / 2 ) ));
//		}
//
//		return derivative;
//	}

	public static ArrayList< Double > computeAbsoluteDerivatives( ArrayList< Double > values, int di )
	{
		final ArrayList< Double > derivatives = new ArrayList<>();

		for ( int i = di / 2 + 1; i < values.size() - di / 2 - 1; ++i )
		{
			derivatives.add( abs( values.get( i + di / 2 ) - values.get( i - di / 2 ) ) );
		}

		return derivatives;
	}

	public static double computeFWHM( CoordinatesAndValues coordinatesAndValues )
	{

		final IndexAndValue indexAndValue = maximumIndexAndValue( coordinatesAndValues );

		final int n = coordinatesAndValues.values.size();

		double halfMaxLoc1 = 0.0, halfMaxLoc2 = 0.0;

		for ( int i = indexAndValue.index; i < n; i++ )
		{
			if ( coordinatesAndValues.values.get( i ) < indexAndValue.value / 2.0 )
			{
				halfMaxLoc2 = 0.5 * ( coordinatesAndValues.values.get( i - 1 ) + coordinatesAndValues.values.get( i ) );
				break;
			}
		}

		for ( int i = indexAndValue.index; i >= 0; i-- )
		{
			if ( coordinatesAndValues.values.get( i ) < indexAndValue.value / 2.0 )
			{
				halfMaxLoc1 = 0.5 * ( coordinatesAndValues.values.get( i + 1 ) + coordinatesAndValues.values.get( i ) );
				break;
			}
		}

		return Math.abs( halfMaxLoc2 - halfMaxLoc1 );
	}


	public static IndexAndValue maximumIndexAndValue( CoordinatesAndValues coordinatesAndValues )
	{
		final int n = coordinatesAndValues.coordinates.size();

		final IndexAndValue indexAndValue = new IndexAndValue();
		indexAndValue.value = - Double.MAX_VALUE;

		for ( int i = 0; i < n; i++ )
		{
			if ( coordinatesAndValues.values.get( i ) > indexAndValue.value )
			{
				indexAndValue.value = coordinatesAndValues.values.get( i );
				indexAndValue.index = i;
			}
		}

		return indexAndValue;
	}

	public static double maxLocCoordinate( CoordinatesAndValues coordinatesAndValues )
	{
		final int n = coordinatesAndValues.coordinates.size();

		final IndexAndValue maxLocIndexAndValue = new IndexAndValue();
		maxLocIndexAndValue.value = - Double.MAX_VALUE;

		for ( int i = 0; i < n; i++ )
		{
			if ( coordinatesAndValues.values.get( i ) > maxLocIndexAndValue.value )
			{
				maxLocIndexAndValue.value = coordinatesAndValues.values.get( i );
				maxLocIndexAndValue.index = i;
			}
		}

		final Double maxLocCoordinate =
				coordinatesAndValues.values.get( maxLocIndexAndValue.index );

		return maxLocCoordinate;
	}


	public static CoordinateAndValue minimum( CoordinatesAndValues coordinatesAndValues )
	{
		return minimum( coordinatesAndValues, null );
	}

	public static CoordinateAndValue minimum(
			CoordinatesAndValues coordinatesAndValues, Double[] coordinateRangeMinMax )
	{
		final int n = coordinatesAndValues.coordinates.size();
		final ArrayList< Double > coordinates = coordinatesAndValues.coordinates;
		final ArrayList< Double > values = coordinatesAndValues.values;

		final IndexAndValue minLocIndexAndValue = new IndexAndValue();
		minLocIndexAndValue.value = Double.MAX_VALUE;

		for ( int i = 0; i < n; i++ )
		{

			if ( coordinateRangeMinMax != null )
			{
				if ( coordinates.get( i ) < coordinateRangeMinMax[ 0 ] ) continue;
				if ( coordinates.get( i ) > coordinateRangeMinMax[ 1 ] ) continue;
			}

			if ( values.get( i ) < minLocIndexAndValue.value )
			{
				minLocIndexAndValue.value = coordinatesAndValues.values.get( i );
				minLocIndexAndValue.index = i;
			}
		}

		final CoordinateAndValue coordinateAndValue = new CoordinateAndValue();
		coordinateAndValue.coordinate =
				coordinatesAndValues.coordinates.get( minLocIndexAndValue.index );
		coordinateAndValue.value = minLocIndexAndValue.value;

		return coordinateAndValue;
	}

	public static CoordinateAndValue maximum( CoordinatesAndValues coordinatesAndValues )
	{
		return maximum( coordinatesAndValues, null );
	}

	public static CoordinateAndValue maximum(
			CoordinatesAndValues coordinatesAndValues,
			Double[] coordinateRangeMinMax )
	{
		final ArrayList< Double > coordinates = coordinatesAndValues.coordinates;
		final ArrayList< Double > values = coordinatesAndValues.values;
		final int n = values.size();

		Double max = - Double.MAX_VALUE;
		Double maxLoc = coordinates.get( 0 );

		for ( int i = 0; i < n; ++i )
		{
			if ( coordinateRangeMinMax != null )
			{
				if ( coordinates.get( i ) < coordinateRangeMinMax[ 0 ] ) continue;
				if ( coordinates.get( i ) > coordinateRangeMinMax[ 1 ] ) continue;
			}

			if ( values.get( i ) > max )
			{
				max = values.get( i );
				maxLoc = coordinates.get( i );
			}
		}

		final CoordinateAndValue coordinateAndValue = new CoordinateAndValue();
		coordinateAndValue.value = max;
		coordinateAndValue.coordinate = maxLoc;

		return coordinateAndValue;
	}

	public static ArrayList< CoordinateAndValue >
	leftMaxAndRightMinLoc( CoordinatesAndValues coordinatesAndValues )
	{
		Double[] rangeMinMax = new Double[ 2 ];

		final ArrayList< CoordinateAndValue > extrema = new ArrayList<>();

		// left
		rangeMinMax[ 0 ] = - Double.MAX_VALUE;
		rangeMinMax[ 1 ] = 0.0;
		extrema.add( maximum( coordinatesAndValues, rangeMinMax ) );

		// right
		rangeMinMax[ 0 ] = 0.0;
		rangeMinMax[ 1 ] = Double.MAX_VALUE;
		extrema.add( minimum( coordinatesAndValues, rangeMinMax ) );

 		return extrema;
	}

	public static Double getValueAtCoordinate(
			CoordinatesAndValues coordinatesAndValues,
			Double coordinate )
	{

		final int coordinateIndex =
				coordinatesAndValues.coordinates.indexOf(
						coordinate );

		return coordinatesAndValues.values.get( coordinateIndex );
	}
}
