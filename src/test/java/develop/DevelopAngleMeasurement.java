package develop;

import de.embl.cba.transforms.utils.Transforms;

public class DevelopAngleMeasurement
{
	public static void main( String[] args )
	{
		final double[] vector = { 5.8, 8.4, 3.8 };
		final double angle = 90.0 - Math.abs( 180.0 / Math.PI *
				Transforms.getAngle( new double[]{ 0, 0, 1 }, vector ) );
	}
}
