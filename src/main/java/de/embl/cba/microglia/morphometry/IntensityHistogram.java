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
package de.embl.cba.microglia.morphometry;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class IntensityHistogram <T extends RealType<T> & NativeType< T > >
{
	public double[] binCenters;
	public double[] frequencies;
	final public double binWidth;
	final public int numBins;
	final RandomAccessibleInterval< T > rai;

	public IntensityHistogram( RandomAccessibleInterval< T > rai, double maxValue, double binWidth )
	{
		this.binWidth = binWidth;
		this.numBins = ( int ) ( maxValue / binWidth );
		this.rai = rai;

		initializeHistogram( numBins, binWidth );
		computeFrequencies();
	}

	public void initializeHistogram( int numBins, double binWidth )
	{
		this.binCenters = new double[ numBins ];
		this.frequencies = new double[ numBins ];

		for ( int i = 0; i < numBins; ++i )
		{
			binCenters[ i ] = i * binWidth + binWidth * 0.5;
		}
	}



	public CoordinateAndValue getMode( )
	{
		final CoordinateAndValue coordinateAndValue = new CoordinateAndValue( 0.0, 0.0);

		for ( int i = 0; i < numBins - 1; ++i ) // numBins - 1 avoids the last bin containing saturated pixels
		{
			if ( frequencies[ i ] > coordinateAndValue.value )
			{
				coordinateAndValue.value = frequencies[ i ];
				coordinateAndValue.coordinate = binCenters[ i ];
			}
		}

		return coordinateAndValue;
	}

	public CoordinateAndValue getRightHandHalfMode( )
	{
		final CoordinateAndValue mode = getMode();

		final CoordinateAndValue coordinateAndValue = new CoordinateAndValue();

		for ( int i = 0; i < numBins; ++i )
		{
			if ( binCenters[ i ] > mode.coordinate )
			{
				if ( frequencies[ i ] <= mode.value / 2.0 )
				{
					coordinateAndValue.coordinate = binCenters[ i ];
					coordinateAndValue.value = frequencies[ i ];
					return coordinateAndValue;
				}
			}
		}
		return coordinateAndValue;
	}


	private void computeFrequencies()
	{
		final Cursor< T > cursor = Views.iterable( rai ).cursor();

		while( cursor.hasNext() )
		{
			increment( cursor.next().getRealDouble() );
		}
	}

	public void increment( double value )
	{
		int bin = (int) ( value / binWidth );

		if ( bin >= numBins )
		{
			bin = numBins - 1;
		}

		frequencies[ bin ]++;
	}

}
