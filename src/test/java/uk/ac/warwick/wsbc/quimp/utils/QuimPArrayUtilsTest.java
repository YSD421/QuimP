package uk.ac.warwick.wsbc.quimp.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * @author p.baniukiewicz
 *
 */
public class QuimPArrayUtilsTest {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(QuimPArrayUtilsTest.class.getName());

  /**
   * The tmpdir.
   */
  static String tmpdir = System.getProperty("java.io.tmpdir") + File.separator;

  /**
   * @throws java.lang.Exception on error
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception on error
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for
   * {@link uk.ac.warwick.wsbc.quimp.utils.QuimPArrayUtils#float2ddouble(float[][])}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testFloat2Ddouble() throws Exception {
    float[][] in = { { 1.0f, 2.0f, 3.0f }, { 1.11f, 2.11f, 3.11f } };
    double[][] out = QuimPArrayUtils.float2ddouble(in);
    for (int r = 0; r < 2; r++) {
      for (int c = 0; c < 3; c++) {
        assertEquals(in[r][c], out[r][c], 1e-3);
      }
    }
  }

  /**
   * Test method for
   * {@link uk.ac.warwick.wsbc.quimp.utils.QuimPArrayUtils#double2float(double[][])}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testDouble2Float() throws Exception {
    double[][] in = { { 1.0, 2.0, 3.0 }, { 1.11, 2.11, 3.11 } };
    float[][] out = QuimPArrayUtils.double2float(in);
    for (int r = 0; r < 2; r++) {
      for (int c = 0; c < 3; c++) {
        assertEquals(in[r][c], out[r][c], 1e-3);
      }
    }
  }

  /**
   * Test method of {@link QuimPArrayUtils#minListIndex(java.util.List)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testMinListIndex() throws Exception {
    ArrayList<Double> ar = new ArrayList<>();
    ar.add(34.0);
    ar.add(5.0);
    ar.add(-5.0);

    assertThat(QuimPArrayUtils.minListIndex(ar), equalTo(2));
  }

  /**
   * Test method for
   * {@link QuimPArrayUtils#file2Array(java.lang.String, java.io.File)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testFile2Array() throws Exception {
    //!>
    double[][] expected =
        { { 1, 2, 3, 4, 5 },
        { 1.1, 2.2, 3.3, 4.4, 5.5 },
        { 6, 7, 8, 9, Math.PI } };
    //!<
    QuimPArrayUtils.arrayToFile(expected, ",", new File(tmpdir + "testFile2Array.map"));

    double[][] test = QuimPArrayUtils.file2Array(",", new File(tmpdir + "testFile2Array.map"));

    assertThat(test, is(expected));

  }

  /**
   * Test method for
   * {@link QuimPArrayUtils#realMatrix2D2File(RealMatrix, java.lang.String)}.
   * 
   * @throws IOException on file problem
   */
  @Test
  public void testRealMatrix2D2File() throws IOException {
    int rows = 4;
    int cols = 3;
    RealMatrix test = new Array2DRowRealMatrix(rows, cols);
    int l = 0;
    for (int r = 0; r < rows; r++) {
      for (int k = 0; k < cols; k++) {
        test.setEntry(r, k, l++);
      }
    }
    QuimPArrayUtils.realMatrix2D2File(test, tmpdir + "testRealMatrix2D2File.txt");
  }

  /**
   * Test of getMin(RealMatrix).
   * 
   * @throws Exception on error
   */
  @Test
  public void testGetMin() throws Exception {
    double[][] test = { { 1, 2, 3, 4 }, { 2, 3, 40, 5 }, { 6, 7, 8, 9 } };
    assertThat(QuimPArrayUtils.getMax(MatrixUtils.createRealMatrix(test)), is(40.0));
  }

}