package test;

import de.embl.cba.microglia.command.MicrogliaMorphometryCommand;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestMorphometryMeasurements
{
	@Test
	public void run( ) throws IOException
	{
		DebugTools.setRootLevel( "off" );
		final ImageJ ij = new ImageJ();

		final MicrogliaMorphometryCommand command = new MicrogliaMorphometryCommand();
		command.opService = ij.op();
		command.intensityFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3.tif");
		command.labelMaskFile = new File("src/test/resources/data/MAX_pg6-3CF1_20--t1-3-labelMasks.tif");
		command.outputDirectory = new File( "src/test/resources/test-data/" );
		command.run();

		assert compareCSV(
				new File( "src/test/resources/data/MAX_pg6-3CF1_20--t1-3.csv" ).toString(),
				new File( "src/test/resources/test-data/MAX_pg6-3CF1_20--t1-3.csv" ).toString()
				);

		System.out.println("Test passed.");
	}

	public static boolean compareCSV(String pathRef, String pathTest) throws IOException {
		List<String[]> refData = readCSV(pathRef);
		List<String[]> testData = readCSV(pathTest);

		if (refData.isEmpty() || testData.isEmpty()) {
			System.out.println("One or both of the files are empty.");
			return false;
		}

		// Assuming first row contains headers
		String[] headers = refData.get(0);

		if (headers.length != testData.get(0).length) {
			System.out.println("The number of columns differs in the header.");
			return false;
		}

		if (refData.size() != testData.size()) {
			System.out.println("The number of rows differs.");
			return false;
		}

		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		boolean identical = true;

		// Start from index 1 to skip headers
		for (int i = 1; i < refData.size(); i++) {
			String[] refRow = refData.get(i);
			String[] testRow = testData.get(i);

			if (refRow.length != testRow.length) {
				System.out.println("Row length differs at row " + (i + 1));
				identical = false;
				continue;
			}

			for (int j = 0; j < refRow.length; j++) {
				String refValue = refRow[j];
				String testValue = testRow[j];

				try {
					double refNumber = Double.parseDouble(refValue);
					double testNumber = Double.parseDouble(testValue);

					refValue = decimalFormat.format(refNumber);
					testValue = decimalFormat.format(testNumber);

					if (!refValue.equals(testValue)) {
							System.out.println( "Difference found at row " + ( i + 1 ) + ", column '" + headers[ j ] + "'" );
							identical = false;
					}
				} catch (NumberFormatException e) {
					// Non-numeric values
				}
			}
		}

		return identical;
	}

	private static List<String[]> readCSV( String path) throws IOException
	{
		List<String[]> data = new ArrayList<>();
		try ( BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\t");
				data.add(values);
			}
		}
		return data;
	}
}
