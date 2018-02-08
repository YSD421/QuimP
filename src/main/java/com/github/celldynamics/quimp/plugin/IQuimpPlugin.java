package com.github.celldynamics.quimp.plugin;

import ij.plugin.PlugIn;

/**
 * This interface supports plugins used in QuimP Bar.
 * 
 * <p>Those plugins are stand alone instances that could be run outside the QuimP.
 * 
 * <p>This interface is for future use.
 * 
 * @author p.baniukiewicz
 *
 */
public interface IQuimpPlugin extends PlugIn {

  /**
   * Return plugin description.
   * 
   * @return Plugin description
   */
  public String about();
}
