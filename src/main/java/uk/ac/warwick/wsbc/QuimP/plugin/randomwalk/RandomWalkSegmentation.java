/**
 * @file RandomWalkSegmentation.java
 * @date 22 Jun 2016
 */
package uk.ac.warwick.wsbc.QuimP.plugin.randomwalk;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.apache.commons.math3.stat.StatUtils;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import uk.ac.warwick.wsbc.QuimP.utils.QuimPArrayUtils;

/**
 * Perform element-wise multiplication by value (.*val in Matlab)
 * 
 * @author p.baniukiewicz
 * @date 22 Jun 2016
 * @remarks Done in-place
 */
class MatrixElementMultiply implements RealMatrixChangingVisitor {

    private double d; // multiplier

    /**
     * Assign multiplier
     * 
     * @param d multiplier
     */
    MatrixElementMultiply(double d) {
        this.d = d;
    }

    @Override
    public double end() {
        return 0;
    }

    @Override
    public void start(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
    }

    /**
     * Multiply entry by value
     */
    @Override
    public double visit(int arg0, int arg1, double arg2) {
        return arg2 * d;
    }

}

/**
 * Perform element-wise exp
 * 
 * @author p.baniukiewicz
 * @date 23 Jun 2016
 * @remarks Done in-place
 */
class MatrixElementExp implements RealMatrixChangingVisitor {

    @Override
    public double end() {
        return 0;
    }

    @Override
    public void start(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {

    }

    @Override
    public double visit(int arg0, int arg1, double arg2) {
        return Math.exp(arg2);
    }

}

/**
 * Perform element-wise power (.^2 in Matlab)
 * 
 * @author p.baniukiewicz
 * @date 22 Jun 2016
 * @remarks Done in-place
 */
class MatrixElementPower implements RealMatrixChangingVisitor {

    @Override
    public double end() {
        return 0;
    }

    @Override
    public void start(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
    }

    /**
     * Multiply entry by itself
     */
    @Override
    public double visit(int arg0, int arg1, double arg2) {
        return arg2 * arg2;
    }
}

/**
 * Multiply in-place this matrix by another
 * @author p.baniukiewicz
 * @date 23 Jun 2016
 *
 */
class MatrixDotProduct implements RealMatrixChangingVisitor {

    RealMatrix m;

    public MatrixDotProduct(RealMatrix m) {
        this.m = m;
    }

    @Override
    public double end() {
        return 0;
    }

    @Override
    public void start(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
        if (m.getColumnDimension() != arg1 || m.getRowDimension() != arg0)
            throw new MatrixDimensionMismatchException(m.getRowDimension(), m.getColumnDimension(),
                    arg0, arg1);

    }

    @Override
    public double visit(int arg0, int arg1, double arg2) {

        return arg2 * m.getEntry(arg0, arg1);
    }

}

/**
 * Hold algorithm parameters
 * 
 * @author p.baniukiewicz
 * @date 22 Jun 2016
 *
 */
class Params {
    double alpha;
    double beta;
    double[] gamma;
    int Iter;
    double dt;
    double relim;

    /**
     * Set default values
     */
    public Params() {
        this.gamma = new double[2];
        alpha = 4e2;
        beta = 2 * 25;
        gamma[0] = 100;
        gamma[1] = 300;
        Iter = 10000;
        dt = 0.1;
        relim = 8e-3;
    }

    /**
     * Set user values
     * 
     * @param alpha
     * @param beta
     * @param gamma
     * @param iter
     * @param dt
     * @param relim
     */
    public Params(double alpha, double beta, double gamma1, double gamma2, int iter, double dt,
            double relim) {
        this();
        this.alpha = alpha;
        this.beta = beta;
        this.gamma[0] = gamma1;
        this.gamma[1] = gamma2;
        Iter = iter;
        this.dt = dt;
        this.relim = relim;
    }
}

/**
 * @author p.baniukiewicz
 * @date 22 Jun 2016
 *
 */
public class RandomWalkSegmentation {

    public static final int RIGHT = -10; //!< Direction of circshift coded as in Matlab */
    public static final int LEFT = 10; //!< Direction of circshift coded as in Matlab */
    public static final int TOP = -01; //!< Direction of circshift coded as in Matlab */
    public static final int BOTTOM = 01; //!< Direction of circshift coded as in Matlab */
    public static final int FOREGROUND = 1; //!< Definition of foreground pixels */
    public static final int BACKGROUND = 2; //!< Definition of background pixels */

    private RealMatrix image; //!< Image to process in 8bit greyscale
    private Params params;

    public RandomWalkSegmentation() {
        params = new Params();
    }

    public RandomWalkSegmentation(RealMatrix image, Params params) {
        this.image = image;
        this.params = params;
    }
    
    /**
     * Find maximum in 2D RealMatrix
     * 
     * @param input Matrix to process
     * @return maximal value in \a input
     */
    static public double getMax(RealMatrix input) {
        double[][] data = ((Array2DRowRealMatrix) input).getDataRef();
        double[] maxs = new double[input.getRowDimension()];
        for (int r = 0; r < input.getRowDimension(); r++)
            maxs[r] = StatUtils.max(data[r]);
        return StatUtils.max(maxs);
    }

    /**
     * Find minimum in 2D RealMatrix
     * 
     * @param input Matrix to process
     * @return minimal value in \a input
     */
    static public double getMin(RealMatrix input) {
        double[][] data = ((Array2DRowRealMatrix) input).getDataRef();
        double[] maxs = new double[input.getRowDimension()];
        for (int r = 0; r < input.getRowDimension(); r++)
            maxs[r] = StatUtils.min(data[r]);
        return StatUtils.min(maxs);
    }

    /**
     * Create RealMatrix 2D from image. Image is converted to Double
     * 
     * @param ip input image
     * @return 2D matrix converted to Double
     */
    static public RealMatrix ImagePlus2RealMatrix(ImagePlus ip) {
        if (ip == null)
            return null;
        RealMatrix out;
        float[][] image = ip.getProcessor().getFloatArray();
        out = MatrixUtils.createRealMatrix(QuimPArrayUtils.float2Ddouble(image));
        return out;
    }

    /**
     * Find \b BACKGROUND and \b FOREGROUND labeled pixels on seed image and return their positions
     * 
     * @param rgb RGB seed image 
     * @param fseed color of marker for foreground pixels
     * @param bseed color of marker for background pixels
     * @return Map containing list of coordinates that belong to foreground and background. Map is
     * addressed by two enums: \a FOREGROUND and \a BACKGROUND
     * @throws RandomWalkException When image other that RGB provided
     */
    protected Map<Integer, List<Point>> decodeSeeds(final ImagePlus rgb, Color fseed, Color bseed)
            throws RandomWalkException {
        // output map integrating two lists of points
        HashMap<Integer, List<Point>> out = new HashMap<Integer, List<Point>>();
        // output lists of points. Can be null if points not found
        List<Point> foreground = new ArrayList<>();
        List<Point> background = new ArrayList<>();
        // verify input condition
        if (rgb.getType() != ImagePlus.COLOR_RGB)
            throw new RandomWalkException("Unsupported image type");
        // find marked pixels
        ColorProcessor cp = (ColorProcessor) rgb.getProcessor(); // can cast here because of type
                                                                 // checking
        for (int x = 0; x < rgb.getWidth(); x++)
            for (int y = 0; y < rgb.getHeight(); y++) {
                Color c = cp.getColor(x, y); // get color for pixel
                if (c.equals(fseed))
                    foreground.add(new Point(x, y)); // remember foreground coords
                else if (c.equals(bseed))
                    background.add(new Point(x, y)); // remember background coords
            }

        // pack outputs into map
        out.put(FOREGROUND, foreground);
        out.put(BACKGROUND, background);
        return out;
    }

    /**
     * Compute image circularly shifted by one pixel towards selected direction
     * 
     * @param input Image to be shifted
     * @param direction Shift direction
     * @return Copy of \a input shifted by one pixel in \a direction. 
     */
    protected RealMatrix circshift(RealMatrix input, int direction) {
        double[][] sub; // part of matrix that does no change put is shifted
        int rows = input.getRowDimension(); // cache sizes
        int cols = input.getColumnDimension();
        RealMatrix out = MatrixUtils.createRealMatrix(rows, cols); // output matrix, shifted
        switch (direction) {
            case RIGHT:
                // rotated right - last column become first
                // cut submatrix from first column to before last
                sub = new double[rows][cols - 1];
                input.copySubMatrix(0, rows - 1, 0, cols - 2, sub); // cols-2 because last is not
                // create new matrix - paste submatrix but shifted right
                out.setSubMatrix(sub, 0, 1);
                // copy last column to first
                out.setColumnVector(0, input.getColumnVector(cols - 1));
                break;
            case LEFT:
                // rotated left - first column become last
                // cut submatrix from second column to last
                sub = new double[rows][cols - 1];
                input.copySubMatrix(0, rows - 1, 1, cols - 1, sub);
                // create new matrix - paste submatrix but shifted right
                out.setSubMatrix(sub, 0, 0);
                // copy first column to last
                out.setColumnVector(cols - 1, input.getColumnVector(0));
                break;
            case TOP:
                // rotated top - first row become last
                // cut submatrix from second row to last
                sub = new double[rows - 1][cols];
                input.copySubMatrix(1, rows - 1, 0, cols - 1, sub);
                // create new matrix - paste submatrix but shifted up
                out.setSubMatrix(sub, 0, 0);
                // copy first row to last
                out.setRowVector(rows - 1, input.getRowVector(0));
                break;
            case BOTTOM:
                // rotated bottom - last row become first
                // cut submatrix from first row to before last
                sub = new double[rows - 1][cols];
                input.copySubMatrix(0, rows - 2, 0, cols - 1, sub);
                // create new matrix - paste submatrix but shifted up
                out.setSubMatrix(sub, 1, 0);
                // copy last row to first
                out.setRowVector(0, input.getRowVector(rows - 1));
                break;
            default:
                throw new IllegalArgumentException("circshift: Unknown direction");
        }
        return out;
    }

    /**
     * 
     * @param a 
     * @param b
     * @return Image (a-b).^2
     */
    protected RealMatrix getSqrdDiffIntensity(RealMatrix a, RealMatrix b) {
        RealMatrix s = a.subtract(b);
        s.walkInOptimizedOrder(new MatrixElementPower());
        return s;
    }

    /**
     * Pre-compute gradient matrices 
     * @return Array of precomputed data in the following order:
     * -# [0] - gRight2
     * -# [1] - gTop2
     * -# [2] - gLeft2
     * -# [3] - gBottom2
     */
    private RealMatrix[] precompute() {
        // setup shifted images
        RealMatrix right = circshift(image, RIGHT);
        RealMatrix top = circshift(image, TOP);
        // compute squared intensity differences
        RealMatrix gRight2 = getSqrdDiffIntensity(image, right);
        RealMatrix gTop2 = getSqrdDiffIntensity(image, top);
        // compute maximum of horizontal and vertical intensity gradients
        double maxGright2 = RandomWalkSegmentation.getMax(gRight2);
        double maxGtop2 = RandomWalkSegmentation.getMax(gTop2);
        double maxGrad2 = maxGright2 > maxGtop2 ? maxGright2 : maxGtop2;
        // Normalize squared gradients to maxGrad
        gRight2.walkInOptimizedOrder(new MatrixElementMultiply(1 / maxGrad2));
        gTop2.walkInOptimizedOrder(new MatrixElementMultiply(1 / maxGrad2));
        RealMatrix gLeft2 = circshift(gRight2, LEFT);
        RealMatrix gBottom2 = circshift(gTop2, BOTTOM);
        // assign outputs
        RealMatrix[] out = new RealMatrix[4];
        out[0] = gRight2;
        out[1] = gTop2;
        out[2] = gLeft2;
        out[3] = gBottom2;
        return out;
    }

    protected void solver(RealMatrix image, Map<Integer, List<Point>> seeds, RealMatrix[] gradients,
            Params params) {
        // compute mean intensity of foreground and background pixels
        double meanseed_fg = StatUtils.mean(getValues(image, seeds.get(FOREGROUND)).getDataRef());
        double meanseed_bg = StatUtils.mean(getValues(image, seeds.get(BACKGROUND)).getDataRef());
        // compute normalised squared differences to mean seed intensities
        RealMatrix diffI_fg = image.scalarAdd(-meanseed_fg);
        diffI_fg.walkInOptimizedOrder(new MatrixElementPower());
        diffI_fg.walkInOptimizedOrder(new MatrixElementMultiply(1 / 65025));
        RealMatrix diffI_bg = image.scalarAdd(-meanseed_bg);
        diffI_bg.walkInOptimizedOrder(new MatrixElementPower());
        diffI_bg.walkInOptimizedOrder(new MatrixElementMultiply(1 / 65025));
        // compute weights for diffusion in all four directions, dependent on local gradients and
        // differences to mean intensities of seeds
        RealMatrix wr_fg = computeweights(diffI_fg, gradients[0]); // TODO optimize alpha*diffI_fg
        RealMatrix wl_fg = computeweights(diffI_fg, gradients[2]);
        RealMatrix wt_fg = computeweights(diffI_fg, gradients[1]);
        RealMatrix wb_fg = computeweights(diffI_fg, gradients[3]);

        RealMatrix wr_bg = computeweights(diffI_bg, gradients[0]); // TODO optimize alpha*diffI_fg
        RealMatrix wl_bg = computeweights(diffI_bg, gradients[2]);
        RealMatrix wt_bg = computeweights(diffI_bg, gradients[1]);
        RealMatrix wb_bg = computeweights(diffI_bg, gradients[3]);

        // compute averaged weights, left/right and top/bottom
        // used when computing second spatial derivate from first one
        RealMatrix avgwx_fg = wl_fg.add(wr_fg);
        avgwx_fg.walkInOptimizedOrder(new MatrixElementMultiply(0.5));
        RealMatrix avgwy_fg = wt_fg.add(wb_fg);
        avgwy_fg.walkInOptimizedOrder(new MatrixElementMultiply(0.5));
        RealMatrix avgwx_bg = wl_bg.add(wr_bg);
        avgwx_bg.walkInOptimizedOrder(new MatrixElementMultiply(0.5));
        RealMatrix avgwy_bg = wt_bg.add(wb_bg);
        avgwy_bg.walkInOptimizedOrder(new MatrixElementMultiply(0.5));
        // d is the diffusion constant times the timestep of the Euler scheme obeye CFL stability
        // criterion. D*dt/(dx^2) should be <<1/4
        double drl2, dtb2;
        double tmp1, tmp2;
        tmp1 = getStabilityCriterion(wr_fg, avgwx_fg);
        tmp2 = getStabilityCriterion(wl_fg, avgwx_fg);
        drl2 = tmp1 < tmp2 ? tmp1 : tmp2;
        tmp1 = getStabilityCriterion(wt_fg, avgwy_fg);
        tmp2 = getStabilityCriterion(wb_fg, avgwy_fg); // WARN See differences between matlab and
                                                       // java in definitoin up,down,etx. In matlab
                                                       // it seems to be wrong according to names
        dtb2 = tmp1 < tmp2 ? tmp1 : tmp2;
        double D = drl2 < dtb2 ? drl2 : dtb2; // D=0.25*min(drl2,dtb2)
        D *= 0.25;

        // precompute terms for loop
        wr_fg.walkInOptimizedOrder(new MatrixDotProduct(avgwx_fg));
        wl_fg.walkInOptimizedOrder(new MatrixDotProduct(avgwx_fg));
        wt_fg.walkInOptimizedOrder(new MatrixDotProduct(avgwy_fg));
        wb_fg.walkInOptimizedOrder(new MatrixDotProduct(avgwy_fg));

        wr_bg.walkInOptimizedOrder(new MatrixDotProduct(avgwx_bg));
        wl_bg.walkInOptimizedOrder(new MatrixDotProduct(avgwx_bg));
        wt_bg.walkInOptimizedOrder(new MatrixDotProduct(avgwy_bg));
        wb_bg.walkInOptimizedOrder(new MatrixDotProduct(avgwy_bg));

        // main loop
        for (int i = 0; i < params.Iter; i++) {

        }
    }

    /**
     * 
     * @param diffI normalized squared differences to mean seed intensities
     * @param grad2 normalized the squared gradients by the maximum gradient
     * @return wr_fg = exp(P.alpha*diffI_fg+P.beta*G.gradright2);
     * @todo optimize this part, see matlab code, products are the same
     */
    private RealMatrix computeweights(RealMatrix diffI, RealMatrix grad2) {
        double alpha = params.alpha;
        double beta = params.beta;
        RealMatrix ad = diffI.scalarMultiply(alpha);
        RealMatrix bg = grad2.scalarMultiply(beta);
        RealMatrix w = ad.add(bg);
        w.walkInOptimizedOrder(new MatrixElementExp());
        return w;
    }

    /**
     * Compute part of stability criterion for stopping iterations
     * 
     * Only in-place multiplication and minimu of result is done here
     * @code
     *  drl2 =  min ( min(min (wr_fg.*avgwx_fg)) , min(min (wl_fg.*avgwx_fg)) );
     *  dtb2 =  min ( min(min (wt_fg.*avgwy_fg)) , min(min (wb_fg.*avgwy_fg)) );
     *  D=0.25*min(drl2,dtb2); 
     * @endcode   
     * @param w__fg
     * @param avgw__fg
     * @return min(min (w__fg.*avgw__fg))
     */
    private double getStabilityCriterion(RealMatrix w__fg, RealMatrix avgw__fg) {
        RealMatrix cp = w__fg.copy();
        cp.walkInOptimizedOrder(new MatrixDotProduct(avgw__fg));
        return RandomWalkSegmentation.getMin(cp);
    }

    /**
     * Return values from \a in matrix that are on indexes \a ind
     * 
     * @param in Input matrix 2D
     * @param ind List of indexes
     * @return Values that are on indexes \a ind
     */
    protected ArrayRealVector getValues(RealMatrix in, List<Point> ind) {
        ArrayRealVector out = new ArrayRealVector(ind.size());
        int l = 0;
        for (Point p : ind)
            out.setEntry(l++, in.getEntry(p.row, p.col));
        return out;
    }

    /**
     * Set values from \a val on indexes \a ind in array \a in 
     * @param in Input matrix. Will be modified
     * @param ind List of indexes
     * @param val List of values, length must be the same as \a ind or 1
     */
    protected void setValues(RealMatrix in, List<Point> ind, ArrayRealVector val) {
        if (ind.size() != val.getDimension() & val.getDimension() != 1)
            throw new InvalidParameterException(
                    "Vector with data must contain 1 element or the same as indexes");
        int delta, l = 0;
        if (val.getDimension() == 1)
            delta = 0;
        else
            delta = 1;
        for (Point p : ind) {
            in.setEntry(p.row, p.col, val.getDataRef()[l]);
            l += delta;
        }
    }

    /**
     * Basic class for storing point in Cartesian system
     * 
     * @author p.baniukiewicz
     * @date 23 Jun 2016
     *
     */
    class Point {
        int row, col;

        /**
         * @param row
         * @param col
         */
        public Point(int col, int row) {
            this.row = row;
            this.col = col;
        }

        /**
         * Default constructor
         */
        Point() {
            row = 0;
            col = 0;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + col;
            result = prime * result + row;
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Point other = (Point) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (col != other.col)
                return false;
            if (row != other.row)
                return false;
            return true;
        }

        private RandomWalkSegmentation getOuterType() {
            return RandomWalkSegmentation.this;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Point [row=" + row + ", col=" + col + "]";
        }

    }

}