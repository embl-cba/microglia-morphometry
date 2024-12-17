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

import de.embl.cba.microglia.morphometry.geometry.CoordinateToValue;
import de.embl.cba.microglia.morphometry.geometry.CoordinatesAndValues;
import ij.gui.Plot;

import java.util.ArrayList;

public class Plots
{
	public static void plot( double[] xValues , double[] yValues )
	{
		Plot plot = new Plot("title","x", "y",  xValues, yValues );
		plot.show();
	}


	public static void plot( ArrayList< Double > xValues , ArrayList< Double >  yValues, String xLab, String yLab )
	{
		Plot plot = new Plot("", xLab, yLab,  xValues.stream().mapToDouble(d -> d).toArray(), yValues.stream().mapToDouble(d -> d).toArray() );
		plot.show();
	}

	public static void plot( CoordinatesAndValues cv, String xLab, String yLab )
	{
		Plot plot = new Plot("",
				xLab,
				yLab,
				cv.coordinates.stream().mapToDouble(d -> d).toArray(),
				cv.values.stream().mapToDouble( d -> d ).toArray() );

		plot.show();
	}

	public static void plot( CoordinateToValue cv, String xLab, String yLab )
	{
		Plot plot = new Plot("",
				xLab,
				yLab,
				cv.keySet().stream().mapToDouble( d -> d ).toArray(),
				cv.values().stream().mapToDouble( d -> d ).toArray() );

		plot.show();
	}

}

