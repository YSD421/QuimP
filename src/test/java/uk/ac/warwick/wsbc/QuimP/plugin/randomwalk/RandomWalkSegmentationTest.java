/**
 * @file RandomWalkSegmentationTest.java
 * @date 22 Jun 2016
 */
package uk.ac.warwick.wsbc.QuimP.plugin.randomwalk;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;

/**
 * @author p.baniukiewicz
 * @date 22 Jun 2016
 *
 */
public class RandomWalkSegmentationTest extends RandomWalkSegmentation {
    static {
        System.setProperty("log4j.configurationFile", "qlog4j2.xml");
    }
    private static final Logger LOGGER =
            LogManager.getLogger(RandomWalkSegmentationTest.class.getName());

    static ImagePlus testImage1; // original 8bit grayscale
    static ImagePlus testImage1seed; // contains rgb image with seed
    static ImagePlus testImage1rgb; // contains rgb image with test seed points

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        testImage1seed = IJ.openImage("src/test/resources/segtest_small_seed.tif");
        testImage1 = IJ.openImage("src/test/resources/segtest_small.tif");
        testImage1rgb = IJ.openImage("src/test/resources/segtest_small_rgb_test.tif");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        testImage1.close();
        testImage1 = null;
        testImage1seed.close();
        testImage1seed = null;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        if (testImage1.changes) { // check if source was modified
            testImage1.changes = false; // set flag to false to prevent save dialog
            throw new Exception("Image has been modified"); // throw exception if source image
                                                            // was modified
        }
    }

    /**
     * @Test of circshift(RealMatrix, int)
     * @pre any 2D matrix
     * @post this matrix shifted to RIGHT
     */
    @Test
    public void testCircshift_right() throws Exception {
        double[][] test = { { 1, 2, 3, 4 }, { 2, 3, 4, 5 }, { 6, 7, 8, 9 } };
        double[][] expected = { { 4, 1, 2, 3 }, { 5, 2, 3, 4 }, { 9, 6, 7, 8 } };
        RealMatrix testrm = MatrixUtils.createRealMatrix(test);
        RealMatrix shift = circshift(testrm, RandomWalkSegmentation.RIGHT);
        assertThat(shift, is(MatrixUtils.createRealMatrix(expected)));
    }

    /**
     * @Test of circshift(RealMatrix, int)
     * @pre any 2D matrix
     * @post this matrix shifted to LEFT
     */
    @Test
    public void testCircshift_left() throws Exception {
        double[][] test = { { 1, 2, 3, 4 }, { 2, 3, 4, 5 }, { 6, 7, 8, 9 } };
        double[][] expected = { { 2, 3, 4, 1 }, { 3, 4, 5, 2 }, { 7, 8, 9, 6 } };
        RealMatrix testrm = MatrixUtils.createRealMatrix(test);
        RealMatrix shift = circshift(testrm, RandomWalkSegmentation.LEFT);
        assertThat(shift, is(MatrixUtils.createRealMatrix(expected)));
    }

    /**
     * @Test of circshift(RealMatrix, int)
     * @pre any 2D matrix
     * @post this matrix shifted to TOP
     */
    @Test
    public void testCircshift_top() throws Exception {
        //!<
        double[][] test =     { { 1, 2, 3, 4 },
                                { 2, 3, 4, 5 },
                                { 6, 7, 8, 9 } };
        
        double[][] expected = { { 2, 3, 4, 5 },
                                { 6, 7, 8, 9 },
                                { 1, 2, 3, 4 }};
        /**/
        RealMatrix testrm = MatrixUtils.createRealMatrix(test);
        RealMatrix shift = circshift(testrm, RandomWalkSegmentation.TOP);
        assertThat(shift, is(MatrixUtils.createRealMatrix(expected)));
    }

    /**
     * @test Test of circshift(RealMatrix, int)
     * @pre any 2D matrix
     * @post this matrix shifted to BOTTOM
     */
    @Test
    public void testCircshift_bottom() throws Exception {
        //!<
        double[][] test =     { { 1, 2, 3, 4 },
                                { 2, 3, 4, 5 },
                                { 6, 7, 8, 9 } };
        
        double[][] expected = { { 6, 7, 8, 9 },
                                { 1, 2, 3, 4 },
                                { 2, 3, 4, 5 } };
        /**/
        RealMatrix testrm = MatrixUtils.createRealMatrix(test);
        RealMatrix shift = circshift(testrm, RandomWalkSegmentation.BOTTOM);
        assertThat(shift, is(MatrixUtils.createRealMatrix(expected)));
    }

    /**
     * @test Test of getSqrdDiffIntensity(RealMatrix, RealMatrix) 
     * @throws Exception
     */
    @Test
    public void testGetSqrdDiffIntensity() throws Exception {
      //!<
        double[][] a =        { { 1, 2 },
                                { 2, 3 } };
        
        double[][] b =        { { 3, 4 },
                                { 6, 2 } };
        
        double[][] expected = { { 4, 4 },
                                { 16, 1 } };
        /**/
        RealMatrix out = getSqrdDiffIntensity(MatrixUtils.createRealMatrix(a),
                MatrixUtils.createRealMatrix(b));
        assertThat(out, is(MatrixUtils.createRealMatrix(expected)));
    }

    /**
     * @test Test of getMin(RealMatrix)
     * @throws Exception
     */
    @Test
    public void testGetMin() throws Exception {
        //!<
        double[][] test =     { { 1, 2, 3, 4 },
                                { 2, 3, 40, 5 },
                                { 6, 7, 8, 9 } };
        /**/
        assertThat(getMax(MatrixUtils.createRealMatrix(test)), is(40.0));
    }

    /**
     * @test Analysis of getSubMatrix from Apache
     */
    @Test
    public void testGetSubMatrix() {
        //!<
        double[][] test =     { { 1, 2, 3, 4 },
                                { 2, 3, 40, 5 },
                                { 6, 7, 8, 9 } };
        /**/
        int r[] = { 0, 1, 0 };
        int c[] = { 0, 2, 3 };

        RealMatrix in = MatrixUtils.createRealMatrix(test);
        RealMatrix out = in.getSubMatrix(r, c);
        LOGGER.debug(out.toString());
    }

    /**
     * @test Test of decodeSeeds(ImagePlus, Color, Color)
     * @pre Image with gree/red seed with known positions (\a segtest_small.rgb.tif)
     * @post Two lists with positions ot seeds
     * 
     * @throws Exception
     */
    @Test
    public void testDecodeSeeds() throws Exception {
        Set<Point> expectedForeground = new HashSet<Point>();
        expectedForeground.add(new Point(70, 70));
        expectedForeground.add(new Point(71, 70));
        expectedForeground.add(new Point(72, 70));
        expectedForeground.add(new Point(100, 20));

        Set<Point> expectedBackground = new HashSet<Point>();
        expectedBackground.add(new Point(20, 20));
        expectedBackground.add(new Point(40, 40));
        expectedBackground.add(new Point(60, 60));

        Map<Integer, List<Point>> ret = decodeSeeds(testImage1rgb, Color.RED, Color.GREEN);

        Set<Point> fseeds = new HashSet<>(ret.get(RandomWalkSegmentation.FOREGROUND));
        Set<Point> bseeds = new HashSet<>(ret.get(RandomWalkSegmentation.BACKGROUND));

        assertThat(fseeds, is(expectedForeground));
        assertThat(bseeds, is(expectedBackground));

    }

    /**
     * @test Test of getValues(RealMatrix, List<Point>)
     * @throws Exception
     */
    @Test
    public void testGetValues() throws Exception {
        //!<
        double[][] test =     { { 1, 2, 3, 4 },
                                { 2, 3, 40, 5 },
                                { 6, 7, 8, 9 } };
        /**/
        double[] expected = { 1, 6, 40 };
        RealMatrix in = MatrixUtils.createRealMatrix(test);
        List<Point> ind = new ArrayList<>();
        ind.add(new Point(0, 0));
        ind.add(new Point(0, 2));
        ind.add(new Point(2, 1));
        ArrayRealVector ret = getValues(in, ind);
        assertThat(ret.getDataRef(), is(expected));
    }

    /**
     * @test of setValues(RealMatrix, List<Point>, ArrayRealVector)
     * @throws Exception
     */
    @Test
    public void testSetValues() throws Exception {
        //!<
        double[][] test =     { { 1, 2, 3, 4 },
                                { 2, 3, 40, 5 },
                                { 6, 7, 8, 9 } };
        /**/
        RealMatrix in = MatrixUtils.createRealMatrix(test);
        List<Point> ind = new ArrayList<>();
        ind.add(new Point(0, 0)); // col,row
        ind.add(new Point(0, 2));
        ind.add(new Point(2, 1));
        double[] toSet = { -1, -2, -3 }; // values to set into indexes ind
        // TODO finish
    }

}