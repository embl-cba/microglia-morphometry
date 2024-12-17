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
package de.embl.cba.microglia.morphometry.geometry.ellipsoids;

public class EllipsoidMLJ
{
	public static final int PHI = 0, THETA = 1, PSI = 2;

	public double[] center = new double[ 3 ];
	public double[] radii = new double[ 3 ];
	public double[] eulerAnglesInDegrees = new double[ 3 ];

	@Override
	public String toString()
	{
		String s = "";
		s += "\n## MorpholibJ ellipsoid parameters:";
		s += "\ncenter_X [pixels]: " + center[0];
		s += "\ncenter_Y [pixels]: " + center[1];
		s += "\ncenter_Z [pixels]: " + center[2];
		s += "\nradii_0 [pixels]: " + radii[0];
		s += "\nradii_1 [pixels]: " + radii[1];
		s += "\nradii_2 [pixels]: " + radii[2];
		s += "\nphi [degrees]: " + eulerAnglesInDegrees[ PHI ];
		s += "\ntheta [degrees]: " + eulerAnglesInDegrees[ THETA ];
		s += "\npsi [degrees]: " + eulerAnglesInDegrees[ PSI ];

		return s;
	}
}
