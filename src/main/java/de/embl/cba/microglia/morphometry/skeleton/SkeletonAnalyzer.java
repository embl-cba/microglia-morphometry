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

import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.ArrayList;

public class SkeletonAnalyzer< R extends RealType< R > >
{

	final RandomAccessibleInterval< BitType > skeleton;
	final OpService opService;

	double totalSkeletonLength;
	long numBranchPoints;
	private RandomAccessibleInterval<BitType> branchpoints;
	private long longestBranchLength;
	private ArrayList< Long > branchLengths;

	public SkeletonAnalyzer(
			RandomAccessibleInterval< BitType > skeleton,
			OpService opService )
	{
		this.skeleton = skeleton;
		this.opService = opService;

		run();
	}


	private void run()
	{
		totalSkeletonLength = measureSum( skeleton );

		branchpoints = detectBranchpoints();

		branchLengths = Skeletons.branchLengths( skeleton );
	}

	private RandomAccessibleInterval< BitType > detectBranchpoints()
	{
		RandomAccessibleInterval< BitType > branchpoints = Skeletons.branchPoints( skeleton );

		numBranchPoints = measureSum( branchpoints );

		return branchpoints;
	}


	public long getNumBranches()
	{
		return branchLengths.size();
	}


	public double getAverageBranchLength()
	{

		if ( branchLengths.size() == 0 ) return 0;

		double avg = 0;

		for ( long length : branchLengths )
			avg += length;

		avg /= branchLengths.size();

		return avg;
	}


	public long getLongestBranchLength()
	{
		if ( branchLengths.size() == 0 ) return 0;

		return branchLengths.get( 0 );
	}

	public long getNumBranchPoints()
	{
		return numBranchPoints;
	}

	public RandomAccessibleInterval< BitType > getBranchpoints()
	{
		return branchpoints;
	}

	public double getTotalSkeletonLength() { return totalSkeletonLength; }

	public static long measureSum( RandomAccessibleInterval< BitType > rai )
	{
		final Cursor< BitType > cursor = Views.iterable( rai ).cursor();

		long sum = 0;

		while ( cursor.hasNext() )
			sum += cursor.next().getRealDouble();

		return sum;
	}



}
