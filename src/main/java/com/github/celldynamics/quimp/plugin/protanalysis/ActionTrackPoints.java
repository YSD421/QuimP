package com.github.celldynamics.quimp.plugin.protanalysis;

import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.scijava.vecmath.Point3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.QParamsQconf;
import com.github.celldynamics.quimp.filesystem.QconfLoader;
import com.github.celldynamics.quimp.geom.MapCoordConverter;
import com.github.celldynamics.quimp.plugin.qanalysis.STmap;

import ij.WindowManager;

/**
 * Action for track button.
 * 
 * @author baniu
 *
 */
@SuppressWarnings("serial")
public class ActionTrackPoints extends ProtAnalysisAbstractAction implements Action {
  static final Logger LOGGER = LoggerFactory.getLogger(ActionTrackPoints.class.getName());

  /**
   * Action creator.
   * 
   * @param name name
   * @param desc description
   * @param ui reference to outer class.
   */
  public ActionTrackPoints(String name, String desc, CustomStackWindow ui) {
    super(name, desc, ui);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    QconfLoader qconfLoader = ui.getModel().getQconfLoader();
    track(qconfLoader);
  }

  void track(QconfLoader qconfLoader) {
    // TODO Finish
    // Outlines must be plotted with ECMM output but not Snakes otherwise coords do not match
    // Remember frames as well with ui.getModel().selected
    STmap[] stMap = ((QParamsQconf) qconfLoader.getQp()).getLoadedDataContainer().getQState();
    TrackVisualisation.Image visStackStatic =
            new TrackVisualisation.Image(ui.getImagePlus().duplicate()); // FIXME no duplicate
    visStackStatic.getOriginalImage().setTitle(WindowManager.makeUniqueName("Static tracking"));
    MaximaFinder mf = new MaximaFinder(ui.getImagePlus().getProcessor());

    List<Point2D> tmpSelected = new ArrayList<>();
    for (Point3i p : ui.getModel().selected) {
      int tmpIndex = MapCoordConverter.findPointIndex(stMap[0].getxMap()[p.getZ()],
              stMap[0].getyMap()[p.getZ()], p.getX(), p.getY(), Double.MAX_VALUE);
      if (tmpIndex >= 0) {
        tmpSelected.add(new Point2D.Double(0, tmpIndex));
      }
    }
    LOGGER.trace("Added " + tmpSelected.size() + " points");
    mf.setMaxima(tmpSelected); // FIXME max are in map coordinates not xy
    // for (STmap mapCell : stMap) {
    visStackStatic.addElementsToImage(stMap[0], null, mf);
    // }
    visStackStatic.getOriginalImage().show();
  }

}
