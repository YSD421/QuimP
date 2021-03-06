package com.github.celldynamics.quimp.filesystem;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.OutlineHandler;

/**
 * Represent collection of OutlineHandlers for cells.
 * 
 * <p>This class is used as storage of OutlineHandlers (results of continuous segmentation) in
 * {@link com.github.celldynamics.quimp.filesystem.DataContainer}.
 * 
 * @author p.baniukiewicz
 *
 */
public class OutlinesCollection implements IQuimpSerialize {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(OutlinesCollection.class.getName());
  /**
   * Contain {@link OutlineHandler} objects.
   * 
   * <p>Each object ({@link OutlineHandler} represents segmented cell (outline) between frame
   * <it>f1</it> and <it>f2</it> but only if segmentation process run continuously between these
   * frames. Outlines are returned by of ECMM module and they are almost the same as Snakes but
   * may differ in node order or number.
   * 
   */
  public ArrayList<OutlineHandler> oHs;

  /**
   * Instantiates a new outlines collection.
   *
   * @param size the size
   */
  public OutlinesCollection(int size) {
    oHs = new ArrayList<>(size);
  }

  /**
   * Instantiates a new outlines collection.
   */
  public OutlinesCollection() {
    oHs = new ArrayList<>();
  }

  /**
   * Prepare Outlines for serialization. Build arrays from Outline objects stored in
   * OutlineHandlers
   */
  @Override
  public void beforeSerialize() {
    if (oHs != null) {
      for (OutlineHandler oh : oHs) {
        oh.beforeSerialize();
      }
    }
  }

  /**
   * Rebuild every Outline from temporary array.
   */
  @Override
  public void afterSerialize() throws Exception {
    if (oHs != null) {
      for (OutlineHandler oh : oHs) {
        oh.afterSerialize();
      }
    }
  }
}
