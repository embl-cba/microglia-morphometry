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

import ij.IJ;

import java.io.File;

public class Logger
{
	public static boolean showDebugInformation = false;

	public static void debug( String s )
	{
		if ( showDebugInformation )
			IJ.log( s );
	}

	public static void log( String message )
	{
		IJ.log( message );
		System.out.println( message );

		if ( Utils.logFilePath != null )
		{
			File logFile = new File( Utils.logFilePath );

			if ( ! logFile.exists() )
			{
				Utils.createLogFile();
			}
			else
			{
				Utils.writeToLogFile( message + "\n" );
			}
		}

	}

	public static void error( String s )
	{
		IJ.showMessage( s );
		System.err.println( s );
	}

	public static void progress( final long total, final int count, final long startTimeMillis, String msg )
	{
		double secondsSpent = (1.0 * System.currentTimeMillis() - startTimeMillis ) / (1000.0);
		double secondsPerTask = secondsSpent / count;
		double secondsLeft = (total - count) * secondsPerTask;

		String unit = "s";
		double divisor = 1;

		if ( secondsSpent > 3 * 60 )
		{
			unit = "min";
			divisor = 60;
		}

		IJ.progress( msg,
				"" + count + "/" + total
						+ "; time ( spent, left, task ) [ " + unit + " ]: "
						+ ( int ) ( secondsSpent / divisor )
						+ ", " + ( int ) ( secondsLeft / divisor )
						+ ", " + String.format("%.3g", secondsPerTask / divisor)
						+ "; memory: "
						+ IJ.freeMemory() );
	}

	public static void progress( String msg, String progress )
	{
		progress = msg + ": " + progress;

		if ( IJ.getLog() != null )
		{
			String[] logs = IJ.getLog().split( "\n" );
			if ( logs.length > 1 )
			{
				if ( logs[ logs.length - 1 ].contains( msg ) )
				{
					progress = "\\Update:" + progress;
				}
			}
			IJ.log( progress );
		}
		else
		{
			System.out.println( progress );
		}
	}

}
