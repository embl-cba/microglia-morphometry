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
package de.embl.cba.microglia.morphometry.skeleton;

import de.embl.cba.microglia.morphometry.Algorithms;
import de.embl.cba.microglia.morphometry.regions.Regions;
import ij.IJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

import java.util.ArrayList;

public class SkeletonCreator< T extends RealType< T > & NativeType< T > >
{

	final ArrayList< RandomAccessibleInterval< BitType > > masks;
	private final OpService opService;

	private ArrayList< RandomAccessibleInterval< BitType > > skeletons;
	private int closingRadius = 0;

	public SkeletonCreator( ArrayList< RandomAccessibleInterval< BitType > > masks,
							OpService opService )
	{
		this.masks = masks;
		this.opService = opService;
	}

	public void setClosingRadius( int closingRadius )
	{
		this.closingRadius = closingRadius;
	}

	public void run()
	{

		int tMin = 0;  // at this point the movie is already cropped in time, such that we can process the full movie
		int tMax = masks.size() - 1;

		skeletons = new ArrayList<>( );

		for ( int t = tMin; t <= tMax; ++t )
		{
			IJ.log( "Creating skeletons, frame " + ( t + 1 ) + " / " + ( ( tMax - tMin ) + 1 ) );

			final ImgLabeling< Integer, IntType > imgLabeling =
					Regions.asImgLabeling(
							masks.get( t ),
							ConnectedComponents.StructuringElement.FOUR_CONNECTED );

			final RandomAccessibleInterval< BitType > skeletons =
					Algorithms.createObjectSkeletons(
							imgLabeling,
							closingRadius, // TODO: Make a parameter
							opService );

			this.skeletons.add( skeletons );
		}

	}

	public ArrayList< RandomAccessibleInterval< BitType > > getSkeletons()
	{
		return skeletons;
	}

}
