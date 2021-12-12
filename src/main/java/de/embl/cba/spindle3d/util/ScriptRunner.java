package de.embl.cba.spindle3d.util;

import ij.ImagePlus;
import ij.macro.Interpreter;
import org.scijava.script.ScriptService;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class ScriptRunner implements Runnable
{
	private final ImagePlus imp;
	private final File file;
	private final ScriptService scriptService; // could be removed!
	private ImagePlus outputImp;

	public ScriptRunner( ImagePlus imp, File file, ScriptService scriptService )
	{
		this.imp = imp;
		this.file = file;
		this.scriptService = scriptService;
	}

	@Override
	public void run()
	{
		//imp.show();

		final String macro = readFile( file.toString() );
		outputImp = new Interpreter().runBatchMacro( macro, imp );

		//outputImp.show();
	}

	// SciJava based solution, did not get it to work without staging the image by imp.show().
	private void tryRun()
	{
		try
		{
			scriptService.run( file, true ).get();
		} catch ( InterruptedException e )
		{
			e.printStackTrace();
		} catch ( ExecutionException e )
		{
			e.printStackTrace();
		} catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		} catch ( ScriptException e )
		{
			e.printStackTrace();
		}
	}

	private static String readFile( String filePath )
	{
		StringBuilder contentBuilder = new StringBuilder();

		try ( Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
		{
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		}
		catch ( IOException e)
		{
			e.printStackTrace();
		}

		return contentBuilder.toString();
	}

	public ImagePlus getOutputImp()
	{
		return outputImp;
	}
}
