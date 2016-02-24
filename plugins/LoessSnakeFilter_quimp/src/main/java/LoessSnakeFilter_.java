import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2d;

import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.warwick.wsbc.QuimP.plugin.ParamList;
import uk.ac.warwick.wsbc.QuimP.plugin.QuimpPluginException;
import uk.ac.warwick.wsbc.QuimP.plugin.snakes.IQuimpPoint2dFilter;
import uk.ac.warwick.wsbc.QuimP.plugin.utils.QWindowBuilder;
import uk.ac.warwick.wsbc.QuimP.plugin.utils.QuimpDataConverter;

/**
 * Interpolation of points (X,Y) by means of Loess method
 * 
 * @author p.baniukiewicz
 * @date 20 Jan 2016
 * @see William S. Cleveland - Robust Locally Weighted Regression and Smoothing
 * Scatterplots
 */
public class LoessSnakeFilter_ implements IQuimpPoint2dFilter<Vector2d> {

    private static final Logger LOGGER = LogManager
            .getLogger(LoessSnakeFilter_.class.getName());
    private QuimpDataConverter xyData; // input List converted to separate X and
                                       // Y arrays
    private double smoothing; // smoothing value (f according to references)
    private ParamList uiDefinition; // Definition of UI for this
                                    // plugin
    private QWindowBuilderInstLoess uiInstance;

    /**
     * Create Loess filter.
     * 
     * All default parameters should be declared here. Non-default are passed by
     * setPluginConfig(HashMap<String, Object>)
     */
    public LoessSnakeFilter_() {
        LOGGER.trace("Entering constructor");
        this.smoothing = 0.25;
        LOGGER.debug("Set default parameter: smoothing=" + smoothing);
        uiDefinition = new ParamList(); // will hold ui definitions
        uiDefinition.put("name", "LoessFilter"); // name of window
        uiDefinition.put("smooth",
                "spinner, 0.05, 0.5, 0.005," + Double.toString(smoothing));
        uiDefinition.put("help", "Higher values stand for more smooth output."
                + " Resonable range is 0.05 - 0.5. For too small values "
                + "plugin throws error. Minimal vale depends on "
                + "polygon shape and can vary.");
        uiInstance = new QWindowBuilderInstLoess(); // create window object,
        uiInstance.buildWindow(uiDefinition); // construct ui (not shown yet)
    }

    /**
     * Attach data to process.
     * 
     * Data are as list of vectors defining points of polygon. Passed points
     * should be sorted according to a clockwise or anti-clockwise direction
     * 
     * @param data
     * Polygon points
     * @see wsbc.plugin.snakes.IQuimpPoint2dFilter.attachData(List<E>)
     */
    @Override
    public void attachData(List<Vector2d> data) {
        LOGGER.trace("Entering attachData");
        xyData = new QuimpDataConverter(data);
    }

    /**
     * Run interpolation on X,Y vectors using LoessInterpolator
     * 
     * @return Filtered points as list of Vector2d objects
     * @throws QuimpPluginException when: - smoothing value is too small
     * (usually below 0.015 but
     * it depends on data)
     */
    @Override
    public List<Vector2d> runPlugin() throws QuimpPluginException {
        // collect actual parameters from UI
        smoothing = uiInstance.getDoubleFromUI("smooth");
        LOGGER.debug(String.format("Run plugin with params: smoothing %f",
                smoothing));

        double density = 1.0f; // If smaller than 1 output points will be
                               // refined. For 1 numbers of output points and
                               // input points are equal.
        LoessInterpolator sI;
        double[] i = new double[xyData.size()]; // table of linear indexes that
                                                // stand for x values for X,Y
                                                // vectors (treated separately
                                                // now)
        List<Vector2d> out = new ArrayList<Vector2d>(); // output interpolated
                                                        // data
        PolynomialSplineFunction psfX; // result of
                                       // LoessInterpolator.interpolate for X
                                       // and Y data
        PolynomialSplineFunction psfY;
        for (int ii = 0; ii < xyData.size(); ii++)
            i[ii] = ii; // create linear indexes for X and Y
        try {
            sI = new LoessInterpolator(smoothing, // f 0.03-0.1
                    1, // W
                    1.0E-15);
            psfX = sI.interpolate(i, xyData.getX()); // interpolation of X
            psfY = sI.interpolate(i, xyData.getY()); // interpolation of Y
        } catch (NumberIsTooSmallException e) {
            // change for checked exception and add cause
            throw new QuimpPluginException("Smoothing value is too small", e);
        }
        // copy to Vector2d List
        for (double ii = 0; ii <= xyData.size() - 1; ii += density) {
            out.add(new Vector2d(psfX.value(ii), psfY.value(ii)));
        }
        return out;
    }

    /**
     * This method should return a flag word that specifies the filters
     * capabilities.
     * 
     * @return Configuration codes
     * @see uk.ac.warwick.wsbc.QuimP.plugin.IQuimpPlugin
     * @see uk.ac.warwick.wsbc.plugin.IQuimpPlugin.setup()
     */
    @Override
    public int setup() {
        LOGGER.trace("Entering setup");
        return DOES_SNAKES;
    }

    /**
     * Configure plugin and overrides default values.
     * 
     * It is called by plugin user to pass configuration to plugin.
     * 
     * Supported keys: 
     * -# \c smoothing - smoothing value of filter
     * 
     * @param par configuration as pairs <key,val>. Keys are defined by plugin
     * creator and plugin user do not modify them.
     * @throws QuimpPluginException on wrong parameters list or wrong parameter
     * conversion
     * @see wsbc.plugin.IQuimpPlugin.setPluginConfig(final ParamList)
     */
    @Override
    public void setPluginConfig(final ParamList par)
            throws QuimpPluginException {
        try {
            smoothing = par.getDoubleValue("smooth");
            uiInstance.setValues(par);
        } catch (Exception e) {
            // we should never hit this exception as parameters are not touched
            // by caller
            // they are only passed to configuration saver and restored from it
            throw new QuimpPluginException(
                    "Wrong input argument->" + e.getMessage(), e);
        }
    }

    /**
     * Transfer plugin configuration to QuimP
     * 
     * Only parameters mapped to UI by QWindowBuilder are supported directly by
     * getValues() Any other parameters created outside QWindowBuilder should be
     * added here manually.
     */
    @Override
    public ParamList getPluginConfig() {
        return uiInstance.getValues();
    }

    @Override
    public void showUI(boolean val) {
        LOGGER.debug("Got message to show UI");
        uiInstance.toggleWindow();
    }

    @Override
    public String getVersion() {
        return null;
    }
}

/**
 * Instance private class for tested QWindowBuilder
 * 
 * Any overrides of UI methods can be done here. For example user can attach
 * listener to ui object.
 * 
 * @author p.baniukiewicz
 * @date 5 Feb 2016
 *
 */
class QWindowBuilderInstLoess extends QWindowBuilder {
}
