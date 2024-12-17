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

import de.embl.cba.transforms.utils.Transforms;
import ij.ImagePlus;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageByte;
import net.imglib2.realtransform.AffineTransform3D;

public abstract class Ellipsoids3DImageSuite
{
	public static EllipsoidVectors fitEllipsoid( ImagePlus mask )
	{
		final ImageByte imageByte = new ImageByte( mask );
		Objects3DPopulation objects3DPopulation = new Objects3DPopulation( imageByte, 0 );
		final Object3D object = objects3DPopulation.getObject( 0 );

		final EllipsoidVectors ellipsoidVectors = new EllipsoidVectors();
		ellipsoidVectors.center = object.getCenterAsArray();
		ellipsoidVectors.shortestAxis = object.getVectorAxis( 0 );
		ellipsoidVectors.middleAxis = object.getVectorAxis( 1 );
		ellipsoidVectors.longestAxis = object.getVectorAxis( 2 );
		ellipsoidVectors.shortestAxisLength = 2 * object.getRadiusMoments( 0 );
		ellipsoidVectors.middleAxisLength = 2 * object.getRadiusMoments( 1 );
		ellipsoidVectors.longestAxisLength = 2 * object.getRadiusMoments( 2 );

		return ellipsoidVectors;
	}

	public static AffineTransform3D createShortestAxisAlignmentTransform( EllipsoidVectors ellipsoidVectors )
	{
		AffineTransform3D translation = new AffineTransform3D();
		translation.translate( ellipsoidVectors.center  );
		translation = translation.inverse();

		final double[] zAxis = new double[]{ 0, 0, 1 };
		final double[] shortestAxis = ellipsoidVectors.shortestAxis.getArray();
		AffineTransform3D rotation = Transforms.getRotationTransform3D( zAxis, shortestAxis );

		AffineTransform3D combinedTransform = translation.preConcatenate( rotation );

		return combinedTransform;
	}

	public static AffineTransform3D createAlignmentTransform( EllipsoidVectors ellipsoidVectors )
	{
		AffineTransform3D transform3D = new AffineTransform3D();
		transform3D.translate( ellipsoidVectors.center  );
		transform3D = transform3D.inverse();

		final double[] xAxis = new double[]{ 1, 0, 0 };
		final double[] longestAxis = ellipsoidVectors.longestAxis.getArray();
		AffineTransform3D longAxisRotation = Transforms.getRotationTransform3D( xAxis, longestAxis );
		transform3D = transform3D.preConcatenate( longAxisRotation );

		final double[] zAxis = new double[]{ 0, 0, 1 };
		final double[] shortestAxis = ellipsoidVectors.shortestAxis.getArray();
		final double[] shortestAxisInLongestAxisAlignedCoordinateSystem = new double[ 3 ];
		longAxisRotation.apply( shortestAxis, shortestAxisInLongestAxisAlignedCoordinateSystem );

		AffineTransform3D shortAxisRotation = Transforms.getRotationTransform3D( zAxis, shortestAxisInLongestAxisAlignedCoordinateSystem );
		transform3D = transform3D.preConcatenate( shortAxisRotation );

		return transform3D;
	}
}
