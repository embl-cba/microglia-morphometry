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
package de.embl.cba.microglia;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.util.List;

public class BdvPointListOverlay extends BdvOverlay
{
	final List< RealPoint > points;
	private final double depthOfField;

	public BdvPointListOverlay( List< RealPoint > points, double depthOfField )
	{
		super();
		this.points = points;
		this.depthOfField = depthOfField;
	}

	@Override
	protected void draw( final Graphics2D g )
	{
		final AffineTransform3D t = new AffineTransform3D();
		getCurrentTransform3D( t );

		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		for ( final RealPoint p : points )
		{
			p.localize( gPos );
			t.apply( gPos, lPos );
			final int size = getSize( lPos[ 2 ] );
			final int x = ( int ) ( lPos[ 0 ] - 0.5 * size );
			final int y = ( int ) ( lPos[ 1 ] - 0.5 * size );
			g.setColor( getColor( lPos[ 2 ] ) );
			g.fillOval( x, y, size, size );
		}
	}

	private Color getColor( final double depth )
	{
		int alpha = 255 - ( int ) Math.round( Math.abs( depth ) );

		if ( alpha < 64 )
			alpha = 64;

		return new Color( 255, 0, 0, alpha );
	}

	private int getSize( final double depth )
	{
		return ( int ) Math.max( 5, 20 - 1.0 / depthOfField * Math.round( Math.abs( depth ) ) );
	}

}
