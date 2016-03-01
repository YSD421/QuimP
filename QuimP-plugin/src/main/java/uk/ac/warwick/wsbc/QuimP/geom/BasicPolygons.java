/**
 * @file BasicPolygons.java
 * @date 29 Feb 2016
 */
package uk.ac.warwick.wsbc.QuimP.geom;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;
import javax.vecmath.Vector2d;

/**
 * Calculates basic geometry on polygons defined as list of point in specified direction
 * 
 * @author p.baniukiewicz
 * @date 29 Feb 2016
 * @see http://www.mathopenref.com/coordpolygonarea.html
 * @todo integrate this class with awt.polygona maybe
 */
public class BasicPolygons<T extends Tuple2d> {

    public BasicPolygons() {

    }

    /**
     * Calculates area of polygon.
     * 
     * Supports triangles, regular and irregular polygons, convex or concave polygons
     * 
     * @param P Vertices of polygon in specified order
     * @return Area
     * @warning Polygon can not intersect itself.
     */
    public double getPolyArea(final List<T> P) {
        int i, j;
        double area = 0;

        for (i = 0; i < P.size(); i++) {
            j = (i + 1) % P.size(); // will round pointer to 0 for last point
            Tuple2d pi = P.get(i);
            Tuple2d pj = P.get(j);
            area += pi.getX() * pj.getY();
            area -= pi.getY() * pj.getX();
        }
        area /= 2.0;
        return (Math.abs(area));
    }

    /**
     * Calculates perimeter of polygon.
     * 
     * @param P Vertices of polygon in specified order
     * @return Perimeter
     */
    public double getPolyPerim(final List<T> P) {
        int i, j;
        double len = 0;
        ArrayList<Vector2d> V = new ArrayList<>();
        // get vectors between points
        for (i = 0; i < P.size(); i++) {
            j = (i + 1) % P.size(); // will round pointer to 0 for last point
            Tuple2d first = P.get(i);
            Tuple2d second = P.get(j);
            Vector2d tmp = new Vector2d(second.getX() - first.getX(), second.getY() - first.getY());
            V.add(tmp);
        }
        for (Vector2d v : V)
            len += v.length();
        return len;
    }

    /**
     * Test whether \c Ptest is inside polygon \c P
     * 
     * @param P Vertices of polygon in specified order
     * @param Ptest Point to be tested
     * @return \c true if \c Ptest is inside \c P, \c false otherwise
     * @see http://www.shodor.org/~jmorrell/interactivate/org/shodor/util11/PolygonUtils.java
     */
    public boolean isPointInside(final List<T> P, final Tuple2d Ptest) {
        double angle = 0;
        Point2d p1 = null, p2 = null;
        for (int i = 0; i < P.size(); i++) {
            p1 = new Point2d(P.get(i).getX() - Ptest.getX(), P.get(i).getY() - Ptest.getY());
            p2 = new Point2d(P.get((i + 1) % P.size()).getX() - Ptest.getX(),
                    P.get((i + 1) % P.size()).getY() - Ptest.getY());
            angle += angle2D(p1, p2);
        }
        return Math.abs(angle) >= Math.PI;
    }

    /**
     * Helper method
     * @param p1
     * @param p2
     * @return
     */
    private double angle2D(Point2d p1, Point2d p2) {
        double dtheta = Math.atan2(p2.y, p2.x) - Math.atan2(p1.y, p1.x);
        while (dtheta > Math.PI)
            dtheta -= 2.0 * Math.PI;
        while (dtheta < -1.0 * Math.PI)
            dtheta += 2.0 * Math.PI;
        return dtheta;
    }

    /**
     * Test if all points \c Ptest are inside polygon
     * @copydetails isPointInside(final List<E>, final Tuple2d)
     */
    public boolean arePointsInside(final List<T> P, final List<T> Ptest) {
        boolean result = true;
        for (T p : Ptest) {
            result &= isPointInside(P, p);
        }
        return result;
    }

}
