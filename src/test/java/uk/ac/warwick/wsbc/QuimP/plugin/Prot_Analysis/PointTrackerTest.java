package uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.tools.javac.util.Pair;

/**
 * @author baniuk
 *
 */
public class PointTrackerTest {
    static {
        if (System.getProperty("quimp.debugLevel") == null)
            Configurator.initialize(null, "log4j2_default.xml");
        else
            Configurator.initialize(null, System.getProperty("quimp.debugLevel"));
    }

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
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y2 = { 10, 9, 8, 7, 5, 5, 4, 3, 2, 1 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] { 5 }, new int[] { 5 }, 1);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(0, 1), new Point(5, 5));

        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>(Arrays.asList(p));
        assertThat(ret1, is(expected1));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_nointersect() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y2 = { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] {}, new int[] {}, 0);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        assertThat(ret1.size(), is(0));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_2intersects() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y2 = { 10, 9, 8, 7, 5, 5, 4, 3, 9, 1 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] { 5, 9 }, new int[] { 5, 9 }, 2);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(0, 1), new Point(5, 5));
        Pair<Point, Point> p1 = new Pair<Point, Point>(new Point(0, 1), new Point(9, 9));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
                add(p1);
            }
        };

        assertThat(ret1, is(expected1));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_allthesame() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] { 1, 2, 3, 4, 6, 8, 5, 7, 10, 9 },
                new int[] { 1, 2, 3, 4, 6, 8, 5, 7, 10, 9 }, 10);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(0, 1), new Point(1, 1));
        Pair<Point, Point> p1 = new Pair<Point, Point>(new Point(0, 1), new Point(2, 2));
        Pair<Point, Point> p2 = new Pair<Point, Point>(new Point(0, 1), new Point(3, 3));
        Pair<Point, Point> p3 = new Pair<Point, Point>(new Point(0, 1), new Point(4, 4));
        Pair<Point, Point> p4 = new Pair<Point, Point>(new Point(0, 1), new Point(5, 5));
        Pair<Point, Point> p5 = new Pair<Point, Point>(new Point(0, 1), new Point(6, 6));
        Pair<Point, Point> p6 = new Pair<Point, Point>(new Point(0, 1), new Point(7, 7));
        Pair<Point, Point> p7 = new Pair<Point, Point>(new Point(0, 1), new Point(8, 8));
        Pair<Point, Point> p8 = new Pair<Point, Point>(new Point(0, 1), new Point(9, 9));
        Pair<Point, Point> p9 = new Pair<Point, Point>(new Point(0, 1), new Point(10, 10));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
                add(p1);
                add(p2);
                add(p3);
                add(p4);
                add(p5);
                add(p6);
                add(p7);
                add(p8);
                add(p9);
            }
        };

        assertThat(ret1, is(expected1));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_3() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y2 = { 10, 9, 8, 7, 5, 5, 4, 3, 2, 1 };

        int[] x3 = { 100, 6, 9, 40 };
        int[] y3 = { 100, 5, 9, 7 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));
        test.add(new Polygon(x3, y3, 4));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] { 6, 5, 9 }, new int[] { 5, 5, 9 }, 3);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(0, 1), new Point(5, 5));
        Pair<Point, Point> p1 = new Pair<Point, Point>(new Point(1, 2), new Point(6, 5));
        Pair<Point, Point> p2 = new Pair<Point, Point>(new Point(0, 2), new Point(9, 9));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
                add(p2);
                add(p1);
            }
        };
        assertThat(ret1, is(expected1));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     * The same point many times. Will be returned once.
     */
    @Test
    public void testGetIntersectionPoints_multiple() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 17, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 70, 8, 9, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 17, 8, 9, 10 };
        int[] y2 = { 10, 9, 8, 7, 6, 5, 70, 3, 2, 1 };

        int[] x3 = { 100, 61, 17, 40 };
        int[] y3 = { 100, 5, 70, 7 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));
        test.add(new Polygon(x3, y3, 4));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] { 17 }, new int[] { 70 }, 1);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(0, 1), new Point(17, 70));
        Pair<Point, Point> p1 = new Pair<Point, Point>(new Point(0, 2), new Point(17, 70));
        Pair<Point, Point> p2 = new Pair<Point, Point>(new Point(1, 2), new Point(17, 70));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
                add(p1);
                add(p2);
            }
        };
        assertThat(ret1, is(expected1));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_oneelementinlist() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 17, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 70, 8, 9, 10 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] {}, new int[] {}, 0);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);

        assertThat(ret1.size(), is(0));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_noforward() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 17, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 70, 8, 9, 10 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x1, y1, 0)); // no second

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] {}, new int[] {}, 0);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);

        assertThat(ret1.size(), is(0));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_nobackward() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 17, 8, 9, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 70, 8, 9, 10 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 0));
        test.add(new Polygon(x1, y1, 10)); // no second

        Polygon ret = new PointTracker().getIntersectionPoints(test);
        Polygon expected = new Polygon(new int[] {}, new int[] {}, 0);

        assertThat(ret.xpoints, is(expected.xpoints));
        assertThat(ret.ypoints, is(expected.ypoints));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);

        assertThat(ret1.size(), is(0));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_withoutselfcrossing() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 400, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 7, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int[] y2 = { 10, 9, 8, 7, 5, 5, 4, 3, 2, 1 };

        int[] x3 = { 100, 200, 300, 400 };
        int[] y3 = { 100, 5, 9, 7 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 10));
        test.add(new Polygon(x2, y2, 10));
        test.add(new Polygon(x3, y3, 4));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITHOUT_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(0, 2), new Point(400, 7));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
            }
        };
        assertThat(ret1, is(expected1));
        assertThat(test.size(), is(3)); // input not changed
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_nobckw() throws Exception {
        int[] x1 = { 1, 2, 3, 4, 5, 6, 7, 8, 400, 10 };
        int[] y1 = { 1, 2, 3, 4, 5, 6, 7, 8, 7, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 400, 8, 200, 10 };
        int[] y2 = { 10, 9, 8, 7, 5, 5, 7, 3, 5, 1 };

        int[] x3 = { 100, 200, 300, 400 };
        int[] y3 = { 100, 5, 9, 7 };

        int[] x4 = { 100, 11, 12, 13 };
        int[] y4 = { 100, 5, 9, 7 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 0)); // no backward track
        test.add(new Polygon(x2, y2, 10));
        test.add(new Polygon(x3, y3, 4));
        test.add(new Polygon(x4, y4, 4));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITHOUT_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(1, 2), new Point(400, 7));
        Pair<Point, Point> p1 = new Pair<Point, Point>(new Point(1, 2), new Point(200, 5));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
                add(p1);
            }
        };
        assertThat(ret1, is(expected1));

        ret1 = new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> pa = new Pair<Point, Point>(new Point(1, 2), new Point(400, 7));
        Pair<Point, Point> p1a = new Pair<Point, Point>(new Point(1, 2), new Point(200, 5));
        Pair<Point, Point> p2a = new Pair<Point, Point>(new Point(2, 3), new Point(100, 100));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected2 = new ArrayList<Pair<Point, Point>>() {
            {
                add(pa);
                add(p1a);
                add(p2a);
            }
        };
        assertThat(ret1, is(expected2));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.PointTracker#getIntersectionPoints(java.util.List)}.
     */
    @Test
    public void testGetIntersectionPoints_noforw() throws Exception {
        int[] x1 = { 1, 200, 3, 4, 5, 6, 7, 8, 400, 10 };
        int[] y1 = { 1, 5, 3, 4, 5, 6, 7, 8, 7, 10 };

        int[] x2 = { 1, 2, 3, 4, 5, 6, 400, 8, 200, 10 };
        int[] y2 = { 10, 9, 8, 7, 5, 5, 7, 3, 5, 1 };

        int[] x3 = { 100, 200, 300, 400 };
        int[] y3 = { 100, 5, 9, 7 };

        int[] x4 = { 100, 11, 12, 13 };
        int[] y4 = { 100, 5, 9, 7 };

        ArrayList<Polygon> test = new ArrayList<>();
        test.add(new Polygon(x1, y1, 0));
        test.add(new Polygon(x2, y2, 10)); // no forward track
        test.add(new Polygon(x3, y3, 4));
        test.add(new Polygon(x4, y4, 4));

        List<Pair<Point, Point>> ret1 =
                new PointTracker().getIntersectionParents(test, PointTracker.WITHOUT_SELFCROSSING);
        Pair<Point, Point> p = new Pair<Point, Point>(new Point(1, 2), new Point(400, 7));
        Pair<Point, Point> p1 = new Pair<Point, Point>(new Point(1, 2), new Point(200, 5));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected1 = new ArrayList<Pair<Point, Point>>() {
            {
                add(p);
                add(p1);
            }
        };
        assertThat(ret1, is(expected1));

        ret1 = new PointTracker().getIntersectionParents(test, PointTracker.WITH_SELFCROSSING);
        Pair<Point, Point> pa = new Pair<Point, Point>(new Point(1, 2), new Point(400, 7));
        Pair<Point, Point> p1a = new Pair<Point, Point>(new Point(1, 2), new Point(200, 5));
        Pair<Point, Point> p2a = new Pair<Point, Point>(new Point(2, 3), new Point(100, 100));

        @SuppressWarnings("serial")
        List<Pair<Point, Point>> expected2 = new ArrayList<Pair<Point, Point>>() {
            {
                add(pa);
                add(p1a);
                add(p2a);
            }
        };
        assertThat(ret1, is(expected2));
    }

}
