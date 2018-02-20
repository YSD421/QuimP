package com.github.celldynamics.quimp.plugin.ana;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.CellStats;
import com.github.celldynamics.quimp.FrameStatistics;
import com.github.celldynamics.quimp.Outline;
import com.github.celldynamics.quimp.OutlineHandler;
import com.github.celldynamics.quimp.QParams;
import com.github.celldynamics.quimp.QParamsQconf;
import com.github.celldynamics.quimp.QuimP;
import com.github.celldynamics.quimp.QuimpException;
import com.github.celldynamics.quimp.QuimpException.MessageSinkTypes;
import com.github.celldynamics.quimp.Vert;
import com.github.celldynamics.quimp.filesystem.ANAParamCollection;
import com.github.celldynamics.quimp.filesystem.DataContainer;
import com.github.celldynamics.quimp.filesystem.OutlinesCollection;
import com.github.celldynamics.quimp.filesystem.converter.FormatConverter;
import com.github.celldynamics.quimp.geom.ExtendedVector2d;
import com.github.celldynamics.quimp.plugin.AbstractPluginQconf;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;
import com.github.celldynamics.quimp.plugin.ecmm.ECMM_Mapping;
import com.github.celldynamics.quimp.plugin.ecmm.ECMp;
import com.github.celldynamics.quimp.plugin.ecmm.ODEsolver;
import com.github.celldynamics.quimp.utils.QuimPArrayUtils;
import com.github.celldynamics.quimp.utils.QuimpToolsCollection;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.YesNoCancelDialog;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Converter;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * Main ANA class implementing IJ PlugInFilter.
 * 
 * @author tyson
 */
public class ANA_ extends AbstractPluginQconf implements DialogListener {

  private static String thisPluginName = "ANA";

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(ANA_.class.getName());

  private OutlineHandler oh; // set by runFrom*
  private OutlineHandler outputH;
  private OutlineHandler ecmH;
  private OutlinesCollection outputOutlineHandlers; // output for new data file
  private Outline frameOneClone;
  private ECMM_Mapping ecmMapping;
  private Overlay overlay;
  // outlines can be plotted separately. They are generated by Ana() and stored here
  private ArrayList<Roi> storedOuterROI; // outer outline for each frame for all cells
  private ArrayList<Roi> storedInnerROI; // inner outline for each frame for all cells

  private ImagePlus setupImage; // image fluoro
  private ImageProcessor orgIpr; // passed by setup

  /**
   * ANA extends statistics generated by BOA by fluorescence related data.
   * 
   * <p>This is object that holds stats read from stQP file.
   */
  private FrameStatistics[] fluoStats;
  private ANAp anap;
  private static final int m =
          Measurements.AREA + Measurements.INTEGRATED_DENSITY + Measurements.MEAN;

  /**
   * Default constructor called always.
   */
  public ANA_() {
    super(new AnaOptions(), thisPluginName);
    storedOuterROI = new ArrayList<>();
    storedInnerROI = new ArrayList<>();
    anap = new ANAp();
    ECMp.plot = false;
    ecmMapping = new ECMM_Mapping(1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.AbstractPluginBase#run(java.lang.String)
   */
  @Override
  public void run(String arg) {
    // overcome problem, IJ UI somehow break this so show before or IJ.run below
    // publishMacroString("ANA");
    setupImage = WindowManager.getCurrentImage();
    if (setupImage == null) {
      IJ.error("Image required to take fluoresence measurments.");
      return;
    }
    if (setupImage.getOriginalFileInfo() == null
            || setupImage.getOriginalFileInfo().directory.matches("")) {
      IJ.log("Error: Fluorescence file needs to be saved to disk");
      IJ.error("Please save your fluorescence image to file.");
      return;
    }
    Prefs.interpolateScaledImages = false; // switch off interpolation of zoomed images
    // IJ.run("Appearance...", " menu=0"); // switch off interpolation of zoomed images
    overlay = new Overlay();
    setupImage.setOverlay(overlay);
    orgIpr = setupImage.getProcessor();
    super.run(arg);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.AbstractPluginQconf#executer()
   */
  @Override
  protected void executer() throws QuimpException {
    IJ.showStatus("ANA Analysis");
    super.executer(); // will run runFrom*
    // post-processing
    if (qconfLoader.getQp() == null) {
      return; // cancelled
    }
    // and then do the rest
    AnaOptions opts = (AnaOptions) options;
    // post-plotting
    overlay = new Overlay();
    setupImage.setOverlay(overlay);
    for (int f = 1; f < setupImage.getStackSize(); f++) {
      setupImage.setSlice(f);
      for (OutlineHandler ohTmp : outputOutlineHandlers.oHs) {
        if (f >= ohTmp.getStartFrame() && f <= ohTmp.getEndFrame()) {
          Outline o = ohTmp.getStoredOutline(f);
          if (o == null) { // should not happen
            continue;
          }
          drawSamplePointsFloat(o, f);
          setupImage.draw();
        }
      }
    }
    // plotting outlines on separate image
    if (opts.plotOutlines) {
      ImagePlus orgIplclone = setupImage.duplicate();
      orgIplclone.show();
      new Converter().run("RGB Color");
      Overlay overlay = new Overlay();
      orgIplclone.setOverlay(overlay);
      for (Roi r : storedOuterROI) {
        overlay.add(r);
      }
      for (Roi r : storedInnerROI) {
        overlay.add(r);
      }
      orgIplclone.draw();
    }

    // edd results to IJtable named Results - to allow Summarise
    if (opts.fluoResultTable || opts.fluoResultTableAppend) {
      if (qconfLoader.isFileLoaded() == QParams.NEW_QUIMP) {
        ResultsTable rt;
        if (opts.fluoResultTableAppend) { // get current table
          rt = Analyzer.getResultsTable();
        } else { // or create new
          rt = new ResultsTable();
          Analyzer.setResultsTable(rt);
        }
        // iterate over cells - all cells for this experiment are cumulated in one table
        for (CellStats cs : qconfLoader.getStats().getStatCollection()) {
          cs.addFluosToResultTable(rt, opts.channel);
        }
        rt.show("Results");
      } else {
        LOGGER.warn(
                "Results can be shown in IJ table only if ANA is started with QCONF file format");
      }
    }
    IJ.log("ANA Analysis complete");
    IJ.showStatus("Finished");
    ecmMapping = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.AbstractPluginQconf#validate()
   */
  @Override
  protected void validate() throws QuimpException {
    qconfLoader.getBOA();
    qconfLoader.getEcmm(); // verify whether ecmm has been run (throws if not)
    qconfLoader.getStats(); // verify whether file contains stats
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpPlugin#about()
   */
  @Override
  public String about() {
    return "ANA plugin.\n" + "Authors: Piotr Baniukiewicz\n"
            + "mail: p.baniukiewicz@warwick.ac.uk\n" + "Richard Tyson";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.AbstractPluginFilterQconf#runFromQconf()
   */
  @Override
  protected void runFromQconf() throws QuimpException {
    AnaOptions opts = (AnaOptions) options;
    LOGGER.debug("Processing from new file format");
    if (apiCall == false && errorSink == MessageSinkTypes.GUI && qconfLoader.isANAPresent()) {
      YesNoCancelDialog ync;
      ync = new YesNoCancelDialog(IJ.getInstance(), "Overwrite",
              "You are about to override previous ANA results. Is it ok?");
      if (!ync.yesPressed()) { // if no or cancel
        IJ.log("No changes done in input file.");
        return; // end}
      }
    }

    QParamsQconf qp = (QParamsQconf) qconfLoader.getQp();
    ANAParamCollection anaStates;
    OutlinesCollection ecmmState = qp.getLoadedDataContainer().ECMMState;
    outputOutlineHandlers = new OutlinesCollection(ecmmState.oHs.size());
    if (qp.getLoadedDataContainer().getANAState() == null) {
      // create ANA slots for all outlines
      anaStates = new ANAParamCollection(ecmmState.oHs.size()); // store ANA options for every cell
    } else {
      anaStates = qp.getLoadedDataContainer().getANAState(); // update old
    }
    try {
      for (int i = 0; i < ecmmState.oHs.size(); i++) { // go over all outlines
        // For compatibility, all methods have the same syntax (assumes that there is only one
        // handler)
        qp.setActiveHandler(i); // set current handler number.
        oh = ecmmState.oHs.get(i); // restore handler from ecmm
        anap = anaStates.aS.get(i); // get i-th ana parameters
        anap.setup(qconfLoader.getQp());

        // get stats stored in QCONF, they are extended by ANA (ChannelStat field)
        fluoStats = qconfLoader.getStats().sHs.get(i).framestat.toArray(new FrameStatistics[0]);

        investigateChannels(oh.indexGetOutline(0));// find first empty channel, change anap
        if (anap.noData && oh.getSize() == 1) {
          // only one frame, so no ECMM. set outline res to 2
          System.out.println("Only one frame. set marker res to 2");
          oh.indexGetOutline(0).setResolution(anap.oneFrameRes); // should be 2!!!
        }
        setImageScale();
        setupImage.setSlice(qconfLoader.getQp().getStartFrame());
        // openadialog only if called from IJ, apiCall==false for all IJ calls, so check also sink
        // to find if run from macros
        if (apiCall == false && errorSink != MessageSinkTypes.IJERROR && !anaDialog()) {
          IJ.log("ANA cancelled");
          return;
        } else { // macro, do part of anaDialog
          frameOneClone = (Outline) oh.indexGetOutline(0).clone(); // FIXME Change to copy construc
          anap.setCortextWidthScale(opts.userScale); // set scale from macro instead from UI
          if (opts.clearFlu && !anap.cleared) {
            resetFluo();
          }
        }
        anap.fluTiffs[opts.channel] = new File(setupImage.getOriginalFileInfo().directory,
                setupImage.getOriginalFileInfo().fileName);
        outputH = new OutlineHandler(oh); // copy input to output (ana will add fields to it)
        runPlugin(); // fills outputH and ChannelStat in FrameStatistics
        // save fluoro to statFile if old format selected
        if (QuimP.newFileFormat.get() == false) {
          FrameStatistics.write(fluoStats, anap.statFile, anap.scale, anap.frameInterval);
        }
        CellStats statH = qconfLoader.getStats().sHs.get(i); // store fluoro in QCONF
        statH.framestat = new ArrayList<FrameStatistics>(Arrays.asList(fluoStats)); // store stats
        outputOutlineHandlers.oHs.add(i, new OutlineHandler(outputH)); // store actual result cont
      }

      DataContainer dc = qp.getLoadedDataContainer();
      dc.ECMMState = outputOutlineHandlers; // assign ECMM container to global output
      dc.ANAState = anaStates;
      qp.writeParams(); // save global container
    } catch (IOException e) {
      throw new QuimpPluginException(e);
    }
    // generate additional OLD files (stQP is generated in loop already), disabled #263, enabled 228
    if (QuimP.newFileFormat.get() == false) {
      FormatConverter foramtConv = new FormatConverter(qconfLoader);
      foramtConv.doConversion();
    }
    IJ.log("The new data file " + qconfLoader.getQp().getParamFile().toString()
            + " has been updated by results of ECMM analysis.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.AbstractPluginFilterQconf#runFromPaqp()
   */
  @Override
  protected void runFromPaqp() throws QuimpException {
    AnaOptions opts = (AnaOptions) options;
    outputOutlineHandlers = new OutlinesCollection(1);
    oh = new OutlineHandler(qconfLoader.getQp());
    try {
      anap.setup(qconfLoader.getQp());
      fluoStats = FrameStatistics.read(anap.statFile);
      investigateChannels(oh.indexGetOutline(0));// find first empty channel

      if (anap.noData && oh.getSize() == 1) {
        // only one frame, so no ECMM. set outline res to 2
        System.out.println("Only one frame. set marker res to 2");
        oh.indexGetOutline(0).setResolution(anap.oneFrameRes); // should be 2!!!
      }

      setImageScale();
      setupImage.setSlice(qconfLoader.getQp().getStartFrame());
      if (!oh.readSuccess) {
        throw new QuimpException("Could not read OutlineHandler");
      }
      // openadialog only if called from IJ
      if (apiCall == false && errorSink != MessageSinkTypes.IJERROR && !anaDialog()) {
        IJ.log("ANA cancelled");
        return;
      } else { // macro, do part of anaDialog
        frameOneClone = (Outline) oh.indexGetOutline(0).clone(); // FIXME Change to copy construc
        anap.setCortextWidthScale(opts.userScale);
        if (opts.clearFlu && !anap.cleared) {
          resetFluo();
        }
      }
      System.out.println("CHannel: " + (opts.channel + 1));
      // qp.cortexWidth = ANAp.cortexWidthScale;
      anap.fluTiffs[opts.channel] = new File(setupImage.getOriginalFileInfo().directory,
              setupImage.getOriginalFileInfo().fileName);

      outputH = new OutlineHandler(oh.getStartFrame(), oh.getEndFrame());
      runPlugin(); // fills outputH and ChannelStat in FrameStatistics

      anap.inFile.delete();
      anap.statFile.delete();
      outputH.writeOutlines(anap.outFile, qconfLoader.getQp().isEcmmHasRun());
      FrameStatistics.write(fluoStats, anap.statFile, anap.scale, anap.frameInterval);

      // ----Write temp files-------
      // File tempFile = new File(ANAp.outFile.getAbsolutePath() +
      // ".tempANA.txt");
      // outputH.writeOutlines(tempFile);
      // File tempStats = new File(ANAp.statFile.getAbsolutePath() +
      // ".tempStats.csv");
      // FluoStats.write(fluoStats, tempStats);
      // IJ.log("ECMM:137, saving to a temp file instead");
      // --------------------------

      qconfLoader.getQp().cortexWidth = anap.getCortexWidthScale();
      qconfLoader.getQp().fluTiffs = anap.fluTiffs;
      qconfLoader.getQp().writeParams();
    } catch (IOException e) {
      throw new QuimpPluginException(e);
    }
    outputOutlineHandlers.oHs.add(0, new OutlineHandler(outputH)); // for plotting purposes

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.AbstractPluginBase#showUi(boolean)
   */
  @Override
  public void showUi(boolean val) throws Exception {
    // load on GUI show as well
    executer();
    if (qconfLoader != null && qconfLoader.getQp() != null) {
      options.paramFile = qconfLoader.getQp().getParamFile().getAbsolutePath();
    }
  }

  /**
   * Show dialog.
   * 
   * @return true if OK pressed
   */
  private boolean anaDialog() {
    AnaOptions opts = (AnaOptions) options;
    GenericDialog pd = new GenericDialog("ANA Dialog", IJ.getInstance());
    // initialise scale UI from QCONF
    pd.addNumericField("Cortex width (\u00B5m)", anap.getCortexWidthScale(), 2);

    String[] channelC = { "1", "2", "3" };
    pd.addChoice("Save in channel", channelC, channelC[opts.channel]);
    pd.addCheckbox("Normalise to interior", opts.normalise);
    pd.addCheckbox("Sample at Ch" + (anap.useLocFromCh + 1) + " locations", opts.sampleAtSame);
    pd.addCheckbox("Clear stored measurements", false);
    pd.addCheckbox("New image with outlines? ", opts.plotOutlines);
    pd.addCheckbox("Copy results to IJ Table?", opts.fluoResultTable);
    pd.addCheckbox("Append results to IJ Table?", opts.fluoResultTableAppend);
    pd.addDialogListener(this);

    frameOneClone = (Outline) oh.indexGetOutline(0).clone(); // FIXME Change to copy constructor
    drawOutlineAsOverlay(frameOneClone, Color.RED);
    shrink(frameOneClone);
    this.markFrozenNodesNormal(frameOneClone);
    setupImage.draw();
    drawOutlineAsOverlay(frameOneClone, Color.RED);
    pd.showDialog();

    return pd.wasOKed();

  }

  /*
   * (non-Javadoc)
   * 
   * @see ij.gui.DialogListener#dialogItemChanged(ij.gui.GenericDialog, java.awt.AWTEvent)
   */
  @Override
  public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
    // fills options from UI
    if (gd.wasOKed()) {
      return true;
    }
    AnaOptions opts = (AnaOptions) options;
    // add and append results can not be both active
    {
      Checkbox cb4 = (Checkbox) gd.getCheckboxes().elementAt(4); // move results to table
      Checkbox cb5 = (Checkbox) gd.getCheckboxes().elementAt(5); // append results to table
      if (e.getSource() == cb4) {
        if (cb4.getState()) {
          cb5.setState(false);
        }
      }
      if (e.getSource() == cb5) {
        if (cb5.getState()) {
          cb4.setState(false);
        }
      }
    }

    Checkbox cb = (Checkbox) gd.getCheckboxes().elementAt(2); // clear measurements
    opts.clearFlu = cb.getState();
    Choice iob = (Choice) gd.getChoices().elementAt(0);
    if (opts.clearFlu && !anap.cleared) { // reset if clear measurments checked
      System.out.println("reset fluo");
      resetFluo();
      cb.setLabel("Measurments Cleared");
      IJ.log("All fluorescence measurements have been cleared");
      iob.select(0);
      return true;
    }

    opts.channel = gd.getNextChoiceIndex();
    opts.normalise = gd.getNextBoolean();
    opts.sampleAtSame = gd.getNextBoolean();
    opts.plotOutlines = ((Checkbox) gd.getCheckboxes().elementAt(3)).getState();
    // under multiple AAN run if there are many cells, remember only
    opts.fluoResultTable = ((Checkbox) gd.getCheckboxes().elementAt(4)).getState();
    opts.fluoResultTableAppend = ((Checkbox) gd.getCheckboxes().elementAt(5)).getState();
    // copy scale to macro options and configuration
    opts.userScale = gd.getNextNumber();
    anap.setCortextWidthScale(opts.userScale);
    if (anap.cleared) { // can't deselect
      cb.setState(true);
    }

    frameOneClone = (Outline) oh.indexGetOutline(0).clone(); // FIXME Change to copy constructor
    overlay.clear();
    drawOutlineAsOverlay(frameOneClone, Color.RED);
    shrink(frameOneClone);
    this.markFrozenNodesNormal(frameOneClone);
    setupImage.draw();
    drawOutlineAsOverlay(frameOneClone, Color.RED);
    return true;// gd.invalidNumber();
  }

  /**
   * Reset fluo.
   */
  void resetFluo() {
    // reset all fluo back to -2 and st res to 2 if only one frame
    AnaOptions opts = (AnaOptions) options;
    Outline o;
    for (int i = 0; i < oh.getSize(); i++) {
      o = oh.indexGetOutline(i);
      o.clearFluores();
      fluoStats[i].clearFluo();
    }

    if (oh.getSize() == 1) {
      // only one frame, so no ECMM. set outline res to 2
      System.out.println("Only one frame. set marker res to 2");
      oh.indexGetOutline(0).setResolution(anap.oneFrameRes);
    }

    // clear frame stats
    anap.noData = true;
    opts.channel = 0;
    anap.useLocFromCh = -1;
    anap.presentData[1] = 0;
    anap.presentData[2] = 0;
    anap.presentData[0] = 0;
    anap.fluTiffs[0] = new File("/");
    anap.fluTiffs[1] = new File("/");
    anap.fluTiffs[2] = new File("/");

    opts.channel = 0;
    anap.cleared = true;
  }

  /**
   * Sets the image scale.
   */
  void setImageScale() {
    setupImage.getCalibration().frameInterval = anap.frameInterval;
    setupImage.getCalibration().pixelHeight = anap.scale;
    setupImage.getCalibration().pixelWidth = anap.scale;
  }

  /**
   * Main method for fluorescence measurements analysis. Adds also new stats to FrameStatistics.
   * 
   * @see #runFromQconf()
   * @see #runFromPaqp()
   */
  private void runPlugin() {
    Roi outerRoi;
    Roi innerRoi;
    Outline o1;
    Outline s1;
    Outline s2;
    AnaOptions opts = (AnaOptions) options;
    IJ.showStatus("Running ANA (" + oh.getSize() + " frames)");
    for (int f = oh.getStartFrame(); f <= oh.getEndFrame(); f++) { // change i to frames
      IJ.log("Frame " + f);
      IJ.showProgress(f, oh.getEndFrame());

      setupImage.setSlice(f);
      o1 = oh.getStoredOutline(f);

      s1 = new Outline(o1);
      s2 = new Outline(o1);
      shrink(s2);

      // HACK for Du's embryoImage
      // shrink(s1);
      // s1.scale(14, 0.2);
      // ***

      // prepare overlay for current frame for plotting inner and outer outline
      overlay = new Overlay();
      setupImage.setOverlay(overlay);
      outerRoi = o1.asFloatRoi(); // convert outlines to ROI
      innerRoi = s2.asFloatRoi();
      outerRoi.setPosition(f); // set for frame f
      outerRoi.setStrokeColor(Color.BLUE);
      innerRoi.setPosition(f);
      innerRoi.setStrokeColor(Color.RED);

      // store in object, will be plotted depending on user choice.
      storedInnerROI.add(innerRoi);
      storedOuterROI.add(outerRoi);
      overlay.add(outerRoi); // this is for real time preview during computations
      overlay.add(innerRoi);

      Polygon polyS2 = s2.asPolygon();
      setFluoStats(s1.asPolygon(), polyS2, f); // compute FrameStatistics for frame f

      // compute Vert.fluores field in Outline (FluoMeasurement[] fluores)
      // use sample points already there
      if (opts.sampleAtSame && anap.useLocFromCh != -1) {
        useGivenSamplepoints(o1);
      } else {

        ecmH = new OutlineHandler(1, 2);
        ecmH.setOutline(1, s1);
        ecmH.setOutline(2, s2);

        ecmH = ecmMapping.runByANA(ecmH, orgIpr, anap.getCortexWidthPixel());

        // copy flur data to o1 and save
        // some nodes may fail to migrate properly so need to check
        // tracknumbers match
        Vert v = o1.getHead();
        Vert v2 = ecmH.getStoredOutline(2).getHead();

        while (v2.getTrackNum() != v.getTrackNum()) { // check id's match
          v = v.getNext();
          if (v.isHead()) {
            IJ.error("ANA fail");
            break;
            // return;
          }
        }

        int vertStart;
        do {
          v.setFluoresChannel(v2.fluores[0], opts.channel);
          v2 = v2.getNext();
          if (v2.isHead()) {
            break;
          }
          vertStart = v.getTrackNum();
          // find next vert in o1 that matches v2
          do {
            v = v.getNext();
            v.setFluoresChannel((int) Math.round(v.getX()), (int) Math.round(v.getY()), -1,
                    opts.channel); // map fail if -1. fix by interpolation
            if (vertStart == v.getTrackNum()) {
              System.out.println("ANA fail");
              return;
            }
          } while (v2.getTrackNum() != v.getTrackNum());
        } while (!v2.isHead());

        interpolateFailures(o1);
      }

      if (opts.normalise) {
        normalise2Interior(o1, f);
      }
      outputH.save(o1, f);
    }
  }

  private void shrink(Outline o) {
    // shrink outline
    o.scaleOutline(anap.getCortexWidthPixel(), -anap.stepRes, anap.angleTh, anap.freezeTh);

    o.unfreezeAll();
  }

  private void markFrozenNodesNormal(Outline o) {
    float[] x;
    float[] y;
    ExtendedVector2d norm;
    PolygonRoi pr;
    Vert v = o.getHead();
    do {
      if (v.isFrozen()) {
        overlay.setStrokeColor(Color.RED);
        norm = new ExtendedVector2d(v.getX(), v.getY());
        norm.addVec(v.getNormal());
        // norm.addVec(new Vect2d(1,1));

        x = new float[2];
        y = new float[2];

        x[0] = (float) v.getX();
        x[1] = (float) norm.getX();
        y[0] = (float) v.getY();
        y[1] = (float) norm.getY();
        pr = new PolygonRoi(x, y, 2, Roi.POLYGON);
        overlay.add(pr);
      }

      v = v.getNext();
    } while (!v.isHead());
  }

  /**
   * Compute statistics.
   * 
   * <p>Update {@link com.github.celldynamics.quimp.plugin.ana.ChannelStat} in
   * {@link com.github.celldynamics.quimp.FrameStatistics}
   * 
   * @param outerPoly outerPoly
   * @param innerPoly innerPoly
   * @param f frame
   */
  private void setFluoStats(Polygon outerPoly, Polygon innerPoly, int f) {
    AnaOptions opts = (AnaOptions) options;
    int store = f - anap.startFrame; // frame to index
    // System.out.println("store: " + store);
    fluoStats[store].frame = f;

    orgIpr.setRoi(outerPoly);
    // this does NOT scale to image
    ImageStatistics is = ImageStatistics.getStatistics(orgIpr, m, null);
    double outerAreaRaw = is.area;
    fluoStats[store].channels[opts.channel].totalFluor = is.mean * outerAreaRaw;
    fluoStats[store].channels[opts.channel].meanFluor = is.mean;

    orgIpr.setRoi(innerPoly);
    is = ImageStatistics.getStatistics(orgIpr, m, null);

    fluoStats[store].channels[opts.channel].innerArea =
            QuimpToolsCollection.areaToScale(is.area, anap.scale);
    fluoStats[store].channels[opts.channel].totalInnerFluor = is.mean * is.area;
    fluoStats[store].channels[opts.channel].meanInnerFluor = is.mean;

    fluoStats[store].channels[opts.channel].cortexArea =
            fluoStats[store].area - fluoStats[store].channels[opts.channel].innerArea; // scaled
    fluoStats[store].channels[opts.channel].totalCorFluo =
            fluoStats[store].channels[opts.channel].totalFluor
                    - fluoStats[store].channels[opts.channel].totalInnerFluor;
    fluoStats[store].channels[opts.channel].meanCorFluo =
            fluoStats[store].channels[opts.channel].totalCorFluo / (outerAreaRaw - is.area);

    fluoStats[store].channels[opts.channel].percCortexFluo =
            (fluoStats[store].channels[opts.channel].totalCorFluo
                    / fluoStats[store].channels[opts.channel].totalFluor) * 100;
    fluoStats[store].channels[opts.channel].cortexWidth = anap.getCortexWidthScale();
  }

  private void normalise2Interior(Outline o, int f) {
    AnaOptions opts = (AnaOptions) options;
    // interior mean fluorescence is used to normalse membrane measurments
    int store = f - anap.startFrame; // frame to index
    Vert v = o.getHead();
    do {
      v.fluores[opts.channel].intensity = v.fluores[opts.channel].intensity
              / fluoStats[store].channels[opts.channel].meanInnerFluor;
      v = v.getNext();
    } while (!v.isHead());

  }

  private void drawOutlineAsOverlay(Outline o, Color c) {
    Roi r = o.asFloatRoi();
    if (r.subPixelResolution()) {
      System.out.println("is sub pixel");
    } else {
      System.out.println("is not sub pixel");
    }
    overlay.setStrokeColor(c);
    overlay.add(r);
    setupImage.updateAndDraw();
  }

  private void investigateChannels(Outline o) {
    // flu maps
    int firstEmptyCh = -1;
    int firstFullCh = -1;
    AnaOptions opts = (AnaOptions) options;

    anap.presentData = new int[3];
    anap.noData = true;

    Vert v = o.getHead();
    for (int i = 0; i < 3; i++) {
      if (v.fluores[i].intensity == -2) { // no data
        anap.presentData[i] = 0;
        if (firstEmptyCh == -1) {
          firstEmptyCh = i;
        }
      } else {
        anap.presentData[i] = 1;
        IJ.log("Data exists in channel " + (i + 1));
        anap.noData = false;
        if (firstFullCh == -1) {
          firstFullCh = i;
        }
        // anap.setCortextWidthScale(fluoStats[0].channels[i].cortexWidth);
      }
    }

    if (QuimPArrayUtils.sumArray(anap.presentData) == 3) {
      firstEmptyCh = 0;
    }

    if (anap.noData) {
      opts.channel = 0;
      IJ.log("No previous sample points available.");
      anap.useLocFromCh = -1;
    } else {
      opts.channel = firstEmptyCh;
      IJ.log("Sample points from channel " + (firstFullCh + 1) + " available.");
      anap.useLocFromCh = firstFullCh;
    }

    v = o.getHead();
    for (int i = 0; i < 3; i++) {
      if (v.fluores[i].intensity != -2) {
        anap.setCortextWidthScale(fluoStats[0].channels[i].cortexWidth);
      }
    }
  }

  private void interpolateFailures(Outline o) {
    Vert v = o.getHead();
    Vert last;
    Vert nex;
    double disLtoN; // distance last to nex
    double disLtoV; // distance last to V
    double ratio;
    double intensityDiff;
    boolean fail;
    int firstID;
    AnaOptions opts = (AnaOptions) options;
    do {
      fail = false;
      if (v.fluores[opts.channel].intensity == -1) {
        IJ.log("\tInterpolated failed node intensity (position: " + v.coord + ")");
        // failed to map - interpolate with last/next successful

        last = v.getPrev();
        firstID = last.getTrackNum();
        while (last.fluores[opts.channel].intensity == -1) {
          last = last.getPrev();
          if (last.getTrackNum() == firstID) {
            IJ.log("Could not interpolate as all nodes failed");
            v.fluores[opts.channel].intensity = 0;
            fail = true;
          }
        }

        nex = v.getNext();
        firstID = nex.getTrackNum();
        while (nex.fluores[opts.channel].intensity == -1) {
          nex = nex.getNext();
          if (nex.getTrackNum() == firstID) {
            IJ.log("Could not interpolate as all nodes failed");
            v.fluores[opts.channel].intensity = 0;
            fail = true;
          }
        }

        if (fail) {
          v = v.getNext();
          continue;
        }

        disLtoN = ExtendedVector2d.lengthP2P(last.getPoint(), nex.getPoint());
        disLtoV = ExtendedVector2d.lengthP2P(last.getPoint(), v.getPoint());
        ratio = disLtoV / disLtoN;
        if (ratio > 1) {
          ratio = 1;
        }
        if (ratio < 0) {
          ratio = 0;
        }
        intensityDiff = (nex.fluores[opts.channel].intensity - last.fluores[opts.channel].intensity)
                * ratio;
        v.fluores[opts.channel].intensity = last.fluores[opts.channel].intensity + intensityDiff;
        if (v.fluores[opts.channel].intensity < 0 || v.fluores[opts.channel].intensity > 255) {
          IJ.log("Error. Interpolated intensity out of range. Set to zero.");
          v.fluores[opts.channel].intensity = 0;
        }
      }

      v = v.getNext();
    } while (!v.isHead());
  }

  private void drawSamplePointsFloat(Outline o, int frame) {
    float x;
    float y;
    PointRoi pr;
    AnaOptions opts = (AnaOptions) options;
    Vert v = o.getHead();
    do {
      x = (float) v.fluores[opts.channel].x;
      y = (float) v.fluores[opts.channel].y;
      pr = new PointRoi(x + 0.5, y + 0.5);
      pr.setPosition(frame);
      overlay.add(pr);
      v = v.getNext();
    } while (!v.isHead());
  }

  /**
   * Add fluorescence data to outline.
   * 
   * @param o1 outline to complete o1.fluores[channel] data
   */
  private void useGivenSamplepoints(Outline o1) {
    int x;
    int y;
    AnaOptions opts = (AnaOptions) options;
    Vert v = o1.getHead();
    do {
      x = (int) v.fluores[anap.useLocFromCh].x;
      y = (int) v.fluores[anap.useLocFromCh].y;
      // use the same sampling as for ECMM solving
      v.fluores[opts.channel].intensity = ODEsolver.sampleFluo(orgIpr, x, y);
      v.fluores[opts.channel].x = x;
      v.fluores[opts.channel].y = y;
      v = v.getNext();
    } while (!v.isHead());

  }
}
