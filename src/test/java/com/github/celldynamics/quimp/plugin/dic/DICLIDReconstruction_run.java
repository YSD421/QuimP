package com.github.celldynamics.quimp.plugin.dic;

import com.github.celldynamics.quimp.plugin.dic.DICLIDReconstruction_;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

// TODO: Auto-generated Javadoc
/**
 * Gui checker for DICLIDReconstruction.
 */
public class DICLIDReconstruction_run {

  // http://stackoverflow.com/questions/21083834/load-log4j2-configuration-file-programmatically
  static {
    System.setProperty("logback.configurationFile", "quimp-logback.xml");
  }

  /**
   * @param args
   * @throws InterruptedException Gui checker for DICLIDReconstruction
   */
  @SuppressWarnings("unused")
  public static void main(String[] args) throws InterruptedException {
    ImageJ ij = new ImageJ();
    // load images #272
    ImagePlus i = IJ.openImage("src/test/Resources-static/C2-bleb_Image4.tif");
    i.show();
    DICLIDReconstruction_ dic = new DICLIDReconstruction_();
    dic.setup("", i);
    dic.run(i.getProcessor());
  }
}