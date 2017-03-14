/**
 */
package uk.ac.warwick.wsbc.quimp.geom;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.vecmath.Point2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.RoiRotator;
import ij.process.ImageProcessor;
import uk.ac.warwick.wsbc.quimp.Outline;
import uk.ac.warwick.wsbc.quimp.geom.SegmentedShapeRoi;
import uk.ac.warwick.wsbc.quimp.geom.TrackOutline;
import uk.ac.warwick.wsbc.quimp.plugin.utils.RoiSaver;

// TODO: Auto-generated Javadoc
/**
 * @author p.baniukiewicz
 *
 */
public class TrackOutlineTest {

  /**
   * The tmpdir.
   */
  static String tmpdir = System.getProperty("java.io.tmpdir") + File.separator;

  /**
   * Accessor to private field.
   * 
   * @param name Name of private field
   * @param obj Reference to object
   * @return of private method
   * @throws NoSuchFieldException
   * @throws SecurityException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  static Object accessPrivateField(String name, TrackOutline obj) throws NoSuchFieldException,
          SecurityException, IllegalArgumentException, IllegalAccessException {
    Field prv = obj.getClass().getDeclaredField(name);
    prv.setAccessible(true);
    return prv.get(obj);
  }

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(TrackOutlineTest.class.getName());
  private ImagePlus image;
  private TrackOutline obj;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    image = IJ.openImage("src/test/resources/outline_track_1.tif");
    obj = new TrackOutline(image, 0);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    if (image.changes) { // check if source was modified
      image.changes = false; // set flag to false to prevent save dialog
      image.close(); // close image
      throw new Exception("Image has been modified"); // throw exception
                                                      // if source image
                                                      // was modified
    }
    image.close();
    obj = null;
  }

  /**
   * testPrepare
   * 
   * post: Generate filtered image
   * 
   * @throws Exception
   */
  @Test
  public void testPrepare() throws Exception {
    ImageProcessor ret = obj.prepare();
    ImagePlus r = image.duplicate();
    r.setProcessor(ret);
    IJ.saveAsTiff(r, tmpdir + "testPrepare.tif");
  }

  /**
   * testGetOutlines
   * 
   * post: Finds all outlines in image and saves them to separate files
   * 
   * @throws Exception
   */
  @Test
  public void testGetOutlines() throws Exception {
    List<List<Point2d>> ret = obj.getOutlinesasPoints(1, false);
    LOGGER.debug("Found " + ret.size());
    ImagePlus r = image.duplicate();
    r.setProcessor((ImageProcessor) accessPrivateField("prepared", obj));
    IJ.saveAsTiff(r, tmpdir + "testGetOutlines.tif");
    RoiSaver.saveROI(tmpdir + "testGetOutlines_roi0.tif", ret.get(0));
    RoiSaver.saveROI(tmpdir + "testGetOutlines_roi1.tif", ret.get(1));
    RoiSaver.saveROI(tmpdir + "testGetOutlines_roi2.tif", ret.get(2));
  }

  /**
   * testGetOutlines_1
   * 
   * post: Finds all outlines in image with smoothing
   * 
   * @throws Exception
   */
  @Test
  public void testGetOutlines_1() throws Exception {
    List<List<Point2d>> ret = obj.getOutlinesasPoints(1, true);
    LOGGER.debug("Found " + ret.size());
    RoiSaver.saveROI(tmpdir + "testGetOutlines_roi_s.tif", ret.get(0));
  }

  /**
   * testGetOutlines_2
   * 
   * post: Finds all outlines in image with smoothing and step 6
   * 
   * @throws Exception
   */
  @Test
  public void testGetOutlines_6() throws Exception {
    List<List<Point2d>> ret = obj.getOutlinesasPoints(6, true);
    LOGGER.debug("Found " + ret.size());
    RoiSaver.saveROI(tmpdir + "testGetOutlines_roi_s6.tif", ret.get(0));
  }

  /**
   * Validates what is returned from ShapeRoi.and
   * 
   * post: operation ret.get(1).and(new ShapeRoi(pr)); modifies ret.get(1) If there is no
   * intersection it return shape wit 0 width/height
   */
  @Test
  public void testIntersection() {
    List<SegmentedShapeRoi> ret = obj.outlines;

    // simulate other ROI
    PolygonRoi pr = new PolygonRoi(ret.get(1).getPolygon(), Roi.FREEROI);
    pr = (PolygonRoi) RoiRotator.rotate(pr, 45); // roate
    LOGGER.debug("ret.get(1) " + ret.get(1));
    ShapeRoi sa1 = ret.get(1).and(new ShapeRoi(pr)); // make common part
    LOGGER.debug("Shape1 " + sa1);
    LOGGER.debug("ret.get(1) after " + ret.get(1));
    RoiSaver.saveRoi(tmpdir + "testIntersection_and.tif", sa1);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetOutlinesDoubleBoolean() throws Exception {
    List<Outline> ret = obj.getOutlines(4, false);
    RoiSaver.saveROI(tmpdir + "test0.tif", ret.get(0).asList());
    RoiSaver.saveROI(tmpdir + "test1.tif", ret.get(1).asList());
    RoiSaver.saveROI(tmpdir + "test2.tif", ret.get(2).asList());
  }

}