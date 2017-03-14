package uk.ac.warwick.wsbc.quimp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.wsbc.quimp.BOAState.BOAp;
import uk.ac.warwick.wsbc.quimp.filesystem.DataContainer;
import uk.ac.warwick.wsbc.quimp.filesystem.FileExtensions;
import uk.ac.warwick.wsbc.quimp.filesystem.IQuimpSerialize;
import uk.ac.warwick.wsbc.quimp.filesystem.versions.Converter170202;
import uk.ac.warwick.wsbc.quimp.utils.QuimpToolsCollection;

// TODO: Auto-generated Javadoc
/**
 * This class override most of methods from super class QParams. The goal of this class is rather
 * not to extend QParams but to use polymorphism to provide requested data to callers keeping
 * compatibility with old QuimP architecture. The QuimP uses QParams to keep parameters read from
 * configuration files (<i>paQP</i>, <i>snQP</i>) and then to provide some of parameters stored in
 * these files to local configuration classes such as e.g.
 * {@link uk.ac.warwick.wsbc.quimp.plugin.ecmm.ECMp},
 * {@link uk.ac.warwick.wsbc.quimp.plugin.qanalysis.Q_Analysis},
 * {@link uk.ac.warwick.wsbc.quimp.plugin.ana.ANAp}. QuimP supports two independent file formats:
 * <ol>
 * <li>based on separate files (old QuimP) such as \a case_cellno.paQP
 * <li>compound <i>case.QCONF<i/> that contains data for all cells
 * </ol>
 * Many of parameters in underlying class QParams are set to be private and they are accessible by
 * setters and getters. Many setter/getter are overridden in this class and contains simple logic to
 * provide requested and expected data even if the source file was <i>QCONF<i/>. There is also
 * method that convert parameters read from QCONF and fills underlying fields in QParams.
 * Appropriate object either QParam or QParamsQconf is created upon configuration file type. Owing
 * to Java late binding, always correct method is called even if the object is casted to QParams
 * 
 * @author p.baniukiewicz
 *
 */
public class QParamsQconf extends QParams {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(QParamsQconf.class.getName());
  private Serializer<DataContainer> loaded; // instance of loaded data
  private File newParamFile;
  /**
   * Currently processed handler.
   * 
   * This is compatibility parameter. Old QuimP uses separated files for every snake thus QParams
   * contained always correct values as given snake has been loaded. New QuimP uses composed file
   * and this field points to currently processed Handler and it must be controlled from outside.
   * For compatibility reasons all setters and getters assumes that there is only one Handler (as
   * in old QuimP). This field allow to set current Handler if QParamsEschange instance is used.
   */
  private int currentHandler;

  /**
   * Instantiates a new q params qconf.
   */
  public QParamsQconf() {

  }

  /**
   * Set default values for superclass, also prefix and path for files.
   * 
   * @param p <i>QCONF<i/> file with extension
   */
  public QParamsQconf(File p) {
    super(p);
    currentHandler = 0;
    newParamFile = p;
    // prepare correct name for old parameters
    super.setParamFile(new File(QuimpToolsCollection
            .removeExtension(newParamFile.getParent() + File.separator + newParamFile.getName())
            + "_" + currentHandler + FileExtensions.configFileExt));
    paramFormat = QParams.NEW_QUIMP;
  }

  /**
   * @return the newParamFile
   */
  @Override
  public File getParamFile() {
    return newParamFile;
  }

  /**
   * @return the prefix. Without any cell number in contrary to super.getFileName(). Only filename
   *         without path and extension.
   */
  @Override
  public String getFileName() {
    return QuimpToolsCollection.removeExtension(newParamFile.getName());
  }

  /**
   * Extract DataContainer from Serializer super class
   * 
   * @return the loadedDataContainer
   */
  public DataContainer getLoadedDataContainer() {
    return loaded.obj;
  }

  /**
   * Read composite <i>QCONF<i/> file.
   * 
   * Update <tt>outputFileCore</tt> in {@link BOAp} to current QCONF.
   * 
   * @throws QuimpException when problem with loading/parsing JSON
   */
  @Override
  public void readParams() throws QuimpException {
    Serializer<DataContainer> s = new Serializer<>(DataContainer.class, QuimP.TOOL_VERSION);
    s.registerConverter(new Converter170202<>(QuimP.TOOL_VERSION));
    try {
      // load file and make first check of correctness
      loaded = s.load(getParamFile()); // try to load
      BOA_.qState = getLoadedDataContainer().getBOAState(); // restore qstate because some
                                                            // methods still need it
      // update path and file core name
      if (getLoadedDataContainer().getBOAState() != null)
        getLoadedDataContainer().getBOAState().boap.setOutputFileCore(
                QuimpToolsCollection.removeExtension(newParamFile.getAbsolutePath()));
    } catch (Exception e) { // stop on fail (file or json error)
      LOGGER.error(e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      throw new QuimpException(
              "Loading or processing of " + getParamFile().getAbsolutePath() + " failed", e);
    }
    // second check of basic logic
    // checking against nulls is in Serializer
    if (!loaded.className.equals("DataContainer") || !loaded.timeStamp.getName().equals("QuimP")
            && !loaded.timeStamp.getName().equals(QuimpToolsCollection.defNote)) {
      LOGGER.error("Not QuimP file?");
      throw new QuimpException(
              "Loaded file " + getParamFile().getAbsolutePath() + " is not QuimP file");
    }
    compatibilityLayer(); // fill underlying data (paQP) from QCONF
  }

  /**
   * Sets the active handler.
   *
   * @param num the new active handler
   */
  public void setActiveHandler(int num) {
    currentHandler = num;
    compatibilityLayer();
  }

  /**
   * Gets the active handler.
   *
   * @return the active handler
   */
  public int getActiveHandler() {
    return currentHandler;
  }

  /**
   * Write all parameters in new format.
   * 
   * Makes pure dump what means that object is already packed with QuimP format. Used when
   * original data has been loaded, modified and then they must be saved again under the same
   * name.
   * 
   * @throws IOException When file can not be saved
   */
  @Override
  public void writeParams() throws IOException {
    LOGGER.debug("New file format: Updating data " + getParamFile());
    try {
      // loaded.obj.beforeSerialize(); // call explicitly beforeSerialize because Dump doesn't
      // do
      // Serializer.Dump(loaded, getParamFile(), BOA_.qState.boap.savePretty); // "loaded" is
      // already
      // packed by
      // Serializer
      Serializer<DataContainer> n;
      n = new Serializer<>(getLoadedDataContainer(),
              new QuimpToolsCollection().getQuimPBuildInfo());
      if (getLoadedDataContainer().BOAState.boap.savePretty) // set pretty format if
                                                             // configured
        n.setPretty();
      n.save(getParamFile().getAbsolutePath());
      n = null;
    } catch (FileNotFoundException e) {
      LOGGER.error("File " + getParamFile() + " could not be saved. " + e.getMessage());
      LOGGER.debug(e.getMessage(), e);
      throw new IOException("File " + getParamFile() + " could not be saved. ", e);
    }
  }

  /**
   * Fill some underlying fields to assure compatibility between new and old formats.
   * <p>
   * <b>Warning</b>
   * <p>
   * Some data depend on status of <tt>currentHandler</tt> that points to current outline. This is
   * due to differences in file handling between old format (separate paQP for every cell) and new
   * (one file).
   */
  private void compatibilityLayer() {
    // fill underlying parameters
    super.setParamFile(new File(QuimpToolsCollection.removeExtension(newParamFile.getAbsolutePath())
            + "_" + currentHandler + FileExtensions.configFileExt));
    super.guessOtherFileNames();
    super.setSnakeQP(getSnakeQP());
    super.setStatsQP(getStatsQP());
    if (getLoadedDataContainer().getBOAState() != null) {
      super.setSegImageFile(getLoadedDataContainer().getBOAState().boap.getOrgFile());
      super.setImageScale(getLoadedDataContainer().getBOAState().boap.getImageScale());
      super.setFrameInterval(getLoadedDataContainer().getBOAState().boap.getImageFrameInterval());
      super.NMAX = getLoadedDataContainer().getBOAState().boap.NMAX;
      super.delta_t = getLoadedDataContainer().getBOAState().boap.delta_t;
      super.max_iterations = getLoadedDataContainer().getBOAState().segParam.max_iterations;
      super.setNodeRes(getLoadedDataContainer().getBOAState().segParam.getNodeRes());
      super.setBlowup(getLoadedDataContainer().getBOAState().segParam.blowup);
      super.sample_tan = getLoadedDataContainer().getBOAState().segParam.sample_tan;
      super.sample_norm = getLoadedDataContainer().getBOAState().segParam.sample_norm;
      super.vel_crit = getLoadedDataContainer().getBOAState().segParam.vel_crit;
      super.f_central = getLoadedDataContainer().getBOAState().segParam.f_central;
      super.f_contract = getLoadedDataContainer().getBOAState().segParam.f_contract;
      super.f_friction = getLoadedDataContainer().getBOAState().boap.f_friction;
      super.f_image = getLoadedDataContainer().getBOAState().segParam.f_image;
      super.sensitivity = getLoadedDataContainer().getBOAState().boap.sensitivity;
      super.finalShrink = getLoadedDataContainer().getBOAState().segParam.finalShrink;
      // set frames from snakes
      super.setStartFrame(getLoadedDataContainer().getBOAState().nest.getHandler(currentHandler)
              .getStartFrame());
      super.setEndFrame(
              getLoadedDataContainer().getBOAState().nest.getHandler(currentHandler).getEndFrame());
      if (getLoadedDataContainer().getECMMState() != null) {
        super.setStartFrame(
                getLoadedDataContainer().getECMMState().oHs.get(currentHandler).getStartFrame());
        super.setEndFrame(
                getLoadedDataContainer().getECMMState().oHs.get(currentHandler).getEndFrame());
      }
      // fill only if ANA has been run
      if (getLoadedDataContainer().getANAState() != null) {
        super.cortexWidth =
                getLoadedDataContainer().getANAState().aS.get(currentHandler).getCortexWidthScale();

        // copy here is due to #204 - when new tiff is added to old loaded fluTiffs,
        // previous absolute paths / are extended to full: /xxx/yyy/Quimp
        File[] lF = getLoadedDataContainer().getANAState().aS.get(currentHandler).fluTiffs;
        this.fluTiffs = new File[lF.length];
        fluTiffs[0] = new File(lF[0].getPath());
        fluTiffs[1] = new File(lF[1].getPath());
        fluTiffs[2] = new File(lF[2].getPath());
      }

    }
  }

  /**
   * Write parameter file paQP in old format (QuimP11).
   * 
   * @throws IOException
   * 
   */
  public void writeOldParams() throws IOException {
    super.writeParams();
  }

  /*
   * (non-Javadoc)
   * 
   * In old way this was related always to loaded file that was separate for every snake. In new
   * way this field should not exist stand alone
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getStartFrame()
   * 
   */
  @Override
  public int getStartFrame() {
    return super.getStartFrame();
  }

  /*
   * (non-Javadoc)
   * 
   * In old way this was related always to loaded file that was separate for every snake. In new
   * way this field should not exist stand alone
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#setStartFrame(int)
   * 
   */
  @Override
  public void setStartFrame(int startFrame) {
    super.setStartFrame(startFrame); // backward compatibility
    getLoadedDataContainer().getBOAState().nest.getHandler(currentHandler).startFrame = startFrame;
  }

  /*
   * (non-Javadoc)
   * 
   * In old way this was related always to loaded file that was separate for every snake. In new
   * way this field should not exist stand alone
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getEndFrame()
   */
  @Override
  public int getEndFrame() {
    return super.getEndFrame();
  }

  /*
   * (non-Javadoc)
   * 
   * In old way this was related always to loaded file that was separate for every snake. In new
   * way this field should not exist stand alone
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#setEndFrame(int)
   */
  @Override
  public void setEndFrame(int endFrame) {
    super.setEndFrame(endFrame);
    getLoadedDataContainer().getBOAState().nest.getHandler(currentHandler).endFrame = endFrame;
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getImageScale()
   */
  @Override
  public double getImageScale() {
    return super.getImageScale();
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#setImageScale(double)
   */
  @Override
  public void setImageScale(double imageScale) {
    getLoadedDataContainer().getBOAState().boap.setImageScale(imageScale);
    super.setImageScale(imageScale);
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getFrameInterval()
   */
  @Override
  public double getFrameInterval() {
    return super.getFrameInterval();
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#setFrameInterval(double)
   */
  @Override
  public void setFrameInterval(double frameInterval) {
    getLoadedDataContainer().getBOAState().boap.setImageFrameInterval(frameInterval);
    super.setFrameInterval(frameInterval);
  }

  /**
   * 
   * @return {@link Nest} object from loaded dataset.
   */
  public Nest getNest() {
    if (getLoadedDataContainer() != null)
      return getLoadedDataContainer().getBOAState().nest;
    else
      return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getBlowup()
   */
  @Override
  public int getBlowup() {
    return super.getBlowup();
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#setBlowup(int)
   */
  @Override
  public void setBlowup(int blowup) {
    getLoadedDataContainer().getBOAState().segParam.blowup = blowup;
    super.setBlowup(blowup);
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getNodeRes()
   */
  @Override
  public double getNodeRes() {
    return super.getNodeRes();
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#setNodeRes(int)
   */
  @Override
  public void setNodeRes(double nodeRes) {
    getLoadedDataContainer().getBOAState().segParam.setNodeRes(nodeRes);
    super.setNodeRes(nodeRes);
  }

  /**
   * For new file format it redirects call to super class searching for old files (paQP).
   * 
   * Finally old files can be processed together with new one.
   * 
   * @return Array of found files.
   * @see uk.ac.warwick.wsbc.quimp.QParams#findParamFiles()
   */
  @Override
  public File[] findParamFiles() {
    return super.findParamFiles();
  }

  /**
   * Create fake snQP name, for compatibility reasons.
   * 
   * @return theoretical name of snQP file which is used then to estimate names of map files by
   *         uk.ac.warwick.wsbc.quimp.Qp class. This name contains \a suffix already
   * @see uk.ac.warwick.wsbc.quimp.QParams#getSnakeQP()
   */
  @Override
  public File getSnakeQP() {
    String path = getParamFile().getParent();
    String file = QuimpToolsCollection.removeExtension(getParamFile().getName());
    return new File(
            path + File.separator + file + "_" + currentHandler + FileExtensions.snakeFileExt);
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParams#getStatsQP()
   * 
   * @see uk.ac.warwick.wsbc.quimp.QParamsQconf.getSnakeQP()
   */
  @Override
  public File getStatsQP() {
    String path = getParamFile().getParent();
    String file = QuimpToolsCollection.removeExtension(getParamFile().getName());
    return new File(
            path + File.separator + file + "_" + currentHandler + FileExtensions.statsFileExt);
  }

}

/**
 * Block execution of afterSerialize() in Serializer.
 * 
 * This method is not necessary now because one does not want to restore full plugin state. Other
 * data do not need any additional operations. (
 * 
 * Currently not used as loaded BOAState must be deserialized to restore Snakes from Elements arrays
 * 
 * @author p.baniukiewicz
 *
 * @param <T>
 * @deprecated But left here as example how to tackle the problem
 */
@Deprecated
class SerializerNoPluginSupport<T extends IQuimpSerialize> extends Serializer<T> {

  /**
   * @param obj
   * @param version
   */
  public SerializerNoPluginSupport(T obj, QuimpVersion version) {
    super(obj, version);
    doAfterSerialize = true; // false blocks afterSerialzie()
  }

  /**
   * @param t
   */
  public SerializerNoPluginSupport(Type t) {
    super(t, QuimP.TOOL_VERSION);
    doAfterSerialize = true; // false blocks afterSerialzie()
  }

}