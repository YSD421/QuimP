package com.github.celldynamics.quimp.filesystem.converter;

/**
 * The Class FormatConverterUiRun.
 *
 * @author p.baniukiewicz
 */
public class FormatConverterUiRun {
  static {
    // disable to redirect all above INFO to internal console
    // System.setProperty("logback.configurationFile", "quimp-logback.xml");
    System.setProperty("quimpconfig.superDebug", "false");
  }

  /**
   * Runner.
   * 
   * @param args args
   * @throws Exception Exception
   */
  public static void main(String[] args) throws Exception {
    // try {
    // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    // } catch (ClassNotFoundException e) {
    // e.printStackTrace();
    // } catch (InstantiationException e) {
    // e.printStackTrace();
    // } catch (IllegalAccessException e) {
    // e.printStackTrace();
    // } catch (UnsupportedLookAndFeelException e) {
    // e.printStackTrace();
    // }
    FormatConverterController obj = new FormatConverterController();
    // obj.run("{status:[]," + "areMultipleFiles:true,"
    // + "paramFile:(/home/baniuk/Desktop/Tests/17.10/test.QCONF)}");
    obj.run("");
  }

}
