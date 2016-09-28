package uk.ac.warwick.wsbc.QuimP.plugin.protanalysis;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.tools.javac.util.Pair;

import uk.ac.warwick.wsbc.QuimP.geom.MapTracker;
import uk.ac.warwick.wsbc.QuimP.plugin.qanalysis.STmap;
import uk.ac.warwick.wsbc.QuimP.utils.QuimPArrayUtils;

/**
 * Track point using tracking map generated by {@link uk.ac.warwick.wsbc.QuimP.geom.MapTracker}.
 * 
 * All methods in this class are safe either for empty tracks returned by MapTracker or trackMaxima.
 * Tracking point coordinates may contain invalid values (negative).
 * 
 * @author p.baniukiewicz
 *
 */
public class TrackMapAnalyser {
    private static final Logger LOGGER = LogManager.getLogger(TrackMapAnalyser.class.getName());
    /**
     * Allow detection common points in backward and forward tracks generated for the same
     * starting point.
     * @see uk.ac.warwick.wsbc.QuimP.geom.MapTracker.includeFirst
     */
    final public static int WITH_SELFCROSSING = 2;
    /**
     * Disallow detection common points in backward and forward tracks generated for the same
     * starting point.
     * @see uk.ac.warwick.wsbc.QuimP.geom.MapTracker.includeFirst
     */
    final public static int WITHOUT_SELFCROSSING = 4;
    /**
     * Maximum point (source of tracks) is included in tracks (if <tt>true</tt>).
     * 
     * It should be changed carefully as many other procedures can assume that first point is 
     * included in Tracks.
     */
    public static boolean INCLUDE_INITIAL = true;

    /**
     * Hold result of Map generation and analysis.
     */
    private TrackCollection trackCollection;

    /**
     * @return the trackCollection
     */
    public TrackCollection getTrackCollection() {
        return trackCollection;
    }

    public TrackMapAnalyser() {
        trackCollection = new TrackCollection(INCLUDE_INITIAL);
    }

    /**
     * Track maxima across motility map as long as they fulfil criterion of amplitude.
     * 
     * @param mapCell holds all maps generated and saved by QuimP
     * @param drop the value (in x/100) while velocity remains above of the peak speed. E.g for
     * drop=1 all tracked points are considered (along positive motility), drop=0.5 stands for 
     * points that are above 0.5*peakval, where peakval is the value of found maximum.  
     * @param maximaFinder properly initialized object that holds maxima of motility map. 
     * All maxima are tracked.
     * 
     * Return list of points tracked from every maximum point as long as they meet criterion.
     * Maximum point can be included in this list depending on setting of 
     * {@link uk.ac.warwick.wsbc.QuimP.geom.MapTracker.includeFirst} flag. First points in tracks
     * are initial points. Forward track is sorted within increasing frames from starting point,
     * backward according to decreasing frames.
     */
    public void trackMaxima(final STmap mapCell, double drop, final MaximaFinder maximaFinder) {

        int numFrames = mapCell.motMap.length;
        // int[] indexes = new int[numFrames];
        Polygon maxi = maximaFinder.getMaxima(); // restore computed maxima
        double[] maxValues = maximaFinder.getMaxValues(); // max values in order of maxi
        MapTracker trackMap = new MapTracker(mapCell.originMap, mapCell.coordMap); // build tracking
                                                                                   // map
        trackMap.includeFirst = INCLUDE_INITIAL; // include also initial point
        ArrayList<Point> tForward = null;
        ArrayList<Point> tBackward = null;
        // end indexes of accepted elements after checking criterion
        int Nb = 0;
        int Nf = 0;
        // iterate through all maxima - take only indexes (x)
        for (int i = 0; i < maxi.npoints; i++) {
            int index = maxi.ypoints[i]; // considered index
            int frame = maxi.xpoints[i]; // considered frame
            LOGGER.trace("Max = [" + frame + "," + index + "]");
            // trace forward every index until end of time
            tForward =
                    (ArrayList<Point>) trackMap.trackForwardValid(frame, index, numFrames - frame);
            // trace backward every index until end of time
            tBackward = (ArrayList<Point>) trackMap.trackBackwardValid(frame, index, frame);
            Collections.reverse(tBackward);
            // check where is drop off - index that has velocity below drop
            double dropValue = maxValues[i] - maxValues[i] * drop;
            for (Nb = 0; Nb < tBackward.size() && tBackward.get(Nb).y >= 0; Nb++) {
                double val = (mapCell.motMap[tBackward.get(Nb).x][tBackward.get(Nb).y]);
                if (val < dropValue) {
                    break;
                }
            }
            LOGGER.trace("tBackward: " + tBackward);
            LOGGER.trace("Accepted:" + Nb);

            for (Nf = 0; Nf < tForward.size() && tForward.get(Nf).y >= 0; Nf++) {
                double val = (mapCell.motMap[tForward.get(Nf).x][tForward.get(Nf).y]);
                if (val < dropValue) {
                    break;
                }
            }
            LOGGER.trace("tForward: " + tForward);
            LOGGER.trace("Accepted:" + Nf);
            // store tracking lines
            // Nb and Nf are pointer AFTER last valid point
            trackCollection.addPair(tBackward.subList(0, Nb), tForward.subList(0, Nf));
        }
    }

    /**
     * 
     * @return All common points among tracks without self crossings (forward-backward for the
     * same starting point)
     */
    public Polygon getCommonPoints() {
        ArrayList<Point> tmpRet = new ArrayList<>();
        List<Pair<Track, Track>> tracks = trackCollection.getBf();
        for (int i = 0; i < tracks.size() - 1; i++)
            for (int j = i + 1; j < tracks.size(); j++) {
                Track b1 = tracks.get(i).fst;
                Track b2 = tracks.get(j).fst;
                Track f1 = tracks.get(i).snd;
                Track f2 = tracks.get(j).snd;
                // check b1-b2, b1-f2, b2-f1, f1-f2
                // b1-b2
                {
                    Track copy = new Track(b1);
                    copy.retainAll(b2);
                    tmpRet.addAll(copy);
                }
                // b1-f2
                {
                    Track copy = new Track(b1);
                    copy.retainAll(f2);
                    tmpRet.addAll(copy);
                }
                // b2-f1
                {
                    Track copy = new Track(b2);
                    copy.retainAll(f1);
                    tmpRet.addAll(copy);
                }
                // f1-f2
                {
                    Track copy = new Track(f1);
                    copy.retainAll(f2);
                    tmpRet.addAll(copy);
                }
            }
        LOGGER.debug("Common points found:" + tmpRet.size());
        return Point2i2Polygon(QuimPArrayUtils.removeDuplicates(tmpRet));
    }

    /**
     * Find common points among polygons.
     * 
     * Check whether there are common points among polygons stored in List.
     * @param tracks List of polygons.
     * @return Polygon of size 0 when no intersection or polygons whose vertices are common for 
     * polygons in <tt>tracks</tt>. If there are vertexes shared among more than two polygons, they
     * appear only once in returned polygon.
     * <p><b>Warning</b><p>
     * Polygon of size 0 may contain x,y, arrays of size 4, only number of points is 0
     */
    public Polygon getIntersectionPoints(List<Polygon> tracks) {
        List<Polygon> tmpRet = new ArrayList<>();
        for (int i = 0; i < tracks.size() - 1; i++)
            for (int j = i + 1; j < tracks.size(); j++) {
                Polygon retPol = getIntersectionPoints(tracks.get(i), tracks.get(j));
                if (retPol.npoints != 0)
                    tmpRet.add(retPol); // add retained elements (common with p2)
            }
        // remove repeating vertexes
        List<Point> retP2i = QuimPArrayUtils.removeDuplicates(Polygon2Point2i(tmpRet));
        // convert from list of polygons to one polygon
        return Point2i2Polygon(retP2i);
    }

    /**
     * Check if p1 and p2 have common vertexes.
     * 
     * @param p1
     * @param p2
     * @return Polygon whose vertexes are those common for p1 and p2.
     */
    public Polygon getIntersectionPoints(Polygon p1, Polygon p2) {
        Polygon ret = new Polygon();
        List<Point> tmpRet = new ArrayList<>();
        List<Point> p1p = Polygon2Point2i(Arrays.asList(p1)); // polygon as list of points
        List<Point> p2p = Polygon2Point2i(Arrays.asList(p2)); // polygon as list of points
        // check if p1 and p2 have common elements
        p1p.retainAll(p2p);
        tmpRet.addAll(p1p); // add retained elements (common with p2)

        ret = Point2i2Polygon(tmpRet);
        return ret;
    }

    /**
     * Find common points among polygons. 
     * 
     * This method provides also parents of every common point. Parents are given as indexes of 
     * polygons in input list that have common vertex.
     * 
     * @param tracks List of polygons.
     * @param mode WITHOUT_SELFCROSSING | WITH_SELFCROSSING
     * @return List of common points together with their parents List<Pair<Parents,Point>>.
     * If there is no common points the list is empty
     */
    public List<Pair<Point, Point>> getIntersectionParents(List<Polygon> tracks, int mode) {
        ArrayList<Pair<Point, Point>> retTmp = new ArrayList<>();
        List<Pair<Point, Point>> ret;
        for (int i = 0; i < tracks.size() - 1; i++)
            for (int j = i + 1; j < tracks.size(); j++) {
                Polygon retPol = getIntersectionPoints(tracks.get(i), tracks.get(j));
                for (int n = 0; n < retPol.npoints; n++) {
                    Pair<Point, Point> pairTmp = new Pair<Point, Point>(new Point(i, j),
                            new Point(retPol.xpoints[n], retPol.ypoints[n]));
                    retTmp.add(pairTmp);
                }
            }
        ret = retTmp;
        if ((mode & WITHOUT_SELFCROSSING) == WITHOUT_SELFCROSSING)
            ret = removeSelfCrossings(ret);
        return ret;
    }

    public List<Pair<Point, Point>> removeSelfRepeatings(List<Pair<Point, Point>> intersections,
            List<Polygon> tracks) {
        HashMap<Integer, List<Pair<Point, Point>>> map = new HashMap<>();
        List<Pair<Point, Point>> ret = new ArrayList<>();
        // collect all intersections into separate maps according to parent (left only considered)
        for (Pair<Point, Point> p : intersections) {
            Integer parentleft = p.fst.x;
            if (map.get(parentleft) == null) // get key
                map.put(parentleft, new ArrayList<>()); // if no create
            map.get(parentleft).add(p); // add crossection point to this key
        }
        // now, there are intersection points under keys which are their left parent.
        // go through every set and check which point is first along this parent
        Iterator<Integer> it = map.keySet().iterator();
        int minInd = Integer.MAX_VALUE;
        while (it.hasNext()) {
            Integer key = it.next();
            List<Pair<Point, Point>> values = map.get(key);
            Pair<Point, Point> minPoint = null; // will newer be added to ret as it will be
                                                // Initialised or exception will be thrown
            for (Pair<Point, Point> p : values) { // iterate over intersections for given parent
                // get indexes of back and for tracks
                // This is strictly related to trackMaxima return order
                int back, forw;
                if (p.fst.x % 2 == 0) { // if index is even it is back and forward is next one
                    back = p.fst.x;
                    forw = back + 1;
                } else { // if index is uneven this is forward and back is previous
                    forw = p.fst.x;
                    back = forw - 1;
                }
                int ind = enumeratePoint(tracks.get(back), tracks.get(forw), p.snd);
                if (ind < 0)
                    throw new IllegalArgumentException("Point does not exist in track");
                if (ind < minInd) {
                    minInd = ind;
                    minPoint = p;
                }
            }
            ret.add(minPoint);
        }
        return ret;

    }

    /**
     * Remove self crossings that happen between backward and forward tracks for the same initial
     * point.
     * 
     * {@link trackMaxima} returns alternating tracks tracks, therefore every pair i,i+1 is related
     * to the same starting points, for even i. If the flag 
     * uk.ac.warwick.wsbc.QuimP.geom.TrackMap.includeFirst is set, those two tracks share one point 
     * that is also starting point.
     * <p>
     * This method remove those Pairs that come from parent <even,uneven>.
     *   
     * @param input
     * @return input list without common points between tracks that belong to the same starting
     * point.
     * @see trackMaxima(STmap, double, MaximaFinder)
     */
    private List<Pair<Point, Point>> removeSelfCrossings(List<Pair<Point, Point>> input) {
        ArrayList<Pair<Point, Point>> ret = new ArrayList<>(input);
        ListIterator<Pair<Point, Point>> it = ret.listIterator();
        while (it.hasNext()) {
            Pair<Point, Point> element = it.next();
            // remove because first parent is even and second is next track. <even,uneven> are
            // <backward,forward> according to trackMaxima.
            if (element.fst.x % 2 == 0 && element.fst.x + 1 == element.fst.y)
                it.remove();
        }
        return ret;
    }

    /**
     * Convert list of Polygons to list of Points.
     * <p>
     * The difference is that for polygons points are kept in 1d arrays, whereas for Point2i they
     * are as separate points that allows object comparison.
     *  
     * @param list List of polygons to convert
     * @return List of points constructed from all polygons.
     */
    static public List<Point> Polygon2Point2i(List<Polygon> list) {
        List<Point> ret = new ArrayList<>();
        for (Polygon pl : list) { // every polygon
            for (int i = 0; i < pl.npoints; i++) // every point in it
                ret.add(new Point(pl.xpoints[i], pl.ypoints[i]));
        }
        return ret;
    }

    /**
     * Convert list of Points to list of Polygons.
     * <p>
     * The difference is that for polygons points are kept in 1d arrays, whereas for Point2i they
     * are as separate points that allows object comparison.
     *  
     * @param list List of points to convert
     * @return Polygon constructed from all points. This is 1-element list.
     */
    static public Polygon Point2i2Polygon(List<Point> list) {
        int x[] = new int[list.size()];
        int y[] = new int[list.size()];
        int l = 0;
        for (Point p : list) { // every point
            x[l] = p.x;
            y[l] = p.y;
            l++;
        }
        return new Polygon(x, y, list.size());
    }

    /**
     * Get index of point in the whole track line composed from backward+forward tracks.
     * 
     * Assumes that order og points in tracks is correct, from first to last. (assured by 
     * {@link trackMaxima(STmap, double, MaximaFinder)}.
     * 
     * Use {@link INCLUDE_INITIAL} to check whether initial point is included in tracks. If it is
     * it means that it appears twice (for backward and forward tracks respectively). then it is 
     * counted only one. For <tt>false</tt> state all points are counted.
     * 
     * @param backwardMap
     * @param forwardMap
     * @param point
     * @return Total index of point or -1 if not found in these track maps.
     */
    static int enumeratePoint(Polygon backwardMap, Polygon forwardMap, Point point) {
        int i = 0;
        int delta = 0;
        // if maximum is included in tracks it appear there twice, for backward and forward
        // track
        if (INCLUDE_INITIAL && forwardMap.npoints > 0 && backwardMap.npoints > 0)
            delta = 1;
        // do no count last point (maximum) if it is there. It will be counted for forward track
        for (i = 0; i < backwardMap.npoints - delta; i++)
            if (backwardMap.xpoints[i] == point.x && backwardMap.ypoints[i] == point.y)
                return i;
        for (; i < forwardMap.npoints + backwardMap.npoints - delta; i++)
            if (forwardMap.xpoints[i - backwardMap.npoints + delta] == point.x
                    && forwardMap.ypoints[i - backwardMap.npoints + delta] == point.y)
                return i;
        return -1;
    }

}
