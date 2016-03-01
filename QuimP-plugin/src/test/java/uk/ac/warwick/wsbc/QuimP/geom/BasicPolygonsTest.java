/**
 * @file BasicPolygonsTest.java
 * @date 29 Feb 2016
 */
package uk.ac.warwick.wsbc.QuimP.geom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import javax.vecmath.Point2d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author p.baniukiewicz
 * @date 29 Feb 2016
 *
 */
public class BasicPolygonsTest {

    private ArrayList<Point2d> points;
    private ArrayList<Point2d> point;
    private ArrayList<Point2d> points2;
    private ArrayList<Point2d> inpoints;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        points = new ArrayList<Point2d>();
        points.add(new Point2d(2, 2));
        points.add(new Point2d(9, 9));
        points.add(new Point2d(6, 4));
        points.add(new Point2d(12, 2));

        point = new ArrayList<Point2d>();
        point.add(new Point2d(2, 2));

        points2 = new ArrayList<Point2d>();
        points2.add(new Point2d(2, 2));
        points2.add(new Point2d(9, 9));

        inpoints = new ArrayList<Point2d>();
        inpoints.add(new Point2d(5, 4));
        inpoints.add(new Point2d(6, 5));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.getPolyPerim(final List<E>).
     * @pre Four point polygon
     * @post Perimeter
     */
    @Test
    public void testGetPolyPerim() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        double p = b.getPolyPerim(points);
        assertEquals(32.055, p, 1e-5);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.getPolyPerim(final List<E>).
     * @pre Only one point
     * @post Perimeter equals 0
     */
    @Test
    public void testGetPolyPerim_1() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        double p = b.getPolyPerim(point);
        assertEquals(0.0, p, 1e-5);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.getPolyArea(final List<E>).
     * @pre Four point polygon
     * @post Area
     */
    @Test
    public void testGetPolyArea() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        double p = b.getPolyArea(points);
        assertEquals(17.0, p, 1e-5);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.getPolyArea(final List<E>).
     * @pre one point
     * @post Area equals 0
     */
    @Test
    public void testGetPolyArea_1() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        double p = b.getPolyArea(point);
        assertEquals(0.0, p, 1e-5);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.getPolyArea(final List<E>).
     * @pre two points
     * @post Area equals 0
     */
    @Test
    public void testGetPolyArea_2() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        double p = b.getPolyArea(points2);
        assertEquals(0.0, p, 1e-5);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.isPointInside(final List<E>, final Tuple2d)
     * @pre Point inside
     * @post Return true
     */
    @Test
    public void testIsPointInside() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        boolean p = b.isPointInside(points, inpoints.get(0));
        assertTrue(p);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.isPointInside(final List<E>, final Tuple2d).
     * @pre Point equals vertices
     * @post Return true
     */
    @Test
    public void testIsPointInside_1() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        boolean p = b.isPointInside(points, new Point2d(6, 4));
        assertTrue(p);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.isPointInside(final List<E>, final Tuple2d).
     * @pre Point outside
     * @post Return false
     */
    @Test
    public void testIsPointInside_2() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        boolean p = b.isPointInside(points, new Point2d(7, 4));
        assertFalse(p);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.arePointsInside(final List<E>, final List<E>).
     * @pre Points inside
     * @post Return true
     */
    @Test
    public void testarePointsInside() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        boolean p = b.arePointsInside(points, inpoints);
        assertTrue(p);
    }

    /**
     * @test Test method for QuimP.geom.BasicPolygons.arePointsInside(final List<E>, final List<E>).
     * @pre Points inside and one outside
     * @post Return false
     */
    @Test
    public void testarePointsInside_1() throws Exception {
        BasicPolygons<Point2d> b = new BasicPolygons<Point2d>();
        ArrayList<Point2d> c = new ArrayList<>(inpoints);
        c.add(new Point2d(10, 10));
        boolean p = b.arePointsInside(points, c);
        assertFalse(p);
    }

}
