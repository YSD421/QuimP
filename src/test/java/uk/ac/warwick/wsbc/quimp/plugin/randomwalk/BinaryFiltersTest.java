package uk.ac.warwick.wsbc.quimp.plugin.randomwalk;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import uk.ac.warwick.wsbc.quimp.plugin.randomwalk.BinaryFilters.MorphoOperations;
import uk.ac.warwick.wsbc.quimp.plugin.randomwalk.BinaryFilters.SimpleMorpho;

/**
 * Preparation images in Matlab:
 * 
 * <pre>
 * {@code i=255*uint8(imread('fg_test1.tif'));imwrite(i,'fg_test1.tif')}
 * </pre>
 * 
 * @author p.baniukiewicz
 *
 */
public class BinaryFiltersTest {

  /**
   * The tmpdir.
   */
  static String tmpdir = System.getProperty("java.io.tmpdir") + File.separator;
  private ImageProcessor im;
  private ImageProcessor im1;

  /**
   * @throws java.lang.Exception Exception on error
   */
  @Before
  public void setUp() throws Exception {
    im = IJ.openImage("src/test/resources/RW/bg_test1.tif").getProcessor();
    im1 = IJ.openImage("src/test/resources/binary_1.tif").getProcessor();
  }

  /**
   * @throws java.lang.Exception Exception on error
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test of filtering of real case.
   */
  @Test
  public void testSimpleMorphoFilter() {
    SimpleMorpho filter = new BinaryFilters.SimpleMorpho();
    ImageProcessor ret = filter.filter(im);
    IJ.saveAsTiff(new ImagePlus("test", ret), tmpdir + "testSimpleMorphoFilter_QuimP.tif");

  }

  /**
   * Test method for
   * {@link BinaryFilters#iterateMorphological(ImageProcessor, MorphoOperations, double)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testIterateMorphologicalIjopen() throws Exception {
    im.invert();
    ImageProcessor res = BinaryFilters.iterateMorphological(BinaryFilters.getBinaryProcessor(im),
            MorphoOperations.IJOPEN, 1);
    IJ.saveAsTiff(new ImagePlus("test", res), tmpdir + "testIterateMorphologicalIJOPEN1_QuimP.tif");

    res = BinaryFilters.iterateMorphological(BinaryFilters.getBinaryProcessor(im),
            MorphoOperations.IJOPEN, 10);
    IJ.saveAsTiff(new ImagePlus("test", res),
            tmpdir + "testIterateMorphologicalIJOPEN10_QuimP.tif");
  }

  /**
   * Test {@link BinaryFilters#iterateMorphological(ImageProcessor, MorphoOperations, double)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testIterateMorphologicalErode() throws Exception {
    // BinaryProcessor rete = BinaryFilters.getBinaryProcessor(im1);
    ImageProcessor rete = BinaryFilters.iterateMorphological(im1, MorphoOperations.ERODE, 3);
    IJ.saveAsTiff(new ImagePlus("", rete), tmpdir + "testIterateMorphologicalERODE3_QuimP.tif");
  }

  /**
   * Test {@link BinaryFilters#iterateMorphological(ImageProcessor, MorphoOperations, double)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testIterateMorphologicalDilate() throws Exception {
    // BinaryProcessor rete = BinaryFilters.getBinaryProcessor(im1);
    ImageProcessor rete = BinaryFilters.iterateMorphological(im1, MorphoOperations.DILATE, 5);
    IJ.saveAsTiff(new ImagePlus("", rete), tmpdir + "testIterateMorphologicalDILATE5_QuimP.tif");
  }

}
