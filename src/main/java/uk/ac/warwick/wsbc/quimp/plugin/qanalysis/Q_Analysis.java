package uk.ac.warwick.wsbc.quimp.plugin.qanalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import uk.ac.warwick.wsbc.quimp.FormatConverter;
import uk.ac.warwick.wsbc.quimp.OutlineHandler;
import uk.ac.warwick.wsbc.quimp.QColor;
import uk.ac.warwick.wsbc.quimp.QParams;
import uk.ac.warwick.wsbc.quimp.QParamsQconf;
import uk.ac.warwick.wsbc.quimp.QuimP;
import uk.ac.warwick.wsbc.quimp.QuimpException;
import uk.ac.warwick.wsbc.quimp.filesystem.DataContainer;
import uk.ac.warwick.wsbc.quimp.filesystem.FileExtensions;
import uk.ac.warwick.wsbc.quimp.filesystem.QconfLoader;
import uk.ac.warwick.wsbc.quimp.plugin.ecmm.ECMM_Mapping;
import uk.ac.warwick.wsbc.quimp.registration.Registration;
import uk.ac.warwick.wsbc.quimp.utils.QuimpToolsCollection;
import uk.ac.warwick.wsbc.quimp.utils.graphics.svg.SVGplotter;

/**
 * Run Q analysis for ECMM data.
 * 
 * <p>In principle this object loads and process name_X.paQP file generated by ECMM. <i>X</i> in
 * this
 * case means number of cell outline. The same result can be achieved by loading QCONF file that
 * contains all outlines for given case. This class is designed to process one outline in one time.
 * Thus most of methods operate on current status private fields (such as <tt>qp</tt>, <tt>oh</tt>).
 * The main analysis runner method is {@link #run()}, whereas support for both input formats is
 * covered by {@link #runFromPaqp()} and {@link #runFromQconf()} (similarly to {@link ECMM_Mapping
 * ECMM_Mapping})
 * 
 * @author rtyson
 * @author p.baniukiewicz
 */
public class Q_Analysis {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(Q_Analysis.class.getName());

  /**
   * The gd.
   */
  GenericDialog gd;
  private OutlineHandler oh; // keep loaded handler, can change during run
  private QconfLoader qconfLoader;
  private STmap stMap; // object holding all maps evaluated for current OutlineHandler (oh)

  /**
   * Main constructor and runner - class entry point.
   * 
   * <p>Left in this form for backward compatibility
   */
  public Q_Analysis() {
    this(null);
  }

  /**
   * Parameterised constructor for tests.
   * 
   * @param paramFile paQP or QCONF file to process. If <tt>null</tt> user is asked for this file
   * @see uk.ac.warwick.wsbc.quimp.plugin.ecmm.ECMM_Mapping#ECMM_Mapping(File)
   */
  public Q_Analysis(File paramFile) {
    about();
    IJ.showStatus("QuimP Analysis");
    // validate registered user
    new Registration(IJ.getInstance(), "QuimP Registration");
    try {
      qconfLoader = new QconfLoader(paramFile); // load file
      if (qconfLoader == null || qconfLoader.getQp() == null) {
        return; // failed to load exit
      }
      // show dialog
      if (!showDialog()) { // if user cancelled dialog
        return; // do nothing
      }
      if (qconfLoader.getConfVersion() == QParams.QUIMP_11) { // old path
        QParams qp;
        runFromPaqp();
        File[] otherPaFiles = qconfLoader.getQp().findParamFiles();
        if (otherPaFiles.length > 0) { // and process them if they are
          YesNoCancelDialog yncd = new YesNoCancelDialog(IJ.getInstance(), "Batch Process?",
                  "\tBatch Process?\n\n" + "Process other " + FileExtensions.configFileExt
                          + " files in the same folder with QAnalysis?"
                          + "\n[The same parameters will be used]");
          if (yncd.yesPressed()) {
            ArrayList<String> runOn = new ArrayList<String>(otherPaFiles.length);
            this.closeAllImages();

            // if user agreed iterate over found files
            // (except that loaded explicitly by user)
            for (int j = 0; j < otherPaFiles.length; j++) {
              IJ.log("Running on " + otherPaFiles[j].getAbsolutePath());
              paramFile = otherPaFiles[j];
              qconfLoader = new QconfLoader(paramFile);
              qp = qconfLoader.getQp();
              Qp.setup(qp);
              oh = new OutlineHandler(qp); // prepare current OutlineHandler
              if (!oh.readSuccess) {
                LOGGER.error("OutlineHandlers could not be read!");
                return;
              }
              run(); // run on current OutlineHandler
              runOn.add(otherPaFiles[j].getName());
              this.closeAllImages();
            }
            IJ.log("\n\nBatch - Successfully ran QAnalysis on:");
            for (int i = 0; i < runOn.size(); i++) {
              IJ.log(runOn.get(i));
            }
          } else {
            return; // no batch processing
          }
        }
      } else if (qconfLoader.getConfVersion() == QParams.NEW_QUIMP) { // new path
        // verification for components run
        qconfLoader.getBOA(); // will throw exception if not present
        qconfLoader.getEcmm(); // will throw exception if not present
        if (qconfLoader.isQPresent()) {
          YesNoCancelDialog ync;
          ync = new YesNoCancelDialog(IJ.getInstance(), "Overwrite",
                  "You are about to override previous Q results. Is it ok?");
          if (!ync.yesPressed()) { // if no or cancel
            IJ.log("No changes done in input file.");
            return; // end}
          }
        }
        runFromQconf();
        IJ.log("The new data file " + qconfLoader.getQp().getParamFile().toString()
                + " has been updated by results of Q Analysis.");
      } else {
        throw new IllegalStateException("QconfLoader returned unknown version of QuimP");
      }
      IJ.log("QuimP Analysis complete");
      IJ.showStatus("Finished");
    } catch (Exception e) {
      LOGGER.debug(e.getMessage(), e);
      LOGGER.error("Problem with running Q Analysis: " + e.getMessage());
    }
  }

  /**
   * Run Q Analysis if input was QCONF.
   * 
   * <p>Saves updated QCONF.
   * 
   * <p><b>Warning</b>
   * 
   * <p>{@link #run()} updates also {@link DataContainer#ECMMState ECMMState} by modifying fields in
   * Outlines that are accessed by reference here.
   * 
   * @throws QuimpException when saving failed or there is no ECMM data in file.
   * @throws IOException On problem with file writing
   */
  private void runFromQconf() throws QuimpException, IOException {
    int i = 0;
    QParamsQconf qp = (QParamsQconf) qconfLoader.getQp();
    Iterator<OutlineHandler> oi = qp.getLoadedDataContainer().getEcmmState().oHs.iterator();
    ArrayList<STmap> tmp = new ArrayList<>();
    while (oi.hasNext()) {
      qp.setActiveHandler(i++); // set current handler number.
      Qp.setup(qconfLoader.getQp()); // copy selected data from general QParams to local storage
      oh = oi.next();
      run();
      tmp.add(new STmap(stMap));
    }
    qp.getLoadedDataContainer().QState = tmp.toArray(new STmap[0]);
    qp.writeParams(); // save global container
    // generate additional OLD files, disabled #263, enabled 228
    if (QuimP.newFileFormat.get() == false) {
      FormatConverter formatConverter = new FormatConverter(qconfLoader);
      formatConverter.doConversion();
    }
  }

  /**
   * Run Q Analysis if input was paQP file.
   * 
   * @throws QuimpException when OutlineHandler can not be read
   */
  private void runFromPaqp() throws QuimpException {
    Qp.setup(qconfLoader.getQp()); // copy selected data from general QParams to local storage
    oh = new OutlineHandler(qconfLoader.getQp()); // load data from file
    if (!oh.readSuccess) {
      throw new QuimpException("Could not read OutlineHandler");
    }
    run();
  }

  /**
   * Display standard QuimP about message.
   */
  private void about() {
    IJ.log(new QuimpToolsCollection().getQuimPversion());
  }

  /**
   * Main runner - do all calculations on current OutlineHandler object.
   * 
   * <p><b>Warning</b>
   * 
   * <p>Process current <tt>OutlineHandler oh</tt>; object and modify it filling some fields from
   * Vertex class
   * 
   */
  private void run() {
    if (oh.getSize() == 1) {
      Qp.singleImage = true;
      // only one frame - re lable node indices
      oh.getOutline(1).resetAllCoords();
    }

    Qp.convexityToPixels();

    stMap = new STmap(oh, Qp.mapRes);

    SVGplotter svgPlotter = new SVGplotter(oh, Qp.fps, Qp.scale, Qp.channel, Qp.outFile);
    svgPlotter.plotTrack(Qp.trackColor, Qp.increment);
    // svgPlotter.plotTrackAnim();
    svgPlotter.plotTrackER(Qp.outlinePlot);

    Qp.convexityToUnits(); // reset the covexity options to units (as they are static)
  }

  private boolean showDialog() {
    gd = new GenericDialog("Q Analysis Options", IJ.getInstance());

    gd.setOKLabel("RUN");

    gd.addMessage(
            "Pixel width: " + Qp.scale + " \u00B5m\nFrame Interval: " + Qp.frameInterval + " sec");

    gd.addMessage("******* Cell track options (svg) *******");
    gd.addNumericField("Frame increment", Qp.increment, 0);
    gd.addChoice("Colour Map", QColor.colourMaps, QColor.colourMaps[0]);

    gd.addMessage("***** Motility movie options (svg) *****");
    gd.addChoice("Colour using", Qp.outlinePlots, Qp.outlinePlots[0]);

    gd.addMessage("********** Convexity options **********");
    gd.addNumericField("Sum over (\u00B5m)", Qp.sumCov, 2);
    gd.addNumericField("Smooth over (\u00B5m)", Qp.avgCov, 2);

    gd.addMessage("************* Map options *************");
    gd.addNumericField("Map resolution", Qp.mapRes, 0);

    // gd.addMessage("************* Head nodes **************");
    // gd.addChoice("Heads", headActions, headActions[0]);

    gd.setResizable(false);
    gd.showDialog();

    if (gd.wasCanceled()) {
      return false;
    }

    // Qp.scale = gd.getNextNumber();
    // Qp.setFPS(gd.getNextNumber());
    Qp.increment = (int) gd.getNextNumber();
    Qp.trackColor = gd.getNextChoice();
    Qp.outlinePlot = gd.getNextChoice();
    Qp.sumCov = gd.getNextNumber();
    Qp.avgCov = gd.getNextNumber();
    Qp.mapRes = (int) gd.getNextNumber();
    // Qp.headProcessing = gd.getNextChoice();

    return true;
  }

  private void closeAllImages() {
    int[] ids = ij.WindowManager.getIDList();
    for (int i = 0; i < ids.length; i++) {
      ij.WindowManager.getImage(ids[i]).close();
    }
  }
}
