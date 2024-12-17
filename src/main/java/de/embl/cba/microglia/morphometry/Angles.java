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

import de.embl.cba.microglia.Transforms;
import net.imglib2.RealPoint;

import static de.embl.cba.microglia.morphometry.Constants.X;
import static de.embl.cba.microglia.morphometry.Constants.Y;
import static java.lang.Math.atan;
import static java.lang.Math.toDegrees;

public abstract class Angles
{
	public static double angle2DToCoordinateSystemsAxisInDegrees( RealPoint point )
	{
		final double[] vector = Vectors.asDoubles( point );

		return angle2DToCoordinateSystemsAxisInDegrees( vector );
	}

	public static double angle2DToCoordinateSystemsAxisInDegrees( double[] vector )
	{

		double angleToZAxisInDegrees;

		if ( vector[ Y ] == 0 )
		{
			angleToZAxisInDegrees = Math.signum( vector[ X ] ) * 90;
		}
		else
		{
			angleToZAxisInDegrees = toDegrees( atan( vector[ X ] / vector[ Y ] ) );

			if ( vector[ Y ] < 0 )
			{
				angleToZAxisInDegrees += 180;
			}
		}

		return angleToZAxisInDegrees;
	}


}
