/**
 */
package uk.ac.warwick.wsbc.QuimP.plugin.randomwalk;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import uk.ac.warwick.wsbc.QuimP.Outline;
import uk.ac.warwick.wsbc.QuimP.geom.OutlineProcessor;
import uk.ac.warwick.wsbc.QuimP.geom.TrackOutline;
import uk.ac.warwick.wsbc.QuimP.plugin.ana.ANAp;
import uk.ac.warwick.wsbc.QuimP.utils.IJTools;
import uk.ac.warwick.wsbc.QuimP.utils.Pair;

/**
 * Generate new seeds for n+1 frame in stack using previous results of segmentation.
 * 
 * This class supports two methods:
 * <ol>
 * <li>Based on morphological operations
 * <li>Based on contour shrinking (part of QuimP Outline framework)
 * </ol>
 * 
 * In both cases the aim is to shrink the object (which is white) to prevent overlapping foreground
 * and background in next frame (assuming that objects are moving). The same is for background.
 * Finally, the new seed should have set foreground pixels to area inside the object and background
 * pixels in remaining part of image. There should be unseeded strip of pixels around the object.
 * 
 * @author p.baniukiewicz
 *
 */
public abstract class PropagateSeeds {
    final static int ERODE = 0;
    final static int DILATE = 1;
    /**
     * Default resolution used during outlining objects.
     * 
     * @see Contour#getOutline(ImageProcessor)
     */
    public static int STEPS = 4;
    /**
     * By default seed history is not stored.
     */
    protected boolean storeSeeds = false;
    /**
     * Container for <FG and BG> seeds pixels used for seed visualisation.
     * 
     * Every imageProcessor in pair contains important bits set to WHITE. For example BG pixels are
     * white here as well as FG pixels.
     * 
     * @see #getCompositeSeed(ImagePlus)
     * @see PropagateSeeds#storeSeeds
     */
    protected List<Pair<ImageProcessor, ImageProcessor>> seeds;
    /**
     * Scale color values in composite preview.
     * 
     * 1.0 stand for opaque colors.
     * 
     * @see #getCompositeSeed(ImagePlus)
     */
    public static double colorScaling = 0.5;

    /**
     * Contain methods for propagating seeds to the next frame using contour shrinking operations.
     * 
     * @author p.baniukiewicz
     *
     */
    public static class Contour extends PropagateSeeds {

        /**
         * Step size during object outline shrinking.
         * 
         * @see OutlineProcessor#shrink(double, double, double, double)
         * @see ANAp
         */
        public static double stepSize = 0.04;

        /**
         * Default constructor without storing seed history.
         */
        public Contour() {
            this(false);
        }

        /**
         * Allow to store seed history that can be later presented in form of composite image.
         * 
         * @param storeSeeds <tt>true</tt> to store seeds.
         * @see getCompositeSeed(ImagePlus)
         */
        public Contour(boolean storeSeeds) {
            this.storeSeeds = storeSeeds;
            if (storeSeeds)
                seeds = new ArrayList<>();
        }

        /**
         * Generate seeds for next frame using provided mask.
         * 
         * The mask provided to this method is shrunk to get new seeds of object (that can move
         * meanwhile). The same mask is expanded and subtracted from image forming the background.
         * 
         * @param previous Previous result of segmentation. BW mask with white object on black
         *        background.
         * @param shrinkPower Shrink size for objects in pixels.
         * @param expandPower Expand size used to generate background (object is expanded and then
         *        subtracted from background)
         * @return List of background and foreground coordinates.
         * @see PropagateSeeds.Morphological#propagateSeed(ImageProcessor, int)
         * @see OutlineProcessor#shrink(double, double, double, double)
         */
        @Override
        public Map<Integer, List<Point>> propagateSeed(ImageProcessor previous, double shrinkPower,
                double expandPower) {
            ByteProcessor small = new ByteProcessor(previous.getWidth(), previous.getHeight());
            ByteProcessor big = new ByteProcessor(previous.getWidth(), previous.getHeight());
            small.setColor(Color.BLACK);
            small.fill();
            big.setColor(Color.BLACK);
            big.fill();
            double stepsshrink = shrinkPower / stepSize; // total shrink/step size
            double stepsexp = (expandPower) / stepSize; // total shrink/step size

            List<Outline> outlines = getOutline(previous);
            for (Outline o : outlines) {
                // shrink outline - copy as we want to expand it later
                Outline copy = new Outline(o);
                new OutlineProcessor(copy).shrink(stepsshrink, 0.04, 0.1, 1); // taken from anap
                copy.unfreezeAll();
                Roi fr = copy.asFloatRoi();
                fr.setFillColor(Color.WHITE);
                fr.setStrokeColor(Color.WHITE);
                small.drawRoi(fr);
            }

            for (Outline o : outlines) {
                // shrink outline - copy as we want to expand it later
                new OutlineProcessor(o).shrink(stepsexp, -0.04, 0.1, 1); // taken from anap
                o.unfreezeAll();
                Roi fr = o.asFloatRoi();
                fr.setFillColor(Color.WHITE);
                fr.setStrokeColor(Color.WHITE);
                big.drawRoi(fr);
            }
            big.invert();
            if (storeSeeds)
                seeds.add(Pair.createPair(small, big));

            return convertToList(small, big);

        }

        /**
         * Convert mask to outline.
         * 
         * @param previous image to outline. White object on black background.
         * @return List of Outline for current frame
         * @see TrackOutline
         */
        private List<Outline> getOutline(ImageProcessor previous) {
            TrackOutline tO = new TrackOutline(previous, 0);
            return tO.getOutlines(STEPS, false);
        }

    }

    /**
     * Contain methods for propagating seeds to next frame using morphological operations.
     * 
     * @author p.baniukiewicz
     *
     */
    public static class Morphological extends PropagateSeeds {

        /**
         * Default constructor without storing seed history.
         */
        public Morphological() {
            this(false);
        }

        /**
         * Allow to store seed history that can be later presented in form of composite image.
         * 
         * @param storeSeeds <tt>true</tt> to store seeds.
         * @see getCompositeSeed(ImagePlus)
         */
        public Morphological(boolean storeSeeds) {
            this.storeSeeds = storeSeeds;
            if (storeSeeds)
                seeds = new ArrayList<>();
        }

        /**
         * Generate new seeds using segmented image.
         * 
         * @param previous segmented image, background on \b zero
         * @param shrinkPower number of erode iterations
         * @param expandPower number of dilate iterations
         * 
         * @return Map containing list of coordinates that belong to foreground and background. Map
         *         is addressed by two enums: <tt>FOREGROUND</tt> and <tt>BACKGROUND</tt>
         * @see RandomWalkSegmentation#decodeSeeds(ImagePlus, Color, Color)
         * @see #convertToList(BinaryProcessor, BinaryProcessor)
         */
        @Override
        public Map<Integer, List<Point>> propagateSeed(ImageProcessor previous, double shrinkPower,
                double expandPower) {
            BinaryProcessor cp = new BinaryProcessor(previous.duplicate().convertToByteProcessor());
            // object smaller than on frame n
            BinaryProcessor small = new BinaryProcessor(cp.duplicate().convertToByteProcessor());
            // object bigger than on frame n
            BinaryProcessor big = new BinaryProcessor(cp.duplicate().convertToByteProcessor());
            // make objects smaller
            iterateMorphological(small, PropagateSeeds.ERODE, shrinkPower);
            // make background bigger
            iterateMorphological(big, PropagateSeeds.DILATE, (int) (expandPower));

            // apply big to old background making object bigger and prevent covering objects on
            // frame
            // n+1
            // by previous background (make "empty" not seeded space around objects)
            // IJ.saveAsTiff(new ImagePlus("", big), "/tmp/testIterateMorphological_bigbef.tif");
            // IJ.saveAsTiff(new ImagePlus("", cp), "/tmp/testIterateMorphological_cp.tif");
            for (int x = 0; x < cp.getWidth(); x++)
                for (int y = 0; y < cp.getHeight(); y++) {
                    big.putPixel(x, y, big.getPixel(x, y) | cp.getPixel(x, y));
                }

            // IJ.saveAsTiff(new ImagePlus("", big), "/tmp/testIterateMorphological_big.tif");

            big.invert(); // invert to have BG pixels white in seed. (required by convertToList)
            if (storeSeeds) {
                seeds.add(Pair.createPair(small, big));
            }

            return convertToList(small, big);
        }

        private void iterateMorphological(BinaryProcessor ip, int oper, double iter) {
            switch (oper) {
                case ERODE:
                    for (int i = 0; i < iter; i++)
                        ip.erode(1, 0); // first param influence precision, for large ,the shape is
                                        // preserved and changes are very small?
                    break;
                case DILATE:
                    for (int i = 0; i < iter; i++)
                        ip.dilate(1, 0);
                    break;
                default:
                    throw new IllegalArgumentException("Binary operation not supported");
            }
        }
    }

    /**
     * Convert processors obtained for object and background to format accepted by RW.
     * 
     * @param small object mask
     * @param big background mask
     * @return List of point coordinates accepted by RW algorithm.
     */
    Map<Integer, List<Point>> convertToList(ImageProcessor small, ImageProcessor big) {
        // output map integrating two lists of points
        HashMap<Integer, List<Point>> out = new HashMap<Integer, List<Point>>();
        // output lists of points. Can be null if points not found
        List<Point> foreground = new ArrayList<>();
        List<Point> background = new ArrayList<>();
        for (int x = 0; x < small.getWidth(); x++)
            for (int y = 0; y < small.getHeight(); y++) {
                if (small.get(x, y) > 0) // WARN Why must be y,x??
                    foreground.add(new Point(y, x)); // remember foreground coords
                if (big.get(x, y) > 0)
                    background.add(new Point(y, x)); // remember background coords
            }
        // pack outputs into map
        out.put(RandomWalkSegmentation.FOREGROUND, foreground);
        out.put(RandomWalkSegmentation.BACKGROUND, background);
        return out;
    }

    /**
     * Produce composite image containing seeds generated during segmentation of particular frames.
     * 
     * To have this method working, the Contour object must be created with storeSeeds==true.
     * 
     * @param org Original image (or stack) where composite layer will be added to.
     * @return Composite image with marked foreground and background.
     */
    public ImagePlus getCompositeSeed(ImagePlus org) {
        ImagePlus ret;
        if (seeds == null)
            throw new IllegalArgumentException("Seeds were not stored.");
        int f = seeds.size();
        if (f == 0)
            throw new IllegalArgumentException("Seeds were not stored.");
        ImageStack smallstack =
                new ImageStack(seeds.get(0).first.getWidth(), seeds.get(0).first.getHeight());
        ImageStack bigstack =
                new ImageStack(seeds.get(0).first.getWidth(), seeds.get(0).first.getHeight());
        for (Pair<ImageProcessor, ImageProcessor> p : seeds) {
            // just in case convert to byte
            ImageProcessor fg = (ImageProcessor) p.first.convertToByte(true);
            ImageProcessor bg = (ImageProcessor) p.second.convertToByte(true);
            // make colors transparent
            bg.multiply(colorScaling);
            fg.multiply(colorScaling);
            // set gray lut just in case
            fg.setLut(IJTools.getGrayLut());
            bg.setLut(IJTools.getGrayLut());
            smallstack.addSlice((ImageProcessor) fg);
            bigstack.addSlice((ImageProcessor) bg);
        }
        // check if stack or not. getComposite requires the same type
        if (org.getStack().getSize() == 1)
            ret = IJTools.getComposite(org.duplicate(),
                    new ImagePlus("", smallstack.getProcessor(1)),
                    new ImagePlus("", bigstack.getProcessor(1)));
        else
            ret = IJTools.getComposite(org.duplicate(), new ImagePlus("", smallstack),
                    new ImagePlus("", bigstack));
        return ret;
    }

    abstract Map<Integer, List<Point>> propagateSeed(ImageProcessor previous, double shrinkPower,
            double expandPower);

}
