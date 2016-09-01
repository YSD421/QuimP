/**
 * @file ProtrusionVisTest.java
 * @date 19 Aug 2016
 */
package uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Polygon;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Point2i;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import uk.ac.warwick.wsbc.QuimP.STmap;

/**
 * @author p.baniukiewicz
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ProtrusionVisTest {
    static {
        if (System.getProperty("quimp.debugLevel") == null)
            Configurator.initialize(null, "log4j2_default.xml");
        else
            Configurator.initialize(null, System.getProperty("quimp.debugLevel"));
    }

    static Object accessPrivate(String name, ProtrusionVis obj, Object[] param,
            Class<?>[] paramtype) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method prv = obj.getClass().getDeclaredMethod(name, paramtype);
        prv.setAccessible(true);
        return prv.invoke(obj, param);
    }

    private static final Logger LOGGER = LogManager.getLogger(ProtrusionVisTest.class.getName());
    private ImagePlus originalImage;
    // http://stackoverflow.com/questions/16467685/difference-between-mock-and-injectmocks
    // @InjectMocks
    private ProtrusionVis protrusionVis;

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
        MockitoAnnotations.initMocks(this); // must be as we create mocked classes in mocked already
                                            // QParams
        originalImage = IJ.openImage(
                "/home/baniuk/Documents/Kay-copy/KZ4/KZ4-220214-cAR1-GFP-devel5.5h-agar07-14.tif");
        protrusionVis = new ProtrusionVis(originalImage);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.ProtrusionVis#ProtrusionVis(uk.ac.warwick.wsbc.QuimP.QParams, uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.MaximaFinder)}.
     */
    @Test
    public void testProtrusionVisQParamsMaximaFinder() throws Exception {

    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.ProtrusionVis#addPointsToImage()}.
     */
    @SuppressWarnings("unused")
    @Test
    public void testAddPointsToImage() throws Exception {
        ImageJ ij = new ImageJ();
        int[] frames = { 0, 1, 2, 3, 4, 5, 6 };
        int[] indexes = { 0, 0, 0, 0, 0, 0, 0 };
        double[][] xs = { { 10 }, { 50 }, { 100 }, { 150 }, { 200 }, { 300 }, { 400 } };
        double[][] ys = { { 50 }, { 60 }, { 160 }, { 210 }, { 360 }, { 460 }, { 510 } };
        STmap mapCell = Mockito.mock(STmap.class);
        MaximaFinder mF = Mockito.mock(MaximaFinder.class);
        Mockito.when(mapCell.getxMap()).thenReturn(xs);
        Mockito.when(mapCell.getyMap()).thenReturn(ys);

        Mockito.when(mF.getMaxima()).thenReturn(new Polygon(indexes, frames, 7));

        protrusionVis.addMaximaToImage(mapCell, mF);

        protrusionVis.getOriginalImage().setTitle("testAddPointsToImage");
        // protrusionVis.getOriginalImage().show();
        // while (protrusionVis.getOriginalImage().isVisible()) {
        // Thread.sleep(1500);
        // }
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.ProtrusionVis#PolygonRoi2Map(java.util.List)}.
     */
    @Test
    public void testPolygonRoi2Map() throws Exception {
        List<Point2i> expected = new ArrayList<>();
        expected.add(new Point2i(1, 11));
        expected.add(new Point2i(1, 44));
        int[] x1 = { 1, 2, 3, 1 };
        int[] y1 = { 11, 22, 33, 44 };
        int[] x2 = { 101, 102, 103 };
        int[] y2 = { 111, 112, 113 };

        ArrayList<PolygonRoi> p = new ArrayList<>();
        p.add(new PolygonRoi(x1, y1, x1.length, Roi.FREELINE));
        p.add(new PolygonRoi(x2, y2, x2.length, Roi.FREELINE));

        @SuppressWarnings("unchecked")
        List<Point2i> ret = (List<Point2i>) accessPrivate("PolygonRoi2Point2i", protrusionVis,
                new Object[] { p }, new Class<?>[] { List.class });
        List<Point2i> result = ret.stream().filter(e -> e.getX() == 1).collect(Collectors.toList());
        assertThat(result, is(expected));

    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.plugin.Prot_Analysis.ProtrusionVis#addTrackingLinesToImage(uk.ac.warwick.wsbc.QuimP.STmap, java.util.List)}.
     */
    @Test
    public void testAddTrackingLinesToImage() throws Exception {
        ArrayList<PolygonRoi> testRoi = new ArrayList<>();
        int[] frames = { 2, 3, 4, 5, 6, 7, 8 };
        int[] indexes = { 0, 0, 0, 0, 0, 0, 0 };
        double[][] xs = { { 0 }, { 0 }, { 10 }, { 11 }, { 12 }, { 13 }, { 14 }, { 15 }, { 16 } };
        double[][] ys = { { 0 }, { 0 }, { 50 }, { 51 }, { 52 }, { 53 }, { 54 }, { 55 }, { 56 } };

        STmap mapCell = Mockito.mock(STmap.class);
        Mockito.when(mapCell.getxMap()).thenReturn(xs);
        Mockito.when(mapCell.getyMap()).thenReturn(ys);

        testRoi.add(new PolygonRoi(indexes, frames, 7, Roi.POLYLINE));
        LOGGER.trace(Arrays.toString(testRoi.get(0).getPolygon().ypoints));
        protrusionVis.addTrackingLinesToImage(mapCell, testRoi);

        protrusionVis.getOriginalImage().setTitle("testAddTrackingLinesToImage");
        // protrusionVis.getOriginalImage().show();
        // while (protrusionVis.getOriginalImage().isVisible()) {
        // Thread.sleep(1500);
        // }
    }

    @Test
    public void testListPoint2iComparator() {
        List<Point2i> expected = new ArrayList<>();
        expected.add(new Point2i(1, 1));
        expected.add(new Point2i(1, 11));
        expected.add(new Point2i(5, 44));

        List<Point2i> result = new ArrayList<>();
        result.add(new Point2i(1, 11));
        result.add(new Point2i(5, 44));
        result.add(new Point2i(1, 1));

        Collections.sort(result, new ListPoint2iComparator());
        assertThat(result, is(expected));
    }

}
