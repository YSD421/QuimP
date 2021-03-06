/**
 * This package contains interfaces that define QuimP plugins.
 * 
 * There are two kinds of Quimp plugins:
 * <ol>
 * <li>Utilised by BOA module - those must implement
 * {@link com.github.celldynamics.quimp.plugin.snakes.IQuimpBOAPoint2dFilter} or
 * {@link com.github.celldynamics.quimp.plugin.snakes.IQuimpBOASnakeFilter}</li>
 * <li>General purpose plugins - they constitute extensible QuimP Toolbar. They implement
 * {@link com.github.celldynamics.quimp.plugin.IQuimpPlugin}</li>
 * </ol>
 * General purpose plugins differ from standard IJ plugins based only on
 * {@link ij.plugin.filter.PlugInFilter} or {@link ij.plugin.PlugIn} interfaces (those are extended
 * by {@link com.github.celldynamics.quimp.plugin.IQuimpPlugin} and
 * {@link com.github.celldynamics.quimp.plugin.IQuimpPluginFilter} respectively. General purpose
 * plugins
 * are meant to be used with QCONF file platform. If plugin works like standard IJ plugin it should
 * implement only IJ interfaces or IQuimp interfaces but it can be based on e.g.
 * {@link com.github.celldynamics.quimp.plugin.AbstractOptionsParser}.
 * 
 * General plugins can be run from IJ macro scripts thus they also implement
 * {@link ij.plugin.PlugIn} interface (via
 * {@link com.github.celldynamics.quimp.plugin.IQuimpPlugin}),
 * thus their architecture should follow IJ guidelines:
 * <ol>
 * <li>Default constructor should do nothing for running the plugin
 * <li>Other constructor are allowed, for example for testing
 * <li>The main runner is {@link com.github.celldynamics.quimp.plugin.IQuimpPlugin#run(String)}
 * method.
 * Input parameter for {@link com.github.celldynamics.quimp.plugin.IQuimpPlugin#run(String) }can be
 * used
 * for passing parameters from e.g. parameterised constructor. It is not that parameter that contain
 * options given in IJ macro scripts. It can be epmty or null.
 * <li>Plugin should be able to run with options given in IJ macro - in this case without UI and
 * with showing all messages in console.
 * </ol>
 * 
 * <p>
 * <h2>Plugin template</h2> Abstract class
 * {@link com.github.celldynamics.quimp.plugin.AbstractOptionsParser}
 * contains basic code that supports macros and calls from UI. <b>Only for plugins that interfere
 * with QCONF platform</b>
 * 
 * @author p.baniukiewicz
 */
package com.github.celldynamics.quimp.plugin;