package test;

import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * Test whether the output is within a certain tolerance
 * of reference measurements.
 *
 * This is to ensure that future code changes due not
 * compromise correct measurements.
 *
 * Measurements:
 *
 * Cell_ID | Spindle_Length_um | Spindle_Width_Avg_um | Spindle_Angle_Degrees
 * NikonSD_100x_HeLa_02 | 13.768170 | 12.184211 | 3.539942
 * NikonSD_60x_HeLa_02 | 13.024016 | 14.684211 | 0.858832
 * ZeissLSM_40x_R1EmESC_01 | 10.764525 | 8.131579 | 1.683764
 * NikonSD_40x_Ptk2_01 | 14.521536 | 8.657895 | 5.887710
 * NikonSD_60x_R1EmESC_01 | 10.550474 | 7.394737 | 5.322811
 * ZeissLSM_40x_CowZygote_01 | 20.202104 | 17.973684 | 10.212573
 * NikonSD_60x_HEK293SiRTubulin_01 | 9.630680 | 9.960526 | 11.556570
 * NikonSD_100x_R1EmESC_01 | 7.141428 | 7.000000 | 43.269730
 *
 *
 * Stainings:
 *
 * Cell_ID	Ch 1	Ch 2
 * NikonSD_100x_HeLa_02	SiR DNA	Tub-GFP
 * NikonSD_60x_HeLa_02	SiR DNA	Tub-GFP
 * ZeissLSM_40x_R1EmESC_01	SiR DNA	Tub-GFP
 * NikonSD_40x_Ptk2_01	Hoechst	Tub-YFP
 * NikonSD_60x_R1EmESC_01	SiR DNA	Tub-GFP
 * ZeissLSM_40x_CowZygote_01	H2B-mScarlet	mClover3-MAP4
 * NikonSD_60x_HEK293SiRTubulin_01	Hoechst	SiR Tubulin
 * NikonSD_100x_R1EmESC_01	SiR DNA	Tub-GFP
 */
public class TestReferenceData
{
	public static final double SPINDLE_LENGTH_TOLERANCE = 0.5; // um
	public static final double SPINDLE_WIDTH_TOLERANCE = 0.3; // um
	public static final double SPINDLE_ANGLE_TOLERANCE = 5; // degrees

	public static void main( String[] args )
	{
		new TestReferenceData().run();
	}

	@Test
	public void run()
	{
		final ImageJ ij = new ImageJ();

		DebugTools.setRootLevel("OFF");

		final HashMap< String, Map< String, double[] > > imageToMeasurements = new HashMap<>();

		addReference( imageToMeasurements, "NikonSD_100x_HeLa_02", 13.768170, 12.184211, 3.539942 );
		addReference( imageToMeasurements, "NikonSD_60x_HeLa_02", 13.024016, 14.684211, 0.858832 );
		addReference( imageToMeasurements, "ZeissLSM_40x_R1EmESC_01", 10.764525, 8.131579, 1.683764 );
		addReference( imageToMeasurements, "NikonSD_40x_Ptk2_01", 14.521536, 8.657895, 5.887710 );
		addReference( imageToMeasurements, "NikonSD_60x_R1EmESC_01", 10.550474, 7.394737, 5.322811 );
		addReference( imageToMeasurements, "ZeissLSM_40x_CowZygote_01", 20.202104, 17.973684, 10.212573 );
		addReference( imageToMeasurements, "NikonSD_60x_HEK293SiRTubulin_01", 9.630680, 9.960526, 11.556570 );
		addReference( imageToMeasurements, "NikonSD_100x_R1EmESC_01", 7.141428, 7.000000, 43.269730 );


		final Spindle3DFileProcessorCommand command = new Spindle3DFileProcessorCommand();
		command.opService = ij.op();
		command.scriptService = ij.script();
		command.outputDirectory = new File( "src/test/resources/test/output" );
		command.dnaChannelIndexOneBased = 1;
		command.spindleChannelIndexOneBased = 2;
		command.showIntermediateImages = false;
		command.showIntermediatePlots = false;
		command.saveResults = false;

		for ( String image : imageToMeasurements.keySet() )
		{
			command.inputImageFile = new File("src/test/resources/test/references/"+image+".tif" );
			command.run();

			final Map< String, Object > measured = command.getObjectMeasurements().get( 0 );
			final Map< String, double[] > reference = imageToMeasurements.get( image );

			for ( String measurement : reference.keySet() )
			{
				testEquality( measured, reference, measurement );
			}
		}
	}

	private void testEquality( Map< String, Object > measured, Map< String, double[] > reference, String measurement )
	{
		assertEquals( reference.get( measurement )[ 0 ], ( double ) measured.get( measurement ), reference.get( measurement )[ 1 ] );
	}

	private void addReference( HashMap< String, Map< String, double[] > > datasetToMeasurements, String dataset, double length, double width, double angle )
	{
		final HashMap< String, double[] > measurements = new HashMap<>();
		measurements.put( Spindle3DMeasurements.SPINDLE_LENGTH, new double[]{ length, SPINDLE_LENGTH_TOLERANCE } );
		measurements.put( Spindle3DMeasurements.SPINDLE_WIDTH_AVG, new double[]{ width, SPINDLE_WIDTH_TOLERANCE } );
		measurements.put( Spindle3DMeasurements.SPINDLE_ANGLE_DEGREES, new double[]{ angle, SPINDLE_ANGLE_TOLERANCE } );
		datasetToMeasurements.put( dataset, measurements );
	}
}
