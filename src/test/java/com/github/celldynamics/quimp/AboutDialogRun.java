package com.github.celldynamics.quimp;

import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Runner for AboutDialog.
 * 
 * @author p.baniukiewicz
 *
 */
public class AboutDialogRun {

  static {
    System.setProperty("log4j.configurationFile", "qlog4j2.xml");
  }

  /**
   * Main runner.
   * 
   * @param args args
   */
  public static void main(String[] args) {
    Frame window = new Frame("Base");
    window.add(new Panel());
    window.setSize(500, 500);
    window.setVisible(true);
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent we) {
        window.dispose();
      }
    });
    AboutDialog ad = new AboutDialog(window); // create about dialog with parent 'window'
    ad.appendLine("Hello");
    ad.appendLine("ff");
    ad.appendDistance();
    ad.aboutWnd.setVisible(true);

  }

}