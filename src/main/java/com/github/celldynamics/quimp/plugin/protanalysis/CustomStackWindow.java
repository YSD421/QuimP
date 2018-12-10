package com.github.celldynamics.quimp.plugin.protanalysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.Outline;
import com.github.celldynamics.quimp.OutlineHandler;
import com.github.celldynamics.quimp.QuimpException;
import com.github.celldynamics.quimp.Vert;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.StackWindow;

/**
 * Implement Prot Analysis UI based on IJ StackWindow.
 * 
 * @author baniu
 *
 */
@SuppressWarnings("serial")
class CustomStackWindow extends StackWindow {
  private Prot_Analysis model; // main model with method to run on ui action
  private Color overlayColor = Color.GREEN; // outline color

  private ArrayList<OutlineHandler> handlers; // extracted from loaded QCONF

  private Component cmp;
  private Overlay overlay;
  private ImagePlus imp;
  JLabel pointsSelected = new JLabel("");

  /**
   * Construct the window.
   * 
   * @param model application logic module (with options)
   * @param imp ImagePlus image to be displayed.
   */
  public CustomStackWindow(Prot_Analysis model, final ImagePlus imp) {
    super(imp, new CustomCanvas(imp, model));
    this.model = model;
    try {
      handlers = model.getQconfLoader().getEcmm().oHs;
    } catch (QuimpException e) {
      // we should never be here as ecmm is validated on load
      throw new RuntimeException("ECMM can not be obtained");
    }
    cmp = this.getComponent(1);
    remove(cmp); // FIXME Protect against single image
    this.imp = imp;
    buildWindow();
  }

  void updateStaticFields() {
    pointsSelected.setText(Integer.toString(model.selected.size()));
  }

  /**
   * Build the window.
   */
  public void buildWindow() {
    ProtAnalysisOptions options = (ProtAnalysisOptions) model.getOptions();
    setLayout(new BorderLayout(10, 10));
    add(ic, BorderLayout.CENTER); // IJ image
    // panel for slidebar to make it more separated from window edges
    JPanel cmpP = new JPanel();
    cmpP.setLayout(new GridLayout());
    cmpP.add(cmp);
    cmpP.setBorder(new EmptyBorder(5, 5, 10, 10));
    add(cmpP, BorderLayout.SOUTH); // slidebar
    // right panel
    Panel right = new Panel();
    final int rightWidth = 120; // width of the right panel
    right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
    // point selection panel
    {
      JPanel selectPointsPanel = new JPanel();
      final int selectPointsPanelHeight = 160;
      selectPointsPanel.setLayout(new BoxLayout(selectPointsPanel, BoxLayout.PAGE_AXIS));
      selectPointsPanel.setBorder(BorderFactory.createTitledBorder("Select points"));
      {
        JTextArea help = new JTextArea("Select points with CTRL key");
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setAlignmentX(Component.RIGHT_ALIGNMENT);
        selectPointsPanel.add(help);
      }
      {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel selected = new JLabel("Selected: ");
        textPanel.add(selected);
        textPanel.add(pointsSelected);
        textPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        textPanel.setMaximumSize(new Dimension(rightWidth, textPanel.getMaximumSize().height));
        selectPointsPanel.add(textPanel);
      }
      {
        JButton bnClear = new JButton();
        bnClear.setAction(new ActionClearPoints("Remove all", "Remove all selected points", this));
        bnClear.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bnClear.setMaximumSize(new Dimension(rightWidth, bnClear.getMaximumSize().height));
        selectPointsPanel.add(bnClear);
      }
      {
        JButton bnTrack = new JButton();
        bnTrack.setAction(new ActionStaticTrackPoints("Track st", "Track points", this));
        bnTrack.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bnTrack.setMaximumSize(new Dimension(rightWidth, bnTrack.getMaximumSize().height));
        selectPointsPanel.add(bnTrack);
      }
      {
        JButton bnTrack = new JButton();
        bnTrack.setAction(new ActionDynamicTrackPoints("Track dyn", "Track points", this));
        bnTrack.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bnTrack.setMaximumSize(new Dimension(rightWidth, bnTrack.getMaximumSize().height));
        selectPointsPanel.add(bnTrack);
      }
      selectPointsPanel.setMaximumSize(new Dimension(rightWidth, selectPointsPanelHeight));
      right.add(selectPointsPanel);
    }

    // clear panel
    {
      JPanel clearPanel = new JPanel();
      clearPanel.setLayout(new BoxLayout(clearPanel, BoxLayout.PAGE_AXIS));
      clearPanel.setBorder(BorderFactory.createTitledBorder("Clear image"));
      {
        JButton bnClearOvelay = new JButton();
        bnClearOvelay.setAction(new ActionClearOverlay("Clear", "Clear Overlay", this));
        bnClearOvelay.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bnClearOvelay
                .setMaximumSize(new Dimension(rightWidth, bnClearOvelay.getMaximumSize().height));
        clearPanel.add(bnClearOvelay);
      }
      right.add(clearPanel);
    }
    // Options panel
    {
      JPanel optionsPanel = new JPanel();
      optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
      optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
      {
        JCheckBox chbNewImage = new JCheckBox();
        chbNewImage.setAction(
                new ActionNewImage("New image", "Always open new image with tracks", this));
        chbNewImage.setSelected(options.guiNewImage);
        chbNewImage.setAlignmentX(Component.RIGHT_ALIGNMENT);
        chbNewImage.setMaximumSize(new Dimension(rightWidth, chbNewImage.getMaximumSize().height));
        optionsPanel.add(chbNewImage);
      }
      right.add(optionsPanel);
    }
    // 2D plot panel
    {
      JPanel plot2dPanel = new JPanel();
      plot2dPanel.setLayout(new BoxLayout(plot2dPanel, BoxLayout.PAGE_AXIS));
      plot2dPanel.setBorder(BorderFactory.createTitledBorder("Plots"));
      {
        JButton bnPlot2d = new JButton();
        bnPlot2d.setAction(new ActionPlot2d("Plot", "Plot selected", this));
        bnPlot2d.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bnPlot2d.setMaximumSize(new Dimension(rightWidth, bnPlot2d.getMaximumSize().height));
        plot2dPanel.add(bnPlot2d);
      }
      right.add(plot2dPanel);

    }

    add(right, BorderLayout.EAST);
    pack();
    updateStaticFields();
    // this.setSize(600, 600);
    imp.setSlice(1);
    updateOverlay(1);
    setVisible(false); // to allow use showUI
  }

  /*
   * On each slice change.
   * 
   * Actions performed:
   * - Clear outlines array (keep outlines only for current frame)
   * - Clear selected points
   * - Update overlay for new frame
   * TODO Comply with new image checkbox
   */
  @Override
  public void updateSliceSelector() {
    super.updateSliceSelector();
    model.currentFrame = imp.getCurrentSlice() - 1;
    new ActionClearPoints(this).clear();
    model.outlines.clear(); // remove old outlines for old frame
    updateOverlay(model.currentFrame + 1);

  }

  /**
   * Plot overlay (outline) at frame.
   * 
   * @param frame to plot in (1-based)
   */
  public void updateOverlay(int frame) {
    overlay = new Overlay();
    for (OutlineHandler oh : handlers) {
      if (oh.isOutlineAt(frame)) {
        Outline outline = oh.getStoredOutline(frame);
        model.outlines.add(outline); // remember outline for proximity calculations
        Roi r = outline.asFloatRoi();
        r.setStrokeColor(overlayColor);
        overlay.add(r);
      }
    }
    imp.setOverlay(overlay);
  }

  /**
   * Show UI.
   * 
   * @param val true or false to show or hide UI
   */
  public void showUI(boolean val) {
    setVisible(val);
  }

  /**
   * Get model.
   * 
   * @return Reference to application model class
   */
  Prot_Analysis getModel() {
    return model;
  }
}

/**
 * Handle mouse events.
 * 
 * @author baniu
 *
 */
@SuppressWarnings("serial")
class CustomCanvas extends ImageCanvas {
  static final Logger LOGGER = LoggerFactory.getLogger(CustomCanvas.class.getName());
  // closest point on outline to mouse position (image coordinates) + index of outline
  PointCoords pc = null;
  private Prot_Analysis model; // main model with method to run on ui action
  private int sensitivity = 10; // square of distance
  private Color pointColor = Color.CYAN; // box color
  private Color staticPointColor = Color.YELLOW; // box color
  private int pointSize = 10; // box size

  public CustomCanvas(ImagePlus imp, Prot_Analysis model) {
    super(imp);
    this.model = model;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ij.gui.ImageCanvas#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(MouseEvent e) {
    // action - select outline point if CTRL is pressed and LMB. In this mode IJ handlers are
    // suppressed. Second click on the point will remove it
    if (SwingUtilities.isLeftMouseButton(e)
            && ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)) {
      if (pc != null) {
        if (model.selected.add(pc) == false) { // already exists
          model.selected.remove(pc); // so remove
        }
        model.getGui().updateStaticFields();
      }
    } else {
      super.mousePressed(e);
    }
  }

  /**
   * Find closes point between outlines for current frame (outlines) and mouse position.
   * 
   * @param current current mouse position in the image coordinates
   * @param dist max distance
   * @return found point that belongs to outline (image coordinates, frame) and outline index in
   *         {@link Prot_Analysis#outlines}
   */
  private PointCoords checkProximity(Point current, double dist) {
    // Point current = new Point(screenXD(currentt.getX()), screenYD(currentt.getY()));
    ListIterator<Outline> it = model.outlines.listIterator();
    while (it.hasNext()) {
      Integer io = it.nextIndex(); // order!
      Outline o = it.next();
      Rectangle2D.Double bounds = o.getDoubleBounds(); // FIXME cache
      if (bounds.contains(current)) { // investigate deeper
        for (Vert v : o) { // over vertices
          if (current.distanceSq(v.getX(), v.getY()) < dist) {
            return new PointCoords(
                    new Point((int) Math.round(v.getX()), (int) Math.round(v.getY())), io);
          }
        }
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ij.gui.ImageCanvas#mouseMoved(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseMoved(MouseEvent e) {
    super.mouseMoved(e);
    // offscreen - coordinates of the image, regardless zoom. e - absolute coordinates of the window
    Point p = new Point(offScreenX(e.getX()), offScreenY(e.getY()));
    // LOGGER.trace("e: [" + e.getX() + "," + e.getY() + "] offScreenX: " + p.toString());
    PointCoords ptmp = checkProximity(p, sensitivity);
    if (ptmp != null) { // if there is point close
      pc = ptmp; // set it to current under mouse
      repaint(); // refresh
    } else {
      if (pc != null) {
        pc = null; // otherwise clear current under mouse
        repaint(); // and repaint
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see ij.gui.ImageCanvas#paint(java.awt.Graphics)
   */
  @Override
  public void paint(Graphics g) {
    super.paint(g);
    Graphics2D g2 = (Graphics2D) g;
    double half = pointSize / 2;
    if (pc != null) {
      Rectangle2D e = new Rectangle2D.Double(screenXD(pc.point.getX()) - half,
              screenYD(pc.point.getY()) - half, pointSize, pointSize);
      g2.setPaint(pointColor);
      g2.draw(e);
    }
    g2.setPaint(staticPointColor);
    for (PointCoords p : model.selected) {
      Ellipse2D e = new Ellipse2D.Double(screenXD(p.point.getX()) - half,
              screenYD(p.point.getY()) - half, pointSize, pointSize);
      g2.fill(e);
    }
  }

}