package com.github.celldynamics.quimp.plugin.bar;

import ij.ImageJ;

/**
 * Bar displayer.
 *
 * @author p.baniukiewicz
 */
public class QuimP_BarRun {

  static {
    System.setProperty("logback.configurationFile", "quimp-logback.xml");
  }

  /**
   * Runner.
   * 
   * @param args args
   */
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    ImageJ ij = new ImageJ();
    QuimP_Bar bar = new QuimP_Bar();
    bar.run("");

  }

}
