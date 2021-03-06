package com.github.celldynamics.quimp.geom;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Point;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.QParamsQconf;
import com.github.celldynamics.quimp.filesystem.QconfLoader;

/**
 * Test class for {@link com.github.celldynamics.quimp.geom.MapTracker}.
 * 
 * @author p.baniukiewicz
 *
 */
public class TrackMapTest {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(TrackMapTest.class.getName());

  /**
   * The q L 1.
   */
  static QconfLoader qL1;

  /**
   * The origin map 1.
   */
  double[][] originMap1;

  /**
   * The coord map 1.
   */
  double[][] coordMap1;

  /**
   * The q L 2.
   */
  static QconfLoader qL2;

  /**
   * The origin map 2.
   */
  double[][] originMap2;

  /**
   * The coord map 2.
   */
  double[][] coordMap2;

  /**
   * Sets the up before class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    qL1 = new QconfLoader(Paths
            .get("src/test/Resources-static/TrackMapTests/Stack_cut_10frames_trackMapTest.QCONF")
            .toFile());
    qL2 = new QconfLoader(
            Paths.get("src/test/Resources-static/TrackMapTests/fluoreszenz-test_eq_smooth.QCONF")
                    .toFile());
  } // throw new UnsupportedOperationException("Not implemented here");

  /**
   * Tear down after class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    coordMap1 = ((QParamsQconf) qL1.getQp()).getLoadedDataContainer().QState[0].getCoordMap();
    originMap1 = ((QParamsQconf) qL1.getQp()).getLoadedDataContainer().QState[0].getOriginMap();

    coordMap2 = ((QParamsQconf) qL2.getQp()).getLoadedDataContainer().QState[0].getCoordMap();
    originMap2 = ((QParamsQconf) qL2.getQp()).getLoadedDataContainer().QState[0].getOriginMap();
  }

  /**
   * Tear down.
   *
   * @throws Exception the exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.geom.MapTracker}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @Test
  public void testTrackMap() throws Exception {
    //!<
    int[][] forwardExpected = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 } };
    int[][] backwardExpected =
            { { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 } };
    /**/
    MapTracker tmpMt = new MapTracker(originMap1, coordMap1);
    assertThat(tmpMt.forwardMap, is(forwardExpected));
    assertThat(tmpMt.backwardMap, is(backwardExpected));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackForward(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings({ "deprecation", "javadoc" })
  @Test
  public void testTrackForward_1() throws Exception {
    int[] expected = { 4, 4, 4, 4, 4, 4, 4, 4, 4, -1 };
    MapTracker tmpMt = new MapTracker(originMap1, coordMap1);
    int[] ret = tmpMt.trackForward(0, 4, 10);
    assertThat(ret, is(expected));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackForwardValid(int, int, int)}.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("serial")
  @Test
  public void testTrackForwardValid_1() throws Exception {
    ArrayList<Point> e = new ArrayList<Point>() {
      {
        add(new Point(1, 4));
        add(new Point(2, 4));
        add(new Point(3, 4));
        add(new Point(4, 4));
        add(new Point(5, 4));
        add(new Point(6, 4));
        add(new Point(7, 4));
        add(new Point(8, 4));
        add(new Point(9, 4));

      }
    };
    MapTracker tmpMt = new MapTracker(originMap1, coordMap1);
    ArrayList<Point> ret = (ArrayList<Point>) tmpMt.trackForwardValid(0, 4, 10);
    assertThat(ret, is(e));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackForward(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings({ "deprecation", "javadoc" })
  @Test
  public void testTrackForward_2() throws Exception {
    //!<
    int[] expected = { 262 - 1, 259 - 1, 263 - 1, 269 - 1, 265 - 1, 274 - 1, 276 - 1, 265 - 1,
        277 - 1, 276 - 1 };
    /**/
    MapTracker tmpMt = new MapTracker(originMap2, coordMap2);
    int[] ret = tmpMt.trackForward(90 - 1, 272 - 1, 10);
    assertThat(ret, is(expected));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackForwardValid(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("serial")
  @Test
  public void testTrackForwardValid_2() throws Exception {
    //!<
    ArrayList<Point> e = new ArrayList<Point>() {
      {
        add(new Point(90, 262 - 1));
        add(new Point(91, 259 - 1));
        add(new Point(92, 263 - 1));
        add(new Point(93, 269 - 1));
        add(new Point(94, 265 - 1));
        add(new Point(95, 274 - 1));
        add(new Point(96, 276 - 1));
        add(new Point(97, 265 - 1));
        add(new Point(98, 277 - 1));
        add(new Point(99, 276 - 1));
      }
    };
    /**/
    MapTracker tmpMt = new MapTracker(originMap2, coordMap2);
    ArrayList<Point> ret = (ArrayList<Point>) tmpMt.trackForwardValid(90 - 1, 272 - 1, 10);
    assertThat(ret, is(e));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#getForwardFrames(int, int)}.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({ "deprecation", "javadoc" })
  @Test
  public void testGetForwardFrames() throws Exception {
    //!>
    int[] expected =
            { 91 - 1, 92 - 1, 93 - 1, 94 - 1, 95 - 1, 96 - 1, 97 - 1, 98 - 1, 99 - 1, 100 - 1 };
    //!<
    MapTracker tmpMt = new MapTracker(originMap2, coordMap2);
    int[] ret = tmpMt.getForwardFrames(90 - 1, 10);
    assertThat(ret, is(expected));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackBackward(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings({ "deprecation", "javadoc" })
  @Test
  public void testTrackBackward_1() throws Exception {
    int[] expected = { -1, 4, 4, 4, 4, 4 };
    MapTracker tmpMt = new MapTracker(originMap1, coordMap1);
    int[] ret = tmpMt.trackBackward(5, 4, 6);
    assertThat(ret, is(expected));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackBackwardValid(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("serial")
  @Test
  public void testTrackBackwardValid_1() throws Exception {
    ArrayList<Point> e = new ArrayList<Point>() {
      {
        add(new Point(0, 4));
        add(new Point(1, 4));
        add(new Point(2, 4));
        add(new Point(3, 4));
        add(new Point(4, 4));
      }
    };
    MapTracker tmpMt = new MapTracker(originMap1, coordMap1);
    ArrayList<Point> ret = (ArrayList<Point>) tmpMt.trackBackwardValid(5, 4, 6);
    assertThat(ret, is(e));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackBackward(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings({ "deprecation", "javadoc" })
  @Test
  public void testTrackBackward_2() throws Exception {
    //!<
    int[] expected = { 303 - 1, 301 - 1, 300 - 1, 297 - 1, 291 - 1, 287 - 1, 278 - 1, 282 - 1,
        278 - 1, 284 - 1, 281 - 1, 292 - 1, 294 - 1, 283 - 1, 297 - 1 };
    /**/
    MapTracker tmpMt = new MapTracker(originMap2, coordMap2);
    int[] ret = tmpMt.trackBackward(100 - 1, 300 - 1, 15);
    assertThat(ret, is(expected));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#trackBackwardValid(int, int, int)}.
   * 
   * <p>Output results generated in Matlab by TrackMapTests/main.m
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("serial")
  @Test
  public void testTrackBackwardValid_2() throws Exception {
    //!<
    ArrayList<Point> e = new ArrayList<Point>() {
      {
        add(new Point(84, 303 - 1));
        add(new Point(85, 301 - 1));
        add(new Point(86, 300 - 1));
        add(new Point(87, 297 - 1));
        add(new Point(88, 291 - 1));
        add(new Point(89, 287 - 1));
        add(new Point(90, 278 - 1));
        add(new Point(91, 282 - 1));
        add(new Point(92, 278 - 1));
        add(new Point(93, 284 - 1));
        add(new Point(94, 281 - 1));
        add(new Point(95, 292 - 1));
        add(new Point(96, 294 - 1));
        add(new Point(97, 283 - 1));
        add(new Point(98, 297 - 1));

      }
    };
    /**/
    MapTracker tmpMt = new MapTracker(originMap2, coordMap2);
    ArrayList<Point> ret = (ArrayList<Point>) tmpMt.trackBackwardValid(100 - 1, 300 - 1, 15);
    assertThat(ret, is(e));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.geom.MapTracker#getBackwardFrames(int, int)}.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings({ "deprecation", "javadoc" })
  @Test
  public void testGetBackwardFrames() throws Exception {
    //!<
    int[] expected = { 85 - 1, 86 - 1, 87 - 1, 88 - 1, 89 - 1, 90 - 1, 91 - 1, 92 - 1, 93 - 1,
        94 - 1, 95 - 1, 96 - 1, 97 - 1, 98 - 1, 99 - 1 };
    /**/
    MapTracker tmpMt = new MapTracker(originMap2, coordMap2);
    int[] ret = tmpMt.getBackwardFrames(100 - 1, 15);
    assertThat(ret, is(expected));
  }
}
