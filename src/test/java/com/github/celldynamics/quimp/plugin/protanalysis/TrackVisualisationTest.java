package com.github.celldynamics.quimp.plugin.protanalysis;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.plugin.qanalysis.STmap;
import com.github.celldynamics.quimp.utils.IJTools;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

/**
 * The Class TrackVisualisationTest.
 *
 * @author p.baniukiewicz
 */
@RunWith(MockitoJUnitRunner.class)
public class TrackVisualisationTest {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(TrackVisualisationTest.class.getName());

  /** The original image. */
  private ImagePlus originalImage;
  // http://stackoverflow.com/questions/16467685/difference-between-mock-and-injectmocks
  /** The protrusion vis. */
  // @InjectMocks
  private TrackVisualisation.Stack protrusionVis;

  private static ImageJ ij;

  /**
   * SetUp ImageJ.
   * 
   * @throws Exception Exception
   */
  @BeforeClass
  public static void before() throws Exception {
    ij = new ImageJ();
  }

  /**
   * Exit ImageJ.
   * 
   * @throws Exception Exception
   */
  @AfterClass
  public static void after() throws Exception {
    IJTools.exitIj(ij);
    ij = null;
  }

  /**
   * tearDown.
   * 
   * @throws Exception Exception
   */
  @After
  public void tearDown() throws Exception {
    IJTools.closeAllImages();
  }

  /**
   * SetUp.
   * 
   * @throws Exception Exception
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this); // must be as we create mocked classes in mocked already
    // QParams
    originalImage = IJ
            .openImage("src/test/Resources-static/" + "fluoreszenz-test_eq_smooth_frames_1-5.tif");
    protrusionVis = new TrackVisualisation.Stack(originalImage);
  }

  /**
   * Test method for
   * {@link TrackVisualisation.Stack#addMaximaToImage(STmap, MaximaFinder)}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testAddCirclesToImage() throws Exception {
    int[] indexes = { 0, 1, 2, 3, 4, 5, 6 };
    int[] frames = { 0, 0, 0, 0, 0, 0, 0 };
    double[][] xs = { { 10 }, { 50 }, { 100 }, { 150 }, { 200 }, { 300 }, { 400 } };
    double[][] ys = { { 50 }, { 60 }, { 160 }, { 210 }, { 360 }, { 460 }, { 510 } };
    STmap mapCell = Mockito.mock(STmap.class);
    MaximaFinder mockFinder = Mockito.mock(MaximaFinder.class);
    Mockito.when(mapCell.getxMap()).thenReturn(xs);
    Mockito.when(mapCell.getyMap()).thenReturn(ys);

    Mockito.when(mockFinder.getMaxima()).thenReturn(new Polygon(indexes, frames, 7));

    protrusionVis.addMaximaToImage(mapCell, mockFinder);

    protrusionVis.getOriginalImage().setTitle("testAddPointsToImage");
    // protrusionVis.getOriginalImage().show();
    // while (protrusionVis.getOriginalImage().isVisible()) {
    // Thread.sleep(1500);
    // }
  }

  // /**
  // * Test method for {@link
  // ProtrusionVis#addTrackingLinesToImage(com.github.celldynamics.quimp.STmap,
  // java.util.List)}.
  // */
  // @Test
  // public void testAddTrackingLinesToImage() throws Exception {
  // ArrayList<Polygon> testRoi = new ArrayList<>();
  // int[] frames = { 2, 3, 4, 5, 6, 7, 8 };
  // int[] indexes = { 0, 0, 0, 0, 0, 0, 0 };
  // double[][] xs = { { 0 }, { 0 }, { 10 }, { 11 }, { 12 }, { 13 }, { 14 }, { 15 }, { 16 } };
  // double[][] ys = { { 0 }, { 0 }, { 50 }, { 51 }, { 52 }, { 53 }, { 54 }, { 55 }, { 56 } };
  //
  // STmap mapCell = Mockito.mock(STmap.class);
  // Mockito.when(mapCell.getxMap()).thenReturn(xs);
  // Mockito.when(mapCell.getyMap()).thenReturn(ys);
  //
  // testRoi.add(new Polygon(indexes, frames, 7));
  // LOGGER.trace(Arrays.toString(testRoi.get(0).ypoints));
  // protrusionVis.addTrackingLinesToImage(mapCell, testRoi);
  //
  // protrusionVis.getOriginalImage().setTitle("testAddTrackingLinesToImage");
  // // protrusionVis.getOriginalImage().show();
  // // while (protrusionVis.getOriginalImage().isVisible()) {
  // // Thread.sleep(1500);
  // // }
  // }
  //
  // /**
  // * Test method for {@link
  // Prot_Analysis.ProtrusionVis#addTrackingLinesToImage(com.github.celldynamics.quimp.STmap,
  // java.util.List)}.
  // * Case with empty tracking line
  // */
  // @Test
  // public void testAddTrackingLinesToImage_1() throws Exception {
  // ArrayList<Polygon> testRoi = new ArrayList<>();
  // int[] frames = { 2, 3, 4, 5, 6, 7, 8 };
  // int[] indexes = { 0, 0, 0, 0, 0, 0, 0 };
  // double[][] xs = { { 0 }, { 0 }, { 10 }, { 11 }, { 12 }, { 13 }, { 14 }, { 15 }, { 16 } };
  // double[][] ys = { { 0 }, { 0 }, { 50 }, { 51 }, { 52 }, { 53 }, { 54 }, { 55 }, { 56 } };
  //
  // STmap mapCell = Mockito.mock(STmap.class);
  // Mockito.when(mapCell.getxMap()).thenReturn(xs);
  // Mockito.when(mapCell.getyMap()).thenReturn(ys);
  //
  // testRoi.add(new Polygon(indexes, frames, 0));
  // LOGGER.trace(Arrays.toString(testRoi.get(0).ypoints));
  // protrusionVis.addTrackingLinesToImage(mapCell, testRoi);
  //
  // protrusionVis.getOriginalImage().setTitle("testAddTrackingLinesToImage_1");
  // // protrusionVis.getOriginalImage().show();
  // // while (protrusionVis.getOriginalImage().isVisible()) {
  // // Thread.sleep(1500);
  // // }
  // }

  /**
   * Test of ListPoint2iComparator.
   */
  @Test
  public void testListPoint2iComparator() {
    List<Point> expected = new ArrayList<>();
    expected.add(new Point(1, 11));
    expected.add(new Point(1, 1));
    expected.add(new Point(5, 44));

    List<Point> result = new ArrayList<>();
    result.add(new Point(1, 11));
    result.add(new Point(5, 44));
    result.add(new Point(1, 1));

    Collections.sort(result, protrusionVis.new ListPoint2iComparator());
    assertThat(result, is(expected));
  }

}
