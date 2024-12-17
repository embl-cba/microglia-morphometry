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

import de.embl.cba.microglia.Utils;
import de.embl.cba.microglia.morphometry.regions.RegionAndSize;
import de.embl.cba.microglia.morphometry.regions.Regions;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.morphology.table2d.Branchpoints;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.ArrayList;

public class Skeletons
{
	public static RandomAccessibleInterval< BitType > longestBranch(
			RandomAccessibleInterval< BitType > skeleton )
	{

		final RandomAccessibleInterval< BitType > longestBranch =
				Utils.copyAsArrayImg( skeleton );

		removeBranchpoints( longestBranch );

		Regions.onlyKeepLargestRegion( longestBranch,
				ConnectedComponents.StructuringElement.EIGHT_CONNECTED );

		return longestBranch;
	}

	public static ArrayList< Long > branchLengths(
			RandomAccessibleInterval< BitType > skeleton )
	{
		final RandomAccessibleInterval< BitType > branches =
				Utils.copyAsArrayImg( skeleton );

		removeBranchpoints( branches );

		final ArrayList< RegionAndSize > regions = Regions.getSizeSortedRegions( branches,
				ConnectedComponents.StructuringElement.EIGHT_CONNECTED );

		final ArrayList< Long > branchLengthPixels = new ArrayList<>();

		for ( RegionAndSize regionAndSize : regions )
			branchLengthPixels.add( regionAndSize.getSize() );

		return branchLengthPixels;
	}

	public static RandomAccessibleInterval< BitType > branchPoints(
			RandomAccessibleInterval< BitType > skeleton )
	{
		RandomAccessibleInterval< BitType > branchPoints =
				ArrayImgs.bits( Intervals.dimensionsAsLongArray( skeleton ) );

		Branchpoints.branchpoints(
				Views.extendBorder( Views.zeroMin( skeleton ) ),
				Views.flatIterable( branchPoints ) );

		Views.translate( branchPoints, Intervals.minAsLongArray( skeleton ) );

		return branchPoints;
	}

	static void removeBranchpoints( RandomAccessibleInterval< BitType > skeleton )
	{
		final RandomAccessibleInterval< BitType > branchpoints = branchPoints( skeleton );

		LoopBuilder.setImages( branchpoints, skeleton ).forEachPixel( ( b, s ) ->
		{
			if ( b.get() ) s.set( false );
		});
	}


	public static RandomAccessibleInterval< BitType > skeleton(
			RandomAccessibleInterval< BitType > input,
			OpService opService )
	{
		final RandomAccessibleInterval< BitType > thin = Utils.createEmptyCopy( input );
		opService.morphology().thinGuoHall( thin, input );
		return thin;
	}
}
