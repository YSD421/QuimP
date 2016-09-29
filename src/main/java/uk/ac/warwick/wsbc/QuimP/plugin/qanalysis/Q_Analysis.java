package uk.ac.warwick.wsbc.QuimP.plugin.qanalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import uk.ac.warwick.wsbc.QuimP.FormatConverter;
import uk.ac.warwick.wsbc.QuimP.OutlineHandler;
import uk.ac.warwick.wsbc.QuimP.QColor;
import uk.ac.warwick.wsbc.QuimP.QParams;
import uk.ac.warwick.wsbc.QuimP.QParamsQconf;
import uk.ac.warwick.wsbc.QuimP.QuimpConfigFilefilter;
import uk.ac.warwick.wsbc.QuimP.QuimpException;
import uk.ac.warwick.wsbc.QuimP.filesystem.DataContainer;
import uk.ac.warwick.wsbc.QuimP.filesystem.QconfLoader;
import uk.ac.warwick.wsbc.QuimP.plugin.ecmm.ECMM_Mapping;
import uk.ac.warwick.wsbc.QuimP.utils.QuimpToolsCollection;
import uk.ac.warwick.wsbc.QuimP.utils.graphics.svg.SVGplotter;

/**
 * Run Q analysis for ECMM data.
 * <p>
 * In principle this object loads and process name_X.paQP file generated by ECMM.  <i>X</i> in this
 * case means number of cell outline. The same result can be achieved by loading QCONF file that 
 * contains all outlines for given case. This class is designed to process one outline in one time.
 * Thus most of methods operate on current status private fields (such as \c qp, \c oH). 
 * The main analysis runner method is {@link #run()}, whereas support for both input formats is 
 * covered by {@link #runFromPAQP()} and {@link #runFromQCONF()} (similarly to 
 * {@link ECMM_Mapping ECMM_Mapping}) 
 *  
 * @author rtyson
 * @author p.baniukiewicz
 */
public class Q_Analysis {
    static {
        if (System.getProperty("quimp.debugLevel") == null)
            Configurator.initialize(null, "log4j2_default.xml");
        else
            Configurator.initialize(null, System.getProperty("quimp.debugLevel"));
    }
    private static final Logger LOGGER = LogManager.getLogger(Q_Analysis.class.getName());
    GenericDialog gd;
    private OutlineHandler oH; // keep loaded handler, can change during run
    private QconfLoader qconfLoader;
    private STmap stMap; // object holding all maps evaluated for current OutlineHandler (oH)

    /**
     * Main constructor and runner - class entry point.
     * <p>
     * Left in this form for backward compatibility
     */
    public Q_Analysis() {
        this(null);
    }

    /**
     * Parameterized constructor for tests.
     * 
     * @param paramFile paQP or QCONF file to process. If <tt>null</tt> user is asked for this file
     * @see uk.ac.warwick.wsbc.QuimP.ECMM_Mapping.ECMM_Mapping(File)
     */
    public Q_Analysis(File paramFile) {
        about();
        IJ.showStatus("QuimP Analysis");
        try {
            qconfLoader = new QconfLoader(paramFile); // load file
            if (qconfLoader == null || qconfLoader.getQp() == null)
                return; // failed to load exit
            // show dialog
            if (!showDialog()) { // if user cancelled dialog
                return; // do nothing
            }
            if (qconfLoader.getConfVersion() == QParams.QUIMP_11) { // old path
                QParams qp;
                runFromPAQP();
                File[] otherPaFiles = qconfLoader.getQp().findParamFiles();
                if (otherPaFiles.length > 0) { // and process them if they are (that pointed by
                                               // user is skipped)
                    YesNoCancelDialog yncd =
                            new YesNoCancelDialog(IJ.getInstance(), "Batch Process?",
                                    "\tBatch Process?\n\n" + "Process other "
                                            + QuimpConfigFilefilter.oldFileExt
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
                            qp = new QParams(paramFile);
                            qp.readParams();
                            Qp.setup(qp);
                            oH = new OutlineHandler(qp); // prepare current OutlineHandler
                            if (!oH.readSuccess) {
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
                qconfLoader.getECMM(); // will throw exception if not present
                if (qconfLoader.isQPresent()) {
                    YesNoCancelDialog ync;
                    ync = new YesNoCancelDialog(IJ.getInstance(), "Overwrite",
                            "You are about to override previous Q results. Is it ok?");
                    if (!ync.yesPressed()) // if no or cancel
                    {
                        IJ.log("No changes done in input file.");
                        return; // end}
                    }
                }
                runFromQCONF();
                IJ.log("The new data file " + paramFile.getName()
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
     * <p>
     * Saves updated QCONF.
     * <p>
     * <b>Warning</b><p>
     * {@link #run()} updates also {@link DataContainer#ECMMState ECMMState} by modifying fields in
     * Outlines that are accessed by reference here. 
     * 
     * @throws QuimpException when saving failed or there is no ECMM data in file.
     * @throws IOException On problem with file writing
     */
    private void runFromQCONF() throws QuimpException, IOException {
        int i = 0;
        Iterator<OutlineHandler> oI =
                qconfLoader.getQp().getLoadedDataContainer().getECMMState().oHs.iterator();
        ArrayList<STmap> tmp = new ArrayList<>();
        while (oI.hasNext()) {
            ((QParamsQconf) qconfLoader.getQp()).setActiveHandler(i++); // set current handler
                                                                        // number.
            Qp.setup(qconfLoader.getQp()); // copy selected data from general QParams to local
                                           // storage
            oH = oI.next();
            run();
            tmp.add(new STmap(stMap));
        }
        qconfLoader.getQp().getLoadedDataContainer().QState = tmp.toArray(new STmap[0]);
        qconfLoader.getQp().writeParams(); // save global container
        // generate additional OLD files
        FormatConverter fC = new FormatConverter(qconfLoader,
                ((QParamsQconf) qconfLoader.getQp()).getParamFile().toPath());
        fC.generateOldDataFiles();
    }

    /**
     * Run Q Analysis if input was paQP file.
     * 
     * @throws QuimpException when OutlineHandler can not be read
     */
    private void runFromPAQP() throws QuimpException {
        Qp.setup(qconfLoader.getQp()); // copy selected data from general QParams to local storage
        oH = new OutlineHandler(qconfLoader.getQp()); // load data from file
        if (!oH.readSuccess) {
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
     * Main runner - do all calculations on current OutlineHandler object
     * 
     * @warning Process current \a OutlineHandler oH; object and modify it filling some fields from 
     * Vertex class
     */
    private void run() {
        if (oH.getSize() == 1) {
            Qp.singleImage = true;
            // only one frame - re lable node indices
            oH.getOutline(1).resetAllCoords();
        }

        Qp.convexityToPixels();

        stMap = new STmap(oH, Qp.mapRes);

        SVGplotter svgPlotter = new SVGplotter(oH, Qp.fps, Qp.scale, Qp.channel, Qp.outFile);
        svgPlotter.plotTrack(Qp.trackColor, Qp.increment);
        // svgPlotter.plotTrackAnim();
        svgPlotter.plotTrackER(Qp.outlinePlot);

        Qp.convexityToUnits(); // reset the covexity options to units (as they are static)
    }

    private boolean showDialog() {
        gd = new GenericDialog("Q Analysis Options", IJ.getInstance());

        gd.setOKLabel("RUN");

        gd.addMessage("Pixel width: " + Qp.scale + " \u00B5m\nFrame Interval: " + Qp.frameInterval
                + " sec");

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

/**
 * Configuration class for Q_Analysis
 * 
 * @author rtyson
 *
 */
class Qp {

    static public File snQPfile;
    static public File stQPfile;
    static public File outFile;
    static public String filename;
    static public double scale = 1; // pixel size in microns
    static public double frameInterval = 1; // frames per second
    static int startFrame, endFrame;
    static public double fps = 1; // frames per second
    static public int increment = 1;
    static public String trackColor;
    static public String[] outlinePlots = { "Speed", "Fluorescence", "Convexity" };
    static public String outlinePlot;
    static public double sumCov = 1;
    static public double avgCov = 0;
    static public int mapRes = 400;
    static public int channel = 0;
    static boolean singleImage = false;
    static boolean useDialog = true;
    //static public String headProcessing; //!< Head processing algorithm. Define how to treat head position */
    final static boolean Build3D = false;

    static void convexityToPixels() {
        avgCov /= scale; // convert to pixels
        sumCov /= scale;
    }

    static void convexityToUnits() {
        avgCov *= scale; // convert to pixels
        sumCov *= scale;
    }

    public Qp() {
    }

    /**
     * Copies selected data from QParams to this object
     * 
     * @param qp General QuimP parameters object
     */
    static void setup(QParams qp) {
        Qp.snQPfile = qp.getSnakeQP();
        Qp.scale = qp.getImageScale();
        Qp.frameInterval = qp.getFrameInterval();
        Qp.filename = QuimpToolsCollection.removeExtension(Qp.snQPfile.getName());
        Qp.outFile = new File(Qp.snQPfile.getParent() + File.separator + Qp.filename);
        Qp.startFrame = qp.getStartFrame();
        Qp.endFrame = qp.getEndFrame();
        // File p = qp.paramFile;
        fps = 1d / frameInterval;
        singleImage = false;
        useDialog = true;
    }
}
