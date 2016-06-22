/**
 * @file QuimPArrayUtils.java
 * @date 22 Jun 2016
 */
package uk.ac.warwick.wsbc.QuimP.utils;

/**
 * Deliver simple methods operating on arrays
 * @author p.baniukiewicz
 * @date 22 Jun 2016
 *
 */
public class QuimPArrayUtils {

    /**
     * Convert 2D float array to double
     * 
     * @param input Array to convert
     * @return converted one
     */
    public static double[][] float2Ddouble(float[][] input) {
        if (input == null)
            return null;
        int rows = input.length;
        double[][] out = new double[rows][];
        // iterate over rows with conversion
        for (int r = 0; r < rows; r++) {
            float[] row = input[r];
            int cols = row.length;
            out[r] = new double[cols];
            // iterate over columns
            for (int c = 0; c < cols; c++) {
                out[r][c] = input[r][c];
            }
        }
        return out;
    }
}
