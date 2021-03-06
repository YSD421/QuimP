package com.github.celldynamics.quimp.plugin.dic;

import com.github.celldynamics.quimp.QuimpException;

/**
 * Basic class derived from Exception for purposes of DICReconstruction module.
 * 
 * @author p.baniukiewicz
 */
@SuppressWarnings("serial")
public class DicException extends QuimpException {

  /**
   * Main constructor.
   * 
   * @param arg0 Reason of exception
   */
  public DicException(final String arg0) {
    super(arg0);
  }

}
