/**
 * <h1>About</h1>
 * 
 * This plugin performs ANA analysis.
 * 
 * <h2>Prerequisites</h2>
 * 
 * <ol>
 * <li>Image to analyse - opened in IJ
 * <li>QCONF file or paQP file
 * </ol>
 * 
 * <h2>Macro support</h2>
 * 
 * Macro is supported.
 * 
 * <h3>Parameters</h3>
 * 
 * See Macro Recorder, note plugin should be run from menu but not from Toolbar. See
 * {@link com.github.celldynamics.quimp.plugin.ana.AnaOptions}
 * 
 * <h2>API support</h2>
 * 
 * API calls are supported, see tests.
 * 
 * <h1>Remarks</h1>
 * Fluorescence statistics are computed by
 * {@link com.github.celldynamics.quimp.plugin.ana.ANA_#setFluoStats} method using inner and outer
 * contours. Outer contour is the result of the segmentation. Inner contour is the outer one shrank
 * by given distance. ImageJ method
 * {@link ij.process.ImageStatistics#getStatistics(ij.process.ImageProcessor, int, ij.measure.Calibration)}
 * is used for computing intensity statistics, saved later in <tt>stQP.csv</tt> output file. Inner
 * and outer contours are used to set ROIs within statistics are computed.
 * 
 * Check {@link com.github.celldynamics.quimp.plugin.ana.ChannelStat} for details on computed
 * statistics.
 * Moreover, some parameters of Outline are filled by ANA:
 * {@link com.github.celldynamics.quimp.Outline}
 * is supplemented with intensity data stored at
 * {@link com.github.celldynamics.quimp.Vert#fluores}. Depending on UI settings
 * <tt>anap.sampleAtSame</tt>, intensity is sampled for coordinates from other channel or for inner
 * contour nodes resulting from ECMM mapping between outer contour and inner contour. For the first
 * case fluorescence is computed as mean of 9-point stencil located at {x,y} position (taken from
 * other channel). For the second case fluorescence data are computed by ECMM
 * {@link com.github.celldynamics.quimp.plugin.ecmm.ECMM_Mapping#runByANA(com.github.celldynamics.quimp.OutlineHandler, ij.process.ImageProcessor, double)}
 * by <tt>ODEsolver#euler</tt> from {@link com.github.celldynamics.quimp.plugin.ecmm} package. As
 * previously 9-point stencil is used. (see
 * {@link com.github.celldynamics.quimp.plugin.ecmm.ODEsolver#sampleFluo(ij.process.ImageProcessor, int, int)}
 * 
 * @author r.tyson
 * @author p.baniukiewicz
 *
 */
package com.github.celldynamics.quimp.plugin.ana;
