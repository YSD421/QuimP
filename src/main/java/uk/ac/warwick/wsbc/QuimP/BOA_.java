package uk.ac.warwick.wsbc.QuimP;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point2d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.YesNoCancelDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.FloatPolygon;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import uk.ac.warwick.wsbc.QuimP.BOA_.BOAState;
import uk.ac.warwick.wsbc.QuimP.BOAp.SegParam;
import uk.ac.warwick.wsbc.QuimP.SnakePluginList.Plugin;
import uk.ac.warwick.wsbc.QuimP.geom.ExtendedVector2d;
import uk.ac.warwick.wsbc.QuimP.plugin.IQuimpPlugin;
import uk.ac.warwick.wsbc.QuimP.plugin.QuimpPluginException;
import uk.ac.warwick.wsbc.QuimP.plugin.snakes.IQuimpPoint2dFilter;

/**
 * Main class implementing BOA plugin.
 * 
 * @author Richard Tyson
 * @author Till Bretschneider
 * @author Piotr Baniukiewicz
 * @date 16-04-09
 * @date 4 Feb 2016
 */
public class BOA_ implements PlugIn {
    // http://stackoverflow.com/questions/21083834/load-log4j2-configuration-file-programmatically
    static {
        System.setProperty("log4j.configurationFile", "qlog4j2.xml");
    }
    private static final Logger LOGGER = LogManager.getLogger(BOA_.class.getName());
    CustomCanvas canvas;
    CustomStackWindow window;
    static TextArea logArea;
    static boolean running = false;
    ImageGroup imageGroup;
    private Nest nest;
    // private int frame; // current frame, CustomStackWindow.updateSliceSelector()
    private Constrictor constrictor;
    private PluginFactory pluginFactory; // load and maintain plugins
    private String lastTool; // last selection tool selected in IJ remember last tool to reselect
                             // it after truncating or deleting operation
    private final static String NONE = "NONE"; // reserved word that stands for plugin that is not
                                               // selected
    private ViewUpdater viewUpdater; // hold current BOA object and provide access to only one
                                     // method from plugin. Reference to this field is passed to
                                     // plugins and give them possibility to call selected methods
                                     // from BOA class
    public static String[] quimpInfo; // keeps data from getQuimPBuildInfo() These information are
                                      // used for About dialog, window title bar, logging
    private static int logCount = 1; // adds counter to logged messages
    static final private int NUM_SNAKE_PLUGINS = 3; /*!< number of Snake plugins  */
    private HistoryLogger historyLogger; // logger

    static final public BOAp boap = new BOAp(); // configuration object, available from all modules
    private BOAState boaState; // current state of BOA module

    /**
     * Hold current BOA state that can be serialized
     * 
     * @author p.baniukiewicz
     * @date 30 Mar 2016
     * @see Serializer
     * @remarks Currently the SegParam object is connected to this class only and it is created in
     * BOAp class constructor. In future SegParam will be part of this class
     * This class in supposed to be main configuration holder for BOA_
     */
    class BOAState implements IQuimpSerialize {
        public int frame; /*!< current frame, CustomStackWindow.updateSliceSelector() */
        public SegParam segParam; /*!< Reference to segmentation parameters */
        public String fileName; /*!< Current data file name */
        /**
         * List of plugins selected in plugin stack and information if they are active or not
         * This field is serializable.
         * 
         * @see SnakePluginList
         * @see uk.ac.warwick.wsbc.QuimP.BOA_.run(final String)
         */
        public SnakePluginList snakePluginList;

        /**
         * Should be called before serialization. Fills extra fields from BOAp
         */
        @Override
        public void beforeSerialize() {
            fileName = boap.fileName; // copy filename from system wide boap
            snakePluginList.beforeSerialize(); // download plugins configurations
        }

        @Override
        public void afterSerialize() throws Exception {
        }
    }

    /**
     * Main method called from Fiji. Initializes internal BOA structures.
     * 
     * @param arg Currently it can be string pointing to plugins directory
     * @see uk.ac.warwick.wsbc.QuimP.BOA_.setup(final ImagePlus)
     */
    @Override
    public void run(final String arg) {
        if (IJ.versionLessThan("1.45")) {
            return;
        }
        boaState = new BOAState(); // create BOA state machine
        boaState.segParam = boap.segParam; // assign reference of segmentation parameters to state
                                           // machine
        if (IJ.getVersion().compareTo("1.46") < 0) {
            boap.useSubPixel = false;
        } else {
            boap.useSubPixel = true;
        }
        if (BOA_.running) {
            BOA_.running = false;
            IJ.error("Warning: Only have one instance of BOA running at a time");
            return;
        }
        // assign current object to ViewUpdater
        viewUpdater = new ViewUpdater(this);
        // collect information about quimp version read from jar
        quimpInfo = getQuimPBuildInfo();
        // create history logger
        historyLogger = new HistoryLogger();

        ImagePlus ip = WindowManager.getCurrentImage();
        lastTool = IJ.getToolName();
        // stack or single image?
        if (ip == null) {
            IJ.error("Image required");
            return;
        } else if (ip.getStackSize() == 1) {
            boap.singleImage = true;
        } else {
            boap.singleImage = false;
        }
        // check if 8-bit image
        if (ip.getType() != ImagePlus.GRAY8) {
            YesNoCancelDialog ync = new YesNoCancelDialog(window, "Image bit depth",
                    "8-bit Image required. Convert?");
            if (ync.yesPressed()) {
                if (boap.singleImage) {
                    new ImageConverter(ip).convertToGray8();
                } else {
                    new StackConverter(ip).convertToGray8();
                }
            } else {
                return;
            }
        }

        // Build plugin engine
        try {
            String path = IJ.getDirectory("plugins");
            if (path == null) {
                IJ.log("BOA: Plugin directory not found");
                LOGGER.warn("BOA: Plugin directory not found, use provided with arg: " + arg);
                path = arg;
            }
            // initialize plugin factory (jar scanning and registering)
            pluginFactory = new PluginFactory(Paths.get(path));
            // initialize arrays for plugins instances and give them initial values (GUI)
            boaState.snakePluginList =
                    new SnakePluginList(NUM_SNAKE_PLUGINS, pluginFactory, null, viewUpdater);
        } catch (Exception e) {
            // temporary catching may in future be removed
            LOGGER.error("run " + e);
        }

        BOA_.running = true;
        setup(ip);

        if (boap.useSubPixel == false) {
            BOA_.log("Upgrade to ImageJ 1.46, or higher," + "\nto get sub-pixel editing.");
        }
        if (IJ.getVersion().compareTo("1.49a") > 0) {
            BOA_.log("(ImageJ " + IJ.getVersion() + " untested)");
        }

        try {
            if (!nest.isVacant()) {
                runBoa(1, 1);
            }
        } catch (BoaException be) {
            BOA_.log("RUNNING BOA...inital preview failed");
            BOA_.log(be.getMessage());
            be.printStackTrace();
        }
    }

    /**
     * Build all BOA windows and setup initial parameters for segmentation
     * Define also windowListener for cleaning after closing the main window by
     * user.
     * 
     * @param ip Reference to image to be processed by BOA
     * @see BOAp
     */
    void setup(final ImagePlus ip) {
        if (boap.paramsExist == null) {
            boap.segParam.setDefaults();
        }
        boap.setup(ip);

        nest = new Nest();
        imageGroup = new ImageGroup(ip, nest);
        boaState.frame = 1;
        // build window and set its title
        canvas = new CustomCanvas(imageGroup.getOrgIpl());
        window = new CustomStackWindow(imageGroup.getOrgIpl(), canvas);
        window.buildWindow();
        window.setTitle(window.getTitle() + " :QuimP: " + quimpInfo[0]);
        // warn about scale
        if (boap.scaleAdjusted) {
            BOA_.log("WARNING Scale was zero - set to 1");
        }
        if (boap.fIAdjusted) {
            BOA_.log("WARNING Frame interval was zero - set to 1");
        }

        // adds window listener called on plugin closing
        window.addWindowListener(new CustomWindowAdapter());

        setScales();
        updateImageScale();
        window.setScalesText();

        // check for ROIs - Use as cells
        new RoiManager(); // get open ROI manager, or create a new one
        RoiManager rm = RoiManager.getInstance();
        if (rm.getRoisAsArray().length != 0) {
            nest.addHandlers(rm.getRoisAsArray(), 1);
        } else {
            BOA_.log("No cells from ROI manager");
            if (ip.getRoi() != null) {
                nest.addHandler(ip.getRoi(), 1);
            } else {
                BOA_.log("No cells from selection");
            }
        }
        rm.close();
        ip.killRoi();

        constrictor = new Constrictor(); // does computations on snakes
    }

    /**
     * Display about information in BOA window. 
     * 
     * Called from menu bar. Reads also information from all found plugins.
     */
    void about() {
        String authors = "###################################\n" + "BOA plugin, by\n"
                + "Richard Tyson (richard.tyson@warwick.ac.uk)\n"
                + "Till Bretschneider (Till.Bretschneider@warwick.ac.uk)\n"
                + "###################################\n";

        // build modal dialog
        Dialog aboutWnd = new Dialog(window, "Info", Dialog.ModalityType.DOCUMENT_MODAL);
        aboutWnd.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                aboutWnd.dispose();
            }
        });
        // located in middle of quimp qindow
        Rectangle orgBounds = window.getBounds();
        aboutWnd.setBounds(orgBounds.x + orgBounds.width / 2, orgBounds.y + orgBounds.height / 2,
                500, 300);
        Panel p = new Panel();
        p.setLayout(new GridLayout(1, 1)); // main window panel
        Panel tp = new Panel(); // panel with text area
        tp.setLayout(new GridLayout(1, 1));
        TextArea info = new TextArea(10, 60); // area to write
        // fill with information
        info.append(authors + '\n');
        info.append("QuimP version: " + quimpInfo[0] + '\n');
        info.append(quimpInfo[1] + '\n');
        // get list of found plugins
        info.append("List of found plugins:\n");
        Map<String, PluginProperties> mp = pluginFactory.getRegisterdPlugins();
        // iterate over set
        for (Map.Entry<String, PluginProperties> entry : mp.entrySet()) {
            info.append("\n");
            info.append("Plugin name: " + entry.getKey() + "\n");
            info.append("   Plugin type: " + entry.getValue().getType() + "\n");
            info.append("   Plugin path: " + entry.getValue().getFile().toString() + "\n");
            info.append("   Plugin vers: " + entry.getValue().getVersion() + "\n");
        }
        info.setEditable(false);
        tp.add(info); // add to panel
        JScrollPane infoPanel = new JScrollPane(tp);
        p.add(infoPanel);

        aboutWnd.add(p);
        aboutWnd.pack();
        aboutWnd.setVisible(true);
    }

    /**
     * Get build info read from jar file
     * 
     * @return Formatted strings with build info and version:
     * -# [0] - contains only version string read from \a MANIFEST.MF
     * -# [1] - contains formatted string with build time and name of builder read from \a MANIFEST.MF
     * -# [2] - contains software name read from \a MANIFEST.MF
     * @warning This method is jar-name dependent - looks for manifest with \a Implementation-Title
     * that contains \c QuimP string.
     */
    public String[] getQuimPBuildInfo() {
        String[] ret = new String[3];
        ret[0] = "version not found in jar";
        ret[1] = "build info not found in jar";
        ret[2] = "name not found in jar";
        try {
            Enumeration<URL> resources =
                    getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                Attributes attributes = manifest.getMainAttributes();
                try {
                    String val = attributes.getValue("Implementation-Title");
                    if (val == null)
                        continue;
                    // name dependent part
                    if (attributes.getValue("Implementation-Title").contains("QuimP")) {
                        ret[1] = "Build by: " + attributes.getValue("Built-By") + " on: "
                                + attributes.getValue("Implementation-Build");
                        ret[0] = attributes.getValue("Implementation-Version");
                        ret[2] = attributes.getValue("Implementation-Title");
                        LOGGER.debug(ret);
                    }
                } catch (Exception e) {
                    ; // do not care about problems - just use defaults defined on beginning
                }
            }
        } catch (IOException e) {
            ; // do not care about problems - just use defaults defined on beginning
        }
        return ret;
    }

    /**
     * Append string to log window in BOA plugin
     * 
     * @param s String to display in BOA window
     */
    static void log(final String s) {
        logArea.append("[" + logCount++ + "] " + s + '\n');
    }

    /**
     * Redraw current view. Process outlines by all active plugins. Do not run segmentation again
     * Updates \c liveSnake.
     */
    public void recalculatePlugins() {
        LOGGER.trace("BOA: recalculatePlugins called");
        SnakeHandler sH;
        if (nest.isVacant())
            return;
        imageGroup.clearPaths(boaState.frame);
        imageGroup.setProcessor(boaState.frame);
        imageGroup.setIpSliceAll(boaState.frame);
        try {
            for (int s = 0; s < nest.size(); s++) { // for each snake
                sH = nest.getHandler(s);
                if (boaState.frame < sH.getStartframe()) // if snake does not exist on current frame
                    continue;
                // but if one is on frame f+n and strtFrame is e.g. 1 it may happen that there is
                // no continuity of this snake between frames. In this case getBackupSnake
                // returns null. In general QuimP assumes that if there is a cell on frame f, it
                // will exist on all consecutive frames.
                Snake snake = sH.getBackupSnake(boaState.frame); // if exist get its backup copy
                                                                 // (segm)
                if (snake == null || !snake.alive) // if not alive
                    continue;
                try {
                    Snake out = iterateOverSnakePlugins(snake); // apply all plugins to snake
                    sH.storeThisSnake(out, boaState.frame); // set processed snake as final
                } catch (QuimpPluginException qpe) {
                    // must be rewritten with whole runBOA #65 #67
                    BOA_.log("Error in filter module: " + qpe.getMessage());
                    LOGGER.error(qpe);
                    sH.storeLiveSnake(boaState.frame); // so store only segmented snake as final
                }
            }
        } catch (Exception e) {
            LOGGER.error("Can not update view. Output snake may be defective: " + e.getMessage());
            LOGGER.error(e);
        } finally {
            historyLogger.addEntry("Plugin settings", boaState);
        }
        imageGroup.updateOverlay(boaState.frame);
    }

    /**
     * Override action performed on window closing. Clear BOA._running static
     * variable and prevent to notify user that QuimP is running when it has
     * been closed and called again.
     * 
     * @bug
     * When user closes window by system button QuimP does not ask for
     * saving current work. This is because by default QuimP window is
     * managed by ImageJ and it \a probably only hides it on closing
     * 
     * @remarks
     * This class could be located directly in CustomStackWindow which is 
     * included in BOA_. But it need to have access to BOA field \c running.
     * 
     * @author p.baniukiewicz
     */
    class CustomWindowAdapter extends WindowAdapter {
        @Override
        // This method will be called when BOA_ window is closed already
        // It is too late for asking user
        public void windowClosed(final WindowEvent arg0) {
            LOGGER.trace("CLOSED");
            BOA_.running = false;
            canvas = null;
            imageGroup = null;
            window = null;
        }

        @Override
        public void windowClosing(final WindowEvent arg0) {
            LOGGER.trace("CLOSING");
        }

        @Override
        public void windowActivated(final WindowEvent e) {
            LOGGER.trace("ACTIVATED");
            // rebuild manu for this local window
            // workaround for Mac and theirs menus om top screen bar
            // IJ is doing the same for activation of its window so every time one has correct menu
            // on top
            window.setMenuBar(window.quimpMenuBar);
        }
    }

    /**
     * Supports mouse actions on image at QuimP window according to selected
     * option
     * 
     * @author rtyson
     *
     */
    @SuppressWarnings("serial")
    class CustomCanvas extends ImageCanvas {

        /**
         * Empty constructor
         * 
         * @param imp Reference to image loaded by BOA
         */
        CustomCanvas(final ImagePlus imp) {
            super(imp);
        }

        /**
         * @deprecated Actually not used in this version of QuimP
         */
        @Override
        public void paint(final Graphics g) {
            super.paint(g);
            // int size = 80;
            // int screenSize = (int)(size*getMagnification());
            // int x = screenX(imageWidth/2 - size/2);
            // int y = screenY(imageHeight/2 - size/2);
            // g.setColor(Color.red);
            // g.drawOval(x, y, screenSize, screenSize);
        }

        /**
         * Implement mouse action on image loaded to BOA Used for manual
         * editions of segmented shape. Define reactions of mouse buttons
         * according to GUI state, set by \b Delete and \b Edit buttons.
         * 
         * @see BOAp
         * @see CustomStackWindow
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            super.mousePressed(e);
            if (boap.doDelete) {
                // BOA_.log("Delete at:
                // ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")");
                deleteCell(offScreenX(e.getX()), offScreenY(e.getY()), boaState.frame);
                IJ.setTool(lastTool);
            }
            if (boap.doDeleteSeg) {
                // BOA_.log("Delete at:
                // ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")");
                deleteSegmentation(offScreenX(e.getX()), offScreenY(e.getY()), boaState.frame);
            }
            if (boap.editMode && boap.editingID == -1) {
                // BOA_.log("Delete at:
                // ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")");
                editSeg(offScreenX(e.getX()), offScreenY(e.getY()), boaState.frame);
            }
        }
    } // end of CustomCanvas

    /**
     * Extends standard ImageJ StackWindow adding own GUI elements.
     * 
     * This class stands for definition of main BOA plugin GUI window. Current
     * state of BOA plugin is stored at {@link BOAp} class.
     * 
     * @author rtyson
     * @see BOAp
     */
    @SuppressWarnings("serial")
    class CustomStackWindow extends StackWindow
            implements ActionListener, ItemListener, ChangeListener {

        final static int DEFAULT_SPINNER_SIZE = 5;
        final static int SNAKE_PLUGIN_NUM = 3; /*!< number of currently supported plugins  */
        private Button bFinish, bSeg, bLoad, bEdit, bDefault, bScale;
        private Button bAdd, bDel, bDelSeg, bQuit;
        private Checkbox cPrevSnake, cExpSnake, cPath, cZoom;
        JScrollPane logPanel;
        Label fpsLabel, pixelLabel, frameLabel;
        JSpinner dsNodeRes, dsVel_crit, dsF_image, dsF_central, dsF_contract, dsFinalShrink;
        JSpinner isMaxIterations, isBlowup, isSample_tan, isSample_norm;
        private Choice firstPluginName, secondPluginName, thirdPluginName;
        private Button bFirstPluginGUI, bSecondPluginGUI, bThirdPluginGUI;
        private Checkbox cFirstPlugin, cSecondPlugin, cThirdPlugin;

        private MenuBar quimpMenuBar;
        private MenuItem menuVersion, menuSaveConfig, menuLoadConfig, menuShowHistory; // items
        private CheckboxMenuItem cbMenuPlotOriginalSnakes;

        /**
         * Default constructor
         * 
         * @param imp Image loaded to plugin
         * @param ic Image canvas
         */
        CustomStackWindow(final ImagePlus imp, final ImageCanvas ic) {
            super(imp, ic);

        }

        /**
         * Build user interface.
         * 
         * This method is called as first. The interface is built in three steps:
         * Left side of window (configuration zone) and right side of main
         * window (logs and other info and buttons) and finally upper menubar
         */
        private void buildWindow() {

            setLayout(new BorderLayout(10, 3));

            if (!boap.singleImage) {
                remove(sliceSelector);
            }
            if (!boap.singleImage) {
                remove(this.getComponent(1)); // remove the play/pause button
            }
            Panel cp = buildControlPanel();
            Panel sp = buildSetupPanel();
            add(new Label(""), BorderLayout.NORTH);
            add(cp, BorderLayout.WEST); // add to the left, position 0
            add(ic, BorderLayout.CENTER);
            add(sp, BorderLayout.EAST);
            add(new Label(""), BorderLayout.SOUTH);

            LOGGER.debug("Menu: " + getMenuBar());
            quimpMenuBar = buildMenu(); // store menu in var to reuse on window activation
            setMenuBar(quimpMenuBar);
            pack();

        }

        /**
         * Build window menu.
         * 
         * Menu is local for this window of QuimP and it is stored in \c quimpMenuBar variable.
         * On every time when QuimP is active, this menu is restored in 
         * uk.ac.warwick.wsbc.QuimP.BOA_.CustomWindowAdapter.windowActivated(WindowEvent) method
         * This is due to overwriting menu by IJ on Mac (all menus are on top screen bar)
         * @return Reference to menu bar
         */
        final MenuBar buildMenu() {
            MenuBar menuBar; // main menu bar
            Menu menuAbout; // menu About in menubar
            Menu menuConfig; // menu Config in menubar

            // if (getMenuBar() != null)
            // menuBar = getMenuBar();
            // else
            menuBar = new MenuBar();

            menuConfig = new Menu("Preferences");

            menuAbout = new Menu("About");

            // build main line
            menuBar.add(menuConfig);
            menuBar.add(menuAbout);

            // add entries
            menuVersion = new MenuItem("Version");
            menuVersion.addActionListener(this);
            menuAbout.add(menuVersion);

            cbMenuPlotOriginalSnakes = new CheckboxMenuItem("Plot original");
            cbMenuPlotOriginalSnakes.setState(boap.isProcessedSnakePlotted);
            cbMenuPlotOriginalSnakes.addItemListener(this);
            menuConfig.add(cbMenuPlotOriginalSnakes);

            menuSaveConfig = new MenuItem("Save preferences");
            menuSaveConfig.addActionListener(this);
            menuConfig.add(menuSaveConfig);

            menuLoadConfig = new MenuItem("Load preferences");
            menuLoadConfig.addActionListener(this);
            menuConfig.add(menuLoadConfig);

            menuShowHistory = new MenuItem("Show history");
            menuShowHistory.addActionListener(this);
            menuConfig.add(menuShowHistory);

            return menuBar;
        }

        /**
         * Build right side of main BOA window
         * 
         * @return Reference to panel
         */
        final Panel buildSetupPanel() {
            Panel setupPanel = new Panel(); // Main panel comprised from North, Centre and South
                                            // subpanels
            Panel northPanel = new Panel(); // Contains static info and four buttons (Scale,
                                            // Truncate, Add, Delete)
            Panel southPanel = new Panel(); // Quit and Finish
            Panel centerPanel = new Panel();
            Panel pluginPanel = new Panel();

            setupPanel.setLayout(new BorderLayout());
            northPanel.setLayout(new GridLayout(3, 2));
            southPanel.setLayout(new GridLayout(2, 2));
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

            // Grid bag for plugin zone
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 0.5;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_START;
            pluginPanel.setLayout(gridbag);

            fpsLabel = new Label("F Interval: " + IJ.d2s(boap.imageFrameInterval, 3) + " s");
            northPanel.add(fpsLabel);
            pixelLabel = new Label("Scale: " + IJ.d2s(boap.imageScale, 6) + " \u00B5m");
            northPanel.add(pixelLabel);

            bScale = addButton("Set Scale", northPanel);
            bDelSeg = addButton("Truncate Seg", northPanel);
            bAdd = addButton("Add cell", northPanel);
            bDel = addButton("Delete cell", northPanel);

            // build subpanel with plugins
            // get plugins names collected by PluginFactory
            ArrayList<String> pluginList =
                    boaState.snakePluginList.getPluginNames(IQuimpPlugin.DOES_SNAKES);
            // add NONE to list
            pluginList.add(0, NONE);

            // plugins are recognized by their names returned from pluginFactory.getPluginNames() so
            // if there is no names, it is not possible to call nonexisting plugins, because calls
            // are made using plugin names. see actionPerformed. If plugin of given name (NONE) is
            // not found getInstance return null which is stored in SnakePluginList and checked
            // during run
            // default values for plugin activity are stored in SnakePluginList
            firstPluginName = addComboBox(pluginList.toArray(new String[0]), pluginPanel);
            c.gridx = 0;
            c.gridy = 0;
            pluginPanel.add(firstPluginName, c);

            bFirstPluginGUI = addButton("GUI", pluginPanel);
            c.gridx = 1;
            c.gridy = 0;
            pluginPanel.add(bFirstPluginGUI, c);

            cFirstPlugin = addCheckbox("A", pluginPanel, boaState.snakePluginList.isActive(0));
            c.gridx = 2;
            c.gridy = 0;
            pluginPanel.add(cFirstPlugin, c);

            secondPluginName = addComboBox(pluginList.toArray(new String[0]), pluginPanel);
            c.gridx = 0;
            c.gridy = 1;
            pluginPanel.add(secondPluginName, c);

            bSecondPluginGUI = addButton("GUI", pluginPanel);
            c.gridx = 1;
            c.gridy = 1;
            pluginPanel.add(bSecondPluginGUI, c);

            cSecondPlugin = addCheckbox("A", pluginPanel, boaState.snakePluginList.isActive(1));
            c.gridx = 2;
            c.gridy = 1;
            pluginPanel.add(cSecondPlugin, c);

            thirdPluginName = addComboBox(pluginList.toArray(new String[0]), pluginPanel);
            c.gridx = 0;
            c.gridy = 2;
            pluginPanel.add(thirdPluginName, c);

            bThirdPluginGUI = addButton("GUI", pluginPanel);
            c.gridx = 1;
            c.gridy = 2;
            pluginPanel.add(bThirdPluginGUI, c);

            cThirdPlugin = addCheckbox("A", pluginPanel, boaState.snakePluginList.isActive(2));
            c.gridx = 2;
            c.gridy = 2;
            pluginPanel.add(cThirdPlugin, c);

            // --------build log---------
            Panel tp = new Panel(); // panel with text area
            tp.setLayout(new GridLayout(1, 1));
            logArea = new TextArea(15, 15);
            logArea.setEditable(false);
            tp.add(logArea);
            logPanel = new JScrollPane(tp);

            // ------------------------------

            // --------build south--------------
            southPanel.add(new Label("")); // blankes
            southPanel.add(new Label("")); // blankes
            bQuit = addButton("Quit", southPanel);
            bFinish = addButton("FINISH", southPanel);
            // ------------------------------

            centerPanel.add(new Label("Snake Plugins:"));
            centerPanel.add(pluginPanel);
            centerPanel.add(new Label("Logs:"));
            centerPanel.add(logPanel);
            setupPanel.add(northPanel, BorderLayout.PAGE_START);
            setupPanel.add(centerPanel, BorderLayout.CENTER);
            setupPanel.add(southPanel, BorderLayout.PAGE_END);

            if (pluginList.isEmpty())
                BOA_.log("No plugins found");
            else
                BOA_.log("Found " + (pluginList.size() - 1) + " plugins (see About)");

            return setupPanel;
        }

        /**
         * Build left side of main BOA window.
         * 
         * @return Reference to built panel
         */
        final Panel buildControlPanel() {
            Panel controlPanel = new Panel();
            Panel topPanel = new Panel();
            Panel paramPanel = new Panel();
            Panel bottomPanel = new Panel();

            controlPanel.setLayout(new BorderLayout());
            topPanel.setLayout(new GridLayout(1, 2));
            paramPanel.setLayout(new GridLayout(14, 1));
            bottomPanel.setLayout(new GridLayout(1, 2));

            // --------build topPanel--------
            bLoad = addButton("Load", topPanel);
            bDefault = addButton("Default", topPanel);
            // -----------------------

            // --------build paramPanel--------------
            dsNodeRes = addDoubleSpinner("Node Spacing:", paramPanel, boap.segParam.getNodeRes(),
                    1., 20., 0.2, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            isMaxIterations =
                    addIntSpinner("Max Iterations:", paramPanel, boap.segParam.max_iterations, 100,
                            10000, 100, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            isBlowup = addIntSpinner("Blowup:", paramPanel, boap.segParam.blowup, 0, 200, 2,
                    CustomStackWindow.DEFAULT_SPINNER_SIZE);
            dsVel_crit = addDoubleSpinner("Crit velocity:", paramPanel, boap.segParam.vel_crit,
                    0.0001, 2., 0.001, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            dsF_image = addDoubleSpinner("Image F:", paramPanel, boap.segParam.f_image, 0.01, 10.,
                    0.01, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            dsF_central = addDoubleSpinner("Central F:", paramPanel, boap.segParam.f_central,
                    0.0005, 1, 0.002, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            dsF_contract = addDoubleSpinner("Contract F:", paramPanel, boap.segParam.f_contract,
                    0.001, 1, 0.001, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            dsFinalShrink = addDoubleSpinner("Final Shrink:", paramPanel, boap.segParam.finalShrink,
                    -100, 100, 0.5, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            isSample_tan = addIntSpinner("Sample tan:", paramPanel, boap.segParam.sample_tan, 1, 30,
                    1, CustomStackWindow.DEFAULT_SPINNER_SIZE);
            isSample_norm = addIntSpinner("Sample norm:", paramPanel, boap.segParam.sample_norm, 1,
                    60, 1, CustomStackWindow.DEFAULT_SPINNER_SIZE);

            cPrevSnake = addCheckbox("Use Previouse Snake", paramPanel,
                    boap.segParam.use_previous_snake);
            cExpSnake = addCheckbox("Expanding Snake", paramPanel, boap.segParam.expandSnake);

            Panel segEditPanel = new Panel();
            segEditPanel.setLayout(new GridLayout(1, 2));
            bSeg = addButton("SEGMENT", segEditPanel);
            bEdit = addButton("Edit", segEditPanel);
            paramPanel.add(segEditPanel);

            // mini panel comprised from slice selector and frame number (if not single image)
            Panel sliderPanel = new Panel();
            sliderPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

            if (!boap.singleImage) {
                sliceSelector.setPreferredSize(new Dimension(165, 20));
                sliceSelector.addAdjustmentListener(this);
                sliderPanel.add(sliceSelector);
                // frame number on right of slice selector
                frameLabel = new Label(imageGroup.getOrgIpl().getSlice() + "  ");
                sliderPanel.add(frameLabel);
            }
            paramPanel.add(sliderPanel);
            // ----------------------------------

            // -----build bottom panel---------
            cPath = addCheckbox("Show paths", bottomPanel, boap.segParam.showPaths);
            cZoom = addCheckbox("Zoom cell", bottomPanel, boap.zoom);
            // -------------------------------
            // build control panel

            controlPanel.add(topPanel, BorderLayout.NORTH);
            controlPanel.add(paramPanel, BorderLayout.CENTER);
            controlPanel.add(bottomPanel, BorderLayout.SOUTH);

            return controlPanel;
        }

        /**
         * Helper method for adding buttons to UI. Creates UI element and adds it to panel
         * 
         * @param label Label on button
         * @param p Reference to the panel where button is located
         * @return Reference to created button
         */
        private Button addButton(final String label, final Container p) {
            Button b = new Button(label);
            b.addActionListener(this);
            p.add(b);
            return b;
        }

        /**
         * Helper method for creating checkbox in UI
         * 
         * @param label Label of checkbox
         * @param p Reference to the panel where checkbox is located
         * @param d Initial state of checkbox
         * @return Reference to created checkbox
         */
        private Checkbox addCheckbox(final String label, final Container p, boolean d) {
            Checkbox c = new Checkbox(label, d);
            c.addItemListener(this);
            p.add(c);
            return c;
        }

        /**
         * Helper method for creating ComboBox in UI. Creates UI element and adds it to panel
         *
         * @param s Strings to be included in ComboBox
         * @param mp Reference to the panel where ComboBox is located
         * @return Reference to created ComboBox
         */
        private Choice addComboBox(final String[] s, final Container mp) {
            Choice c = new Choice();
            for (String st : s)
                c.add(st);
            c.select(0);
            c.addItemListener(this);
            mp.add(c);
            return c;
        }

        /**
         * Helper method for creating spinner in UI with real values
         * 
         * @param s Label of spinner (added on its left side)
         * @param mp Reference of panel where spinner is located
         * @param d The current vale of model
         * @param min The first number in sequence
         * @param max The last number in sequence
         * @param step The difference between numbers in sequence
         * @param columns The number of columns preferred for display
         * @return Reference to created spinner
         */
        private JSpinner addDoubleSpinner(final String s, final Container mp, double d, double min,
                double max, double step, int columns) {
            SpinnerNumberModel model = new SpinnerNumberModel(d, min, max, step);
            JSpinner spinner = new JSpinner(model);
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
            spinner.addChangeListener(this);

            Panel p = new Panel();
            p.setLayout(new FlowLayout(FlowLayout.RIGHT));
            Label label = new Label(s);
            p.add(label);
            p.add(spinner);
            mp.add(p);
            return spinner;
        }

        /**
         * Helper method for creating spinner in UI with integer values
         * 
         * @param s Label of spinner (added on its left side)
         * @param mp Reference of panel where spinner is located
         * @param d The current vale of model
         * @param min The first number in sequence
         * @param max The last number in sequence
         * @param step The difference between numbers in sequence
         * @param columns The number of columns preferred for display
         * @return Reference to created spinner
         */
        private JSpinner addIntSpinner(final String s, final Container mp, int d, int min, int max,
                int step, int columns) {
            SpinnerNumberModel model = new SpinnerNumberModel(d, min, max, step);
            JSpinner spinner = new JSpinner(model);
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
            spinner.addChangeListener(this);

            Panel p = new Panel();
            p.setLayout(new FlowLayout(FlowLayout.RIGHT));
            Label label = new Label(s);
            p.add(label);
            p.add(spinner);
            mp.add(p);
            return spinner;
        }

        /**
         * Set default values defined in model class
         * {@link uk.ac.warwick.wsbc.QuimP.BOAp} and update UI
         * 
         * @see BOAp
         */
        private void setDefualts() {
            boap.segParam.setDefaults();
            updateSpinnerValues();
        }

        /**
         * Update spinners in BOA UI Update spinners according to values stored
         * in machine state {@link uk.ac.warwick.wsbc.QuimP.BOAp}
         * 
         * @see BOAp
         */
        private void updateSpinnerValues() {
            boap.supressStateChangeBOArun = true;
            dsNodeRes.setValue(boap.segParam.getNodeRes());
            dsVel_crit.setValue(boap.segParam.vel_crit);
            dsF_image.setValue(boap.segParam.f_image);
            dsF_central.setValue(boap.segParam.f_central);
            dsF_contract.setValue(boap.segParam.f_contract);
            dsFinalShrink.setValue(boap.segParam.finalShrink);
            isMaxIterations.setValue(boap.segParam.max_iterations);
            isBlowup.setValue(boap.segParam.blowup);
            isSample_tan.setValue(boap.segParam.sample_tan);
            isSample_norm.setValue(boap.segParam.sample_norm);
            boap.supressStateChangeBOArun = false;
        }

        /**
         * Update checkboxes
         * 
         * @see SnakePluginList
         * @see itemStateChanged(ItemEvent)
         */
        private void updateCheckBoxes() {
            // first plugin activity
            cFirstPlugin.setState(boaState.snakePluginList.isActive(0));
            // second plugin activity
            cSecondPlugin.setState(boaState.snakePluginList.isActive(1));
            // third plugin activity
            cThirdPlugin.setState(boaState.snakePluginList.isActive(2));
        }

        /**
         * Update Choices
         * 
         * @see SnakePluginList
         * @see itemStateChanged(ItemEvent)
         */
        private void updateChoices() {
            // first slot snake plugin
            if (boaState.snakePluginList.getInstance(0) == null)
                firstPluginName.select(NONE);
            else
                firstPluginName.select(boaState.snakePluginList.getName(0));
            // second slot snake plugin
            if (boaState.snakePluginList.getInstance(1) == null)
                secondPluginName.select(NONE);
            else
                secondPluginName.select(boaState.snakePluginList.getName(1));
            // third slot snake plugin
            if (boaState.snakePluginList.getInstance(2) == null)
                thirdPluginName.select(NONE);
            else
                thirdPluginName.select(boaState.snakePluginList.getName(2));

        }

        /**
         * Main method that handles all actions performed on UI elements.
         * 
         * Do not support mouse events, only UI elements like buttons, spinners and menus. 
         * Runs also main algorithm on specified input state and update screen on plugins
         * operations.
         * 
         * @param e Type of event
         * @see BOAp
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            boolean run = false; // some actions require to re-run segmentation.
                                 // They set run to true
            Object b = e.getSource();
            if (b == bDel && !boap.editMode && !boap.doDeleteSeg) {
                if (boap.doDelete == false) {
                    bDel.setLabel("*STOP DEL*");
                    boap.doDelete = true;
                    lastTool = IJ.getToolName();
                    IJ.setTool(Toolbar.LINE);
                } else {
                    boap.doDelete = false;
                    bDel.setLabel("Delete cell");
                    IJ.setTool(lastTool);
                }
                return;
            }
            if (boap.doDelete) { // stop if delete is on
                BOA_.log("**DELETE IS ON**");
                return;
            }
            if (b == bDelSeg && !boap.editMode) {
                if (!boap.doDeleteSeg) {
                    bDelSeg.setLabel("*STOP TRUNCATE*");
                    boap.doDeleteSeg = true;
                    lastTool = IJ.getToolName();
                    IJ.setTool(Toolbar.LINE);
                } else {
                    boap.doDeleteSeg = false;
                    bDelSeg.setLabel("Truncate Seg");
                    IJ.setTool(lastTool);
                }
                return;
            }
            if (boap.doDeleteSeg) { // stop if delete is on
                BOA_.log("**TRUNCATE SEG IS ON**");
                return;
            }
            if (b == bEdit) {
                if (boap.editMode == false) {
                    bEdit.setLabel("*STOP EDIT*");
                    BOA_.log("**EDIT IS ON**");
                    boap.editMode = true;
                    lastTool = IJ.getToolName();
                    IJ.setTool(Toolbar.LINE);
                    if (nest.size() == 1)
                        editSeg(0, 0, boaState.frame); // if only 1 snake go straight to edit, if
                                                       // more user
                    // must pick one
                } else {
                    boap.editMode = false;
                    if (boap.editingID != -1) {
                        stopEdit();
                    }
                    bEdit.setLabel("Edit");
                    IJ.setTool(lastTool);
                }
                return;
            }
            if (boap.editMode) { // stop if edit on
                BOA_.log("**EDIT IS ON**");
                return;
            }
            if (b == bDefault) {
                this.setDefualts();
                run = true;
            } else if (b == bSeg) { // main segmentation procedure starts here
                IJ.showStatus("SEGMENTING...");
                bSeg.setLabel("computing");
                int framesCompleted;
                try {
                    runBoa(boaState.frame, boap.FRAMES);
                    framesCompleted = boap.FRAMES;
                    IJ.showStatus("COMPLETE");
                } catch (BoaException be) {
                    BOA_.log(be.getMessage());
                    framesCompleted = be.getFrame();
                    IJ.showStatus("FAIL AT " + framesCompleted);
                    BOA_.log("FAIL AT " + framesCompleted);
                }
                bSeg.setLabel("SEGMENT");
            } else if (b == bLoad) {
                try {
                    if (boap.readParams()) {
                        updateSpinnerValues();
                        if (loadSnakes()) {
                            run = false;
                        } else {
                            run = true;
                        }

                    }
                } catch (Exception IOe) {
                    IJ.error("Exception when reading parameters from file...");
                }

            } else if (b == bScale) {
                setScales();
                pixelLabel.setText("Scale: " + IJ.d2s(boap.imageScale, 6) + " \u00B5m");
                fpsLabel.setText("F Interval: " + IJ.d2s(boap.imageFrameInterval, 3) + " s");
            } else if (b == bAdd) {
                addCell(canvas.getImage().getRoi(), boaState.frame);
                canvas.getImage().killRoi();
            } else if (b == bFinish) {
                BOA_.log("Finish: Exiting BOA...");
                fpsLabel.setName("moo");
                finish();
            } else if (b == bQuit) {
                quit();
            }
            // process plugin GUI buttons
            if (b == bFirstPluginGUI) {
                LOGGER.debug("First plugin GUI, state of BOAp is "
                        + boaState.snakePluginList.getInstance(0));
                if (boaState.snakePluginList.getInstance(0) != null) // call 0 instance
                    boaState.snakePluginList.getInstance(0).showUI(true);
            }
            if (b == bSecondPluginGUI) {
                LOGGER.debug("Second plugin GUI, state of BOAp is "
                        + boaState.snakePluginList.getInstance(1));
                if (boaState.snakePluginList.getInstance(1) != null) // call 1 instance
                    boaState.snakePluginList.getInstance(1).showUI(true);
            }
            if (b == bThirdPluginGUI) {
                LOGGER.debug("Third plugin GUI, state of BOAp is "
                        + boaState.snakePluginList.getInstance(2));
                if (boaState.snakePluginList.getInstance(2) != null) // call 2 instance
                    boaState.snakePluginList.getInstance(2).showUI(true);
            }

            // menu listeners
            if (b == menuVersion) {
                about();
            }
            if (b == menuSaveConfig) {
                String saveIn = boap.orgFile.getParent();
                SaveDialog sd = new SaveDialog("Save plugin config data...", saveIn, boap.fileName,
                        ".pgQP");
                if (sd.getFileName() != null) {
                    try {
                        // Create Serialization object wit extra info layer
                        Serializer<SnakePluginList> s;
                        s = new Serializer<>(boaState.snakePluginList, quimpInfo);
                        s.setPretty(); // set pretty format
                        s.save(sd.getDirectory() + sd.getFileName()); // save it
                        s = null; // remove
                    } catch (FileNotFoundException e1) {
                        LOGGER.error("Problem with saving plugin config");
                    }
                }
            }
            // TODO Add checking loaded version using quimpInfo data sealed in Serializer.save
            if (b == menuLoadConfig) {
                OpenDialog od = new OpenDialog("Load plugin config data...", "");
                if (od.getFileName() != null) {
                    try {
                        Serializer<SnakePluginList> loaded; // loaded instance
                        // create serializer
                        Serializer<SnakePluginList> s = new Serializer<>(SnakePluginList.class);
                        s.registerInstanceCreator(SnakePluginList.class,
                                new SnakePluginListInstanceCreator(3, pluginFactory, null,
                                        viewUpdater)); // pass data to constructor of serialized
                                                       // object. Those data are not serialized
                                                       // and must be passed externally
                        loaded = s.load(od.getDirectory() + od.getFileName());
                        // restore loaded objects
                        boaState.snakePluginList.clear(); // closes windows, etc
                        boaState.snakePluginList = loaded.obj; // replace with fres instance
                        updateCheckBoxes(); // update checkboxes
                        updateChoices(); // and choices
                        recalculatePlugins(); // and screen
                    } catch (IOException e1) {
                        LOGGER.error("Problem with loading plugin config");
                    } catch (JsonSyntaxException e1) {
                        LOGGER.error("Problem with configuration file: " + e1.getMessage());
                    } catch (Exception e1) {
                        LOGGER.error(e1); // something serious
                    }
                }
            }

            // history
            if (b == menuShowHistory) {
                LOGGER.debug("got ShowHistory");
                if (historyLogger.isOpened())
                    historyLogger.closeHistory();
                else
                    historyLogger.openHistory();
            }

            // run segmentation for selected cases
            if (run) {
                System.out.println("running from in stackwindow");
                // run on current frame
                try {
                    runBoa(boaState.frame, boaState.frame);
                } catch (BoaException be) {
                    BOA_.log(be.getMessage());
                }
                // imageGroup.setSlice(1);
            }
        }

        /**
         * Detect changes in checkboxes and run segmentation for current frame
         * if necessary. Transfer parameters from changed GUI element to
         * {@link uk.ac.warwick.wsbc.QuimP.BOAp} class
         * 
         * @param e Type of event
         */
        @Override
        public void itemStateChanged(final ItemEvent e) {
            // detect check boxes
            if (boap.doDelete) {
                BOA_.log("**WARNING:DELETE IS ON**");
            }
            boolean run = false; // set to true if any of items changes require
                                 // to re-run segmentation
            Object source = e.getItemSelectable();
            if (source == cPath) {
                boap.segParam.showPaths = cPath.getState();
                if (boap.segParam.showPaths) {
                    this.setImage(imageGroup.getPathsIpl());
                } else {
                    this.setImage(imageGroup.getOrgIpl());
                }
                if (boap.zoom && !nest.isVacant()) { // set zoom
                    imageGroup.zoom(canvas, boaState.frame);
                }
            } else if (source == cPrevSnake) {
                boap.segParam.use_previous_snake = cPrevSnake.getState();
            } else if (source == cExpSnake) {
                boap.segParam.expandSnake = cExpSnake.getState();
                run = true;
            } else if (source == cZoom) {
                boap.zoom = cZoom.getState();
                if (boap.zoom && !nest.isVacant()) {
                    imageGroup.zoom(canvas, boaState.frame);
                } else {
                    imageGroup.unzoom(canvas);
                }
            } else if (source == cFirstPlugin) {
                boaState.snakePluginList.setActive(0, cFirstPlugin.getState());
                recalculatePlugins();
            } else if (source == cSecondPlugin) {
                boaState.snakePluginList.setActive(1, cSecondPlugin.getState());
                recalculatePlugins();
            } else if (source == cThirdPlugin) {
                boaState.snakePluginList.setActive(2, cThirdPlugin.getState());
                recalculatePlugins();
            }

            if (source == cbMenuPlotOriginalSnakes) {
                LOGGER.debug("got cbMenuPlotProcessedSnakes");
                boap.isProcessedSnakePlotted = cbMenuPlotOriginalSnakes.getState();
                recalculatePlugins();
            }

            if (source == firstPluginName) {
                LOGGER.debug("Used firstPluginName, val: " + firstPluginName.getSelectedItem());
                instanceSnakePlugin((String) firstPluginName.getSelectedItem(), 0,
                        cFirstPlugin.getState());
                recalculatePlugins();
            }
            if (source == secondPluginName) {
                LOGGER.debug("Used secondPluginName, val: " + secondPluginName.getSelectedItem());
                instanceSnakePlugin((String) secondPluginName.getSelectedItem(), 1,
                        cSecondPlugin.getState());
                recalculatePlugins();
            }
            if (source == thirdPluginName) {
                LOGGER.debug("Used thirdPluginName, val: " + thirdPluginName.getSelectedItem());
                instanceSnakePlugin((String) thirdPluginName.getSelectedItem(), 2,
                        cThirdPlugin.getState());
                recalculatePlugins();
            }

            if (run) {
                if (boap.supressStateChangeBOArun) {
                    // boap.supressStateChangeBOArun = false;
                    System.out.println("supressStateItem");
                    System.out.println(source.toString());
                    return;
                }
                // run on current frame
                try {
                    runBoa(boaState.frame, boaState.frame);
                } catch (BoaException be) {
                    BOA_.log(be.getMessage());
                }
                // imageGroup.setSlice(1);
            }
        }

        /**
         * Detect changes in spinners and run segmentation for current frame if
         * necessary. Transfer parameters from changed GUI element to
         * {@link uk.ac.warwick.wsbc.QuimP.BOAp} class
         * 
         * @param ce Type of event
         */
        @Override
        public void stateChanged(final ChangeEvent ce) {
            if (boap.doDelete) {
                BOA_.log("**WARNING:DELETE IS ON**");
            }
            boolean run = false; // set to true if any of items changes require to re-run
                                 // segmentation
            Object source = ce.getSource();

            if (source == dsNodeRes) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.setNodeRes((Double) spinner.getValue());
                run = true;
            } else if (source == dsVel_crit) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.vel_crit = (Double) spinner.getValue();
                run = true;
            } else if (source == dsF_image) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.f_image = (Double) spinner.getValue();
                run = true;
            } else if (source == dsF_central) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.f_central = (Double) spinner.getValue();
                run = true;
            } else if (source == dsF_contract) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.f_contract = (Double) spinner.getValue();
                run = true;
            } else if (source == dsFinalShrink) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.finalShrink = (Double) spinner.getValue();
                run = true;
            } else if (source == isMaxIterations) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.max_iterations = (Integer) spinner.getValue();
                run = true;
            } else if (source == isBlowup) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.blowup = (Integer) spinner.getValue();
                run = true;
            } else if (source == isSample_tan) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.sample_tan = (Integer) spinner.getValue();
                run = true;
            } else if (source == isSample_norm) {
                JSpinner spinner = (JSpinner) source;
                boap.segParam.sample_norm = (Integer) spinner.getValue();
                run = true;
            }

            if (run) {
                if (boap.supressStateChangeBOArun) {
                    // boap.supressStateChangeBOArun = false;
                    System.out.println("supressState");
                    System.out.println(source.toString());
                    return;
                }
                // System.out.println("run from state change");
                // run on current frame
                try {
                    runBoa(boaState.frame, boaState.frame);
                } catch (BoaException be) {
                    BOA_.log(be.getMessage());
                }
                // imageGroup.setSlice(1);
            }

        }

        /**
         * Update the frame label, overlay, frame and set zoom Called when user
         * clicks on slice selector in IJ window.
         */
        @Override
        public void updateSliceSelector() {
            super.updateSliceSelector();
            zSelector.setValue(imp.getCurrentSlice()); // this is delayed in
                                                       // super.updateSliceSelector force it now

            // if in edit, save current edit and start edit of next frame if exists
            boolean wasInEdit = boap.editMode;
            if (boap.editMode) {
                // BOA_.log("next frame in edit mode");
                stopEdit();
            }

            boaState.frame = imp.getCurrentSlice();
            frameLabel.setText("" + boaState.frame);
            imageGroup.updateOverlay(boaState.frame); // draw overlay
            imageGroup.setIpSliceAll(boaState.frame);

            // zoom to snake zero
            if (boap.zoom && !nest.isVacant()) {
                SnakeHandler sH = nest.getHandler(0);
                if (sH.isStoredAt(boaState.frame)) {
                    imageGroup.zoom(canvas, boaState.frame);
                }
            }

            if (wasInEdit) {
                bEdit.setLabel("*STOP EDIT*");
                BOA_.log("**EDIT IS ON**");
                boap.editMode = true;
                lastTool = IJ.getToolName();
                IJ.setTool(Toolbar.LINE);
                editSeg(0, 0, boaState.frame);
                IJ.setTool(lastTool);
            }
        }

        /**
         * Turn delete mode off by setting proper value in
         * {@link uk.ac.warwick.wsbc.QuimP.BOAp}
         */
        void switchOffDelete() {
            boap.doDelete = false;
            bDel.setLabel("Delete cell");
        }

        @Override
        public void setImage(ImagePlus imp2) {
            double m = this.ic.getMagnification();
            Dimension dem = this.ic.getSize();
            super.setImage(imp2);
            this.ic.setMagnification(m);
            this.ic.setSize(dem);
        }

        /**
         * Turn truncate mode off by setting proper value in
         * {@link uk.ac.warwick.wsbc.QuimP.BOAp}
         */
        void switchOfftruncate() {
            boap.doDeleteSeg = false;
            bDelSeg.setLabel("Truncate Seg");
        }

        void setScalesText() {
            pixelLabel.setText("Scale: " + IJ.d2s(boap.imageScale, 6) + " \u00B5m");
            fpsLabel.setText("F Interval: " + IJ.d2s(boap.imageFrameInterval, 3) + " s");
        }
    } // end of CustomStackWindow

    /**
     * Creates instance (through SnakePluginList) of plugin of given name on given UI slot.
     * 
     * Decides if plugin will be created or destroyed basing on plugin \b name from Choice list
     * 
     * @warning The same contexts should be restored after plugin load
     * 
     * @param selectedPlugin Name of plugin returned from UI elements
     * @param slot Slot of plugin
     * @param act Indicates if plugins is activated in GUI
     * @see QuimP.SnakePluginList
     */
    private void instanceSnakePlugin(final String selectedPlugin, int slot, boolean act) {

        try {
            // get instance using plugin name (obtained from getPluginNames from PluginFactory
            if (selectedPlugin != NONE) { // do no pass NONE to pluginFact
                boaState.snakePluginList.setInstance(slot, selectedPlugin, act); // build instance
            } else {
                if (boaState.snakePluginList.getInstance(slot) != null)
                    boaState.snakePluginList.getInstance(slot).showUI(false);
                boaState.snakePluginList.deletePlugin(slot);
            }
        } catch (QuimpPluginException e) {
            LOGGER.warn("Plugin " + selectedPlugin + " cannot be loaded");
        }
    }

    /**
     * Start segmentation process on range of frames
     * 
     * This method is called for update only current view as well (\c startF ==
     * \c endF)
     * 
     * @param startF start frame
     * @param endF end frame
     * @throws BoaException
     * @todo TODO Rewrite exceptions here
     * @see http://www.trac-wsbc.linkpc.net:8080/trac/QuimP/ticket/65
     */
    public void runBoa(int startF, int endF) throws BoaException {
        System.out.println("run BOA");
        boap.SEGrunning = true;
        if (nest.isVacant()) {
            BOA_.log("Nothing to segment!");
            boap.SEGrunning = false;
            return;
        }
        try {
            IJ.showProgress(0, endF - startF);

            // if(boap.expandSnake) boap.NMAX = 9990; // percent hack

            nest.resetForFrame(startF);
            if (!boap.segParam.expandSnake) { // blowup snake ready for contraction (only those not
                // starting
                // at or after the startF)
                constrictor.loosen(nest, startF);
            } else {
                constrictor.implode(nest, startF);
            }
            SnakeHandler sH;

            int s = 0;
            Snake snake;
            imageGroup.clearPaths(startF);

            for (boaState.frame = startF; boaState.frame <= endF; boaState.frame++) { // per frame
                // System.out.println("\n737 Frame: " + frame);
                imageGroup.setProcessor(boaState.frame);
                imageGroup.setIpSliceAll(boaState.frame);

                try {
                    if (boaState.frame != startF) {// expand snakes for next frame
                        if (!boap.segParam.use_previous_snake) {
                            nest.resetForFrame(boaState.frame);
                        } else {
                            if (!boap.segParam.expandSnake) {
                                constrictor.loosen(nest, boaState.frame);
                            } else {
                                constrictor.implode(nest, boaState.frame);
                            }
                        }
                    }
                    // imageGroup.drawContour(nest.getSNAKES(), frame);//draw
                    // starting snake
                    // if(frame==2) break;

                    for (s = 0; s < nest.size(); s++) { // for each snake
                        sH = nest.getHandler(s);
                        snake = sH.getLiveSnake();
                        try {
                            if (!snake.alive || boaState.frame < sH.getStartframe()) {
                                continue;
                            }
                            imageGroup.drawPath(snake, boaState.frame); // pre tightned snake on
                                                                        // path
                            tightenSnake(snake);
                            imageGroup.drawPath(snake, boaState.frame); // post tightned snake on
                                                                        // path
                            sH.backupLiveSnake(boaState.frame);
                            Snake out = iterateOverSnakePlugins(snake);
                            sH.storeThisSnake(out, boaState.frame); // store resulting snake as
                                                                    // final

                        } catch (QuimpPluginException qpe) {
                            // must be rewritten with whole runBOA #65 #67
                            BOA_.log("Error in filter module: " + qpe.getMessage());
                            LOGGER.error(qpe);
                            sH.storeLiveSnake(boaState.frame); // store segmented nonmodified

                        } catch (BoaException be) {
                            imageGroup.drawPath(snake, boaState.frame); // failed
                            // position
                            // sH.deleteStoreAt(frame);
                            sH.storeLiveSnake(boaState.frame);
                            sH.backupLiveSnake(boaState.frame);
                            nest.kill(sH);
                            snake.defreeze();
                            BOA_.log("Snake " + snake.snakeID + " died, frame " + boaState.frame);
                            boap.SEGrunning = false;
                            if (nest.allDead()) {
                                throw new BoaException("All snakes dead: " + be.getMessage(),
                                        boaState.frame, 1);
                            }
                        }

                    }
                    imageGroup.updateOverlay(boaState.frame);
                    IJ.showProgress(boaState.frame, endF);
                } catch (BoaException be) {
                    boap.SEGrunning = false;
                    if (!boap.segParam.use_previous_snake) {
                        imageGroup.setIpSliceAll(boaState.frame);
                        imageGroup.updateOverlay(boaState.frame);
                    } else {
                        System.out.println("\nL811. Exception");
                        throw be;
                    }
                } finally {
                    historyLogger.addEntry("Processing", boaState);
                }
            }
            boaState.frame = endF;

        } catch (Exception e) {
            // e.printStackTrace();
            /// imageGroup.drawContour(nest.getSNAKES(), frame);
            // imageGroup.updateAndDraw();
            boap.SEGrunning = false;
            e.printStackTrace();
            throw new BoaException("Frame " + boaState.frame + ": " + e.getMessage(),
                    boaState.frame, 1);
        }
        boap.SEGrunning = false;
    }

    /**
     * Process \c Snake by all active plugins. Processed \c Snake is returned as new Snake with
     * the same ID. Input snake is not modified. For empty plugin list it just return input snake
     *
     * @param snake snake to be processed
     * @return Processed snake or original input one when there is no plugin selected
     * @throws QuimpPluginException on plugin error
     * @throws Exception
     */
    private Snake iterateOverSnakePlugins(final Snake snake)
            throws QuimpPluginException, Exception {
        Snake outsnake = snake;
        if (!boaState.snakePluginList.isRefListEmpty()) {
            LOGGER.debug("sPluginList not empty");
            List<Point2d> dataToProcess = snake.asList();
            for (Plugin qP : boaState.snakePluginList.getList()) {
                if (!qP.isExecutable())
                    continue; // no plugin on this slot or not active
                // because it is guaranteed by pluginFactory.getPluginNames(DOES_SNAKES) used
                // when populating GUI names and boap.sPluginList in actionPerformed(ActionEvent e).
                IQuimpPoint2dFilter qPcast = (IQuimpPoint2dFilter) qP.getRef();
                qPcast.attachData(dataToProcess);
                dataToProcess = qPcast.runPlugin();
            }
            outsnake = new Snake(dataToProcess, snake.snakeID);
        } else
            LOGGER.debug("sPluginList empty");
        return outsnake;
    }

    private void tightenSnake(final Snake snake) throws BoaException {

        int i;
        // imageGroup.drawPath(snake, frame); //draw initial contour on path
        // image

        for (i = 0; i < boap.segParam.max_iterations; i++) { // iter constrict snake
            if (i % boap.cut_every == 0) {
                snake.cutLoops(); // cut out loops every p.cut_every timesteps
            }
            if (i % 10 == 0 && i != 0) {
                snake.correctDistance(true);
            }
            if (constrictor.constrict(snake, imageGroup.getOrgIp())) { // if all nodes frozen
                break;
            }
            if (i % 4 == 0) {
                imageGroup.drawPath(snake, boaState.frame); // draw current snake
            }

            if ((snake.getNODES() / snake.startingNnodes) > boap.NMAX) {
                // if max nodes reached (as % starting) prompt for reset
                if (boap.segParam.use_previous_snake) {
                    // imageGroup.drawContour(snake, frame);
                    // imageGroup.updateAndDraw();
                    throw new BoaException(
                            "Frame " + boaState.frame + "-max nodes reached " + snake.getNODES(),
                            boaState.frame, 1);
                } else {
                    BOA_.log("Frame " + boaState.frame + "-max nodes reached..continue");
                    break;
                }
            }
            // if (i == boap.max_iterations - 1) {
            // BOA_.log("Frame " + frame + "-max iterations reached");
            // }
            // break;
        }
        snake.defreeze(); // set freeze tag back to false

        if (!boap.segParam.expandSnake) { // shrink a bit to get final outline
            snake.shrinkSnake();
        }
        // System.out.println("finished tighten- cut loops and intersects");
        snake.cutLoops();
        snake.cutIntersects();
        // System.out.println("finished tighten with loop cuts");

        // snake.correctDistance();
        // imageGroup.drawPath(snake, frame); //draw final contour on path image
    }

    void setScales() {
        GenericDialog gd = new GenericDialog("Set image scale", window);
        gd.addNumericField("Frame interval (seconds)", boap.imageFrameInterval, 3);
        gd.addNumericField("Pixel width (\u00B5m)", boap.imageScale, 6);
        gd.showDialog();

        double tempFI = gd.getNextNumber(); // force to check for errors
        double tempP = gd.getNextNumber();

        if (gd.invalidNumber()) {
            IJ.error("Values invalid");
            BOA_.log("Scale was not updated:\n\tinvalid input");
        } else if (gd.wasOKed()) {
            boap.imageFrameInterval = tempFI;
            boap.imageScale = tempP;
            updateImageScale();
            BOA_.log("Scale successfully updated");
        }

    }

    void updateImageScale() {
        imageGroup.getOrgIpl().getCalibration().frameInterval = boap.imageFrameInterval;
        imageGroup.getOrgIpl().getCalibration().pixelHeight = boap.imageScale;
        imageGroup.getOrgIpl().getCalibration().pixelWidth = boap.imageScale;
    }

    boolean loadSnakes() {

        YesNoCancelDialog yncd = new YesNoCancelDialog(IJ.getInstance(), "Load associated snakes?",
                "\tLoad associated snakes?\n");
        if (!yncd.yesPressed()) {
            return false;
        }

        OutlineHandler oH = new OutlineHandler(boap.readQp);
        if (!oH.readSuccess) {
            BOA_.log("Could not read in snakes");
            return false;
        }
        // convert to BOA snakes

        nest.addOutlinehandler(oH);
        imageGroup.setProcessor(oH.getStartFrame());
        imageGroup.updateOverlay(oH.getStartFrame());
        BOA_.log("Successfully read snakes");
        return true;
    }

    /**
     * Add ROI to Nest.
     * 
     * This method is called on selection that should contain object to be
     * segmented. Initialize Snake object in Nest and it performs also initial
     * segmentation of selected cell
     * 
     * @param r ROI object (IJ)
     * @param f number of current frame
     * @see tightenSnake(Snake)
     */
    // @SuppressWarnings("unchecked")
    void addCell(final Roi r, int f) {
        boolean isPluginError = false; // any error from plugin?
        SnakeHandler sH = nest.addHandler(r, f);
        Snake snake = sH.getLiveSnake();
        imageGroup.setProcessor(f);
        try {
            imageGroup.drawPath(snake, f); // pre tightned snake on path
            tightenSnake(snake);
            imageGroup.drawPath(snake, f); // post tightned snake on path
            sH.backupLiveSnake(f);

            Snake out = iterateOverSnakePlugins(snake); // process segmented snake by plugins
            sH.storeThisSnake(out, f); // store processed snake as final
        } catch (QuimpPluginException qpe) {
            isPluginError = true; // we have error
            BOA_.log("Error in filter module: " + qpe.getMessage());
            LOGGER.error(qpe);
        } catch (BoaException be) {
            BOA_.log("New snake failed to converge");
            LOGGER.error(be);
        } catch (Exception e) {
            BOA_.log("Undefined error from plugin");
            LOGGER.fatal(e);
        }
        // if any problem with plugin or other, store snake without modification
        // because snake.asList() returns copy
        try {
            if (isPluginError)
                sH.storeLiveSnake(f); // so store original livesnake after segmentation
        } catch (BoaException be) {
            BOA_.log("Could not store new snake");
            LOGGER.error(be);
        } finally {
            imageGroup.updateOverlay(f);
            historyLogger.addEntry("Added cell", boaState);
        }

    }

    boolean deleteCell(int x, int y, int frame) {
        if (nest.isVacant()) {
            return false;
        }

        SnakeHandler sH;
        Snake snake;
        ExtendedVector2d sV;
        ExtendedVector2d mV = new ExtendedVector2d(x, y);
        double[] distance = new double[nest.size()];

        for (int i = 0; i < nest.size(); i++) { // calc all distances
            sH = nest.getHandler(i);
            if (sH.isStoredAt(frame)) {
                snake = sH.getStoredSnake(frame);
                sV = snake.getCentroid();
                distance[i] = ExtendedVector2d.lengthP2P(mV, sV);
            }
        }
        int minIndex = Tool.minArrayIndex(distance);
        if (distance[minIndex] < 10) { // if closest < 10, delete it
            BOA_.log("Deleted cell " + nest.getHandler(minIndex).getID());
            nest.removeHandler(nest.getHandler(minIndex));
            imageGroup.updateOverlay(frame);
            window.switchOffDelete();
            return true;
        } else {
            BOA_.log("Click the cell centre to delete");
        }
        return false;
    }

    void deleteSegmentation(int x, int y, int frame) {
        SnakeHandler sH;
        Snake snake;
        ExtendedVector2d sV;
        ExtendedVector2d mV = new ExtendedVector2d(x, y);
        double[] distance = new double[nest.size()];

        for (int i = 0; i < nest.size(); i++) { // calc all distances
            sH = nest.getHandler(i);

            if (sH.isStoredAt(frame)) {
                snake = sH.getStoredSnake(frame);
                sV = snake.getCentroid();
                distance[i] = ExtendedVector2d.lengthP2P(mV, sV);
            } else {
                distance[i] = 9999;
            }
        }

        int minIndex = Tool.minArrayIndex(distance);
        // BOA_.log("Debug: closest index " + minIndex + ", id " +
        // nest.getHandler(minIndex).getID());
        if (distance[minIndex] < 10) { // if closest < 10, delete it
            BOA_.log("Deleted snake " + nest.getHandler(minIndex).getID() + " from " + frame
                    + " onwards");
            sH = nest.getHandler(minIndex);
            sH.deleteStoreFrom(frame);
            imageGroup.updateOverlay(frame);
            window.switchOfftruncate();
        } else {
            BOA_.log("Click the cell centre to delete");
        }
    }

    /**
     * Called when user click Edit button.
     * 
     * @param x Coordinate of clicked point
     * @param y Coordinate of clicked point
     * @param frame current frame in stack
     * @see stopEdit
     * @see updateSliceSelector
     */
    void editSeg(int x, int y, int frame) {
        SnakeHandler sH;
        Snake snake;
        ExtendedVector2d sV;
        ExtendedVector2d mV = new ExtendedVector2d(x, y);
        double[] distance = new double[nest.size()];

        for (int i = 0; i < nest.size(); i++) { // calc all distances
            sH = nest.getHandler(i);
            if (sH.isStoredAt(frame)) {
                snake = sH.getStoredSnake(frame);
                sV = snake.getCentroid();
                distance[i] = ExtendedVector2d.lengthP2P(mV, sV);
            }
        }
        int minIndex = Tool.minArrayIndex(distance);
        if (distance[minIndex] < 10 || nest.size() == 1) { // if closest < 10, edit it
            sH = nest.getHandler(minIndex);
            boap.editingID = minIndex; // sH.getID();
            BOA_.log("Editing cell " + sH.getID());
            imageGroup.clearOverlay();

            Roi r;
            if (boap.useSubPixel == true) {
                r = sH.getStoredSnake(frame).asPolyLine();
            } else {
                r = sH.getStoredSnake(frame).asIntRoi();
            }
            // Roi r = sH.getStoredSnake(frame).asFloatRoi();
            Roi.setColor(Color.cyan);
            canvas.getImage().setRoi(r);
        } else {
            BOA_.log("Click a cell centre to edit");
        }
    }

    /**
     * Called when user ends editing.
     * 
     * @see updateSliceSelector
     */
    void stopEdit() {
        Roi r = canvas.getImage().getRoi();
        Roi.setColor(Color.yellow);
        SnakeHandler sH = nest.getHandler(boap.editingID);
        sH.storeRoi((PolygonRoi) r, boaState.frame);
        canvas.getImage().killRoi();
        imageGroup.updateOverlay(boaState.frame);
        boap.editingID = -1;
    }

    void deleteSeg(int x, int y) {
    }

    /**
     * Initializing all data saving and exporting results to disk and IJ
     */
    private void finish() {
        IJ.showStatus("BOA-FINISHING");
        YesNoCancelDialog ync;

        if (boap.saveSnake) {
            try {
                if (nest.writeSnakes()) {
                    nest.analyse(imageGroup.getOrgIpl());
                    // auto save plugin config
                    // Create Serialization object wit extra info layer
                    Serializer<SnakePluginList> s;
                    s = new Serializer<>(boaState.snakePluginList, quimpInfo);
                    s.setPretty(); // set pretty format
                    s.save(boap.outFile.getParent() + File.separator + boap.fileName + ".pgQP");
                    s = null; // remove
                } else {
                    ync = new YesNoCancelDialog(window, "Save Segmentation",
                            "Quit without saving?");
                    if (!ync.yesPressed()) {
                        return;
                    }
                }
            } catch (Exception e) {
                IJ.error("Exception while saving");
                e.printStackTrace();
                return;
            }

        }
        BOA_.running = false;
        imageGroup.makeContourImage();
        nest = null; // remove from memory
        imageGroup.getOrgIpl().setOverlay(new Overlay());
        new StackWindow(imageGroup.getOrgIpl()); // clear overlay
        window.setImage(new ImagePlus());
        window.close();
    }

    /**
     * Action for Quit button Set BOA_.running static field to false and close
     * the window
     * 
     * @author rtyson
     * @author p.baniukiewicz
     */
    void quit() {
        YesNoCancelDialog ync;
        ync = new YesNoCancelDialog(window, "Quit", "Quit without saving?");
        if (!ync.yesPressed()) {
            return;
        }

        BOA_.running = false;
        nest = null; // remove from memory
        imageGroup.getOrgIpl().setOverlay(new Overlay()); // clear overlay
        new StackWindow(imageGroup.getOrgIpl());

        window.setImage(new ImagePlus());// remove link to window
        window.close();
    }
}

/**
 * Hold, manipulate and draw on images
 * 
 * @author rtyson
 *
 */
class ImageGroup {
    // paths - snake drawn as it contracts
    // contour - final snake drawn
    // org - original image, kept clean

    private ImagePlus orgIpl, pathsIpl;// , contourIpl;
    private ImageStack orgStack, pathsStack; // , contourStack;
    private ImageProcessor orgIp, pathsIp; // , contourIp;
    private Overlay overlay;
    private final Nest nest;
    int w, h, f;

    public ImageGroup(ImagePlus oIpl, Nest n) {
        nest = n;
        // create two new stacks for drawing

        // image set up
        orgIpl = oIpl;
        orgIpl.setSlice(1);
        orgIpl.getCanvas().unzoom();
        orgIpl.getCanvas().getMagnification();

        orgStack = orgIpl.getStack();
        orgIp = orgStack.getProcessor(1);

        w = orgIp.getWidth();
        h = orgIp.getHeight();
        f = orgIpl.getStackSize();

        // set up blank path image
        pathsIpl = NewImage.createByteImage("Node Paths", w, h, f, NewImage.FILL_BLACK);
        pathsStack = pathsIpl.getStack();
        pathsIpl.setSlice(1);
        pathsIp = pathsStack.getProcessor(1);

        setIpSliceAll(1);
        setProcessor(1);
    }

    public ImagePlus getOrgIpl() {
        return orgIpl;
    }

    public ImagePlus getPathsIpl() {
        return pathsIpl;
    }

    public ImageProcessor getOrgIp() {
        return orgIp;
    }

    /**
     * Plots snakes on current frame.
     * 
     * Depending on configuration this method can plot:
     * -# Snake after segmentation, without processing by plugins
     * -# Snake after segmentation and after processing by all active plugins
     * 
     * @param frame Current frame
     */
    public void updateOverlay(int frame) {
        SnakeHandler sH;
        Snake snake, back;
        int x, y;
        TextRoi text;
        Roi r;
        overlay = new Overlay();
        for (int i = 0; i < nest.size(); i++) {
            sH = nest.getHandler(i);
            if (sH.isStoredAt(frame)) { // is there a snake a;t f?
                // plot segmented snake
                if (BOA_.boap.isProcessedSnakePlotted == true) {
                    back = sH.getBackupSnake(frame);
                    // Roi r = snake.asRoi();
                    r = back.asFloatRoi();
                    r.setStrokeColor(Color.RED);
                    overlay.add(r);
                }

                // plot segmented and filtered snake
                snake = sH.getStoredSnake(frame);
                // Roi r = snake.asRoi();
                r = snake.asFloatRoi();
                r.setStrokeColor(Color.YELLOW);
                overlay.add(r);
                x = (int) Math.round(snake.getHead().getX()) - 15;
                y = (int) Math.round(snake.getHead().getY()) - 15;
                text = new TextRoi(x, y, "   " + snake.snakeID);
                overlay.add(text);

                // draw centre point
                PointRoi pR = new PointRoi((int) snake.getCentroid().getX(),
                        (int) snake.getCentroid().getY());
                overlay.add(pR);

            }
        }
        orgIpl.setOverlay(overlay);
    }

    public void clearOverlay() {
        // overlay = new Overlay();
        orgIpl.setOverlay(null);
    }

    final public void setProcessor(int i) {
        orgIp = orgStack.getProcessor(i);
        pathsIp = pathsStack.getProcessor(i);
        // System.out.println("\n1217 Proc set to : " + i);
    }

    final public void setIpSliceAll(int i) {
        // set slice on all images
        pathsIpl.setSlice(i);
        orgIpl.setSlice(i);
    }

    public void clearPaths(int fromFrame) {
        for (int i = fromFrame; i <= BOA_.boap.FRAMES; i++) {
            pathsIp = pathsStack.getProcessor(i);
            pathsIp.setValue(0);
            pathsIp.fill();
        }
        pathsIp = pathsStack.getProcessor(fromFrame);
    }

    public void drawPath(Snake snake, int frame) {
        pathsIp = pathsStack.getProcessor(frame);
        drawSnake(pathsIp, snake, false);
    }

    private void drawSnake(final ImageProcessor ip, final Snake snake, boolean contrast) {
        // draw snake
        int x, y;
        int intensity;

        Node n = snake.getHead();
        do {
            x = (int) (n.getPoint().getX());
            y = (int) (n.getPoint().getY());

            if (!contrast) {
                intensity = 245;
            } else {
                // paint as black or white for max contrast
                if (ip.getPixel(x, y) > 127) {
                    intensity = 10;
                } else {
                    intensity = 245;
                }
            }
            // for colour:
            // if(boap.drawColor) intensity = n.colour.getColorInt();

            if (BOA_.boap.HEIGHT > 800) {
                drawPixel(x, y, intensity, true, ip);
            } else {
                drawPixel(x, y, intensity, false, ip);
            }
            n = n.getNext();
        } while (!n.isHead());
    }

    private void drawPixel(int x, int y, int intensity, boolean fat, ImageProcessor ip) {
        ip.putPixel(x, y, intensity);
        if (fat) {
            ip.putPixel(x + 1, y, intensity);
            ip.putPixel(x + 1, y + 1, intensity);
            ip.putPixel(x, y + 1, intensity);
            ip.putPixel(x - 1, y + 1, intensity);
            ip.putPixel(x - 1, y, intensity);
            ip.putPixel(x - 1, y - 1, intensity);
            ip.putPixel(x, y - 1, intensity);
            ip.putPixel(x + 1, y - 1, intensity);
        }
    }

    void makeContourImage() {
        ImagePlus contourIpl = NewImage.createByteImage("Contours", w, h, f, NewImage.FILL_BLACK);
        ImageStack contourStack = contourIpl.getStack();
        contourIpl.setSlice(1);
        ImageProcessor contourIp;

        for (int i = 1; i <= BOA_.boap.FRAMES; i++) { // copy original
            orgIp = orgStack.getProcessor(i);
            contourIp = contourStack.getProcessor(i);
            contourIp.copyBits(orgIp, 0, 0, Blitter.COPY);
        }

        drawCellRois(contourStack);
        new ImagePlus(orgIpl.getTitle() + "_Segmentation", contourStack).show();

    }

    void zoom(final ImageCanvas ic, int frame) {
        // zoom to cell 1
        if (nest.isVacant()) {
            return;
        }
        SnakeHandler sH = nest.getHandler(0);
        Snake snake;
        if (sH.isStoredAt(frame)) {
            snake = sH.getStoredSnake(frame);
        } else {
            return;
        }

        Rectangle r = snake.getBounds();
        int border = 40;

        // add border (10 either way)
        r.setBounds(r.x - border, r.y - border, r.width + border * 2, r.height + border * 2);

        // correct r's aspect ratio
        double icAspect = (double) ic.getWidth() / (double) ic.getHeight();
        double rAspect = r.getWidth() / r.getHeight();
        int newDim; // new dimenesion size

        if (icAspect < rAspect) {
            // too short
            newDim = (int) (r.getWidth() / icAspect);
            r.y = r.y - ((newDim - r.height) / 2); // move snake to center
            r.height = newDim;
        } else {
            // too thin
            newDim = (int) (r.getHeight() * icAspect);
            r.x = r.x - ((newDim - r.width) / 2); // move snake to center
            r.width = newDim;
        }

        // System.out.println("ic " + icAspect + ". rA: " + r.getWidth() /
        // r.getHeight());

        double newMag;

        newMag = (double) ic.getHeight() / r.getHeight(); // mag required

        ic.setMagnification(newMag);
        Rectangle sr = ic.getSrcRect();
        sr.setBounds(r);

        ic.repaint();

    }

    void unzoom(final ImageCanvas ic) {
        // Rectangle sr = ic.getSrcRect();
        // sr.setBounds(0, 0, boap.WIDTH, boap.HEIGHT);
        ic.unzoom();
        // ic.setMagnification(orgMag);
        // ic.repaint();
    }

    void drawCellRois(final ImageStack stack) {
        Snake snake;
        SnakeHandler sH;
        ImageProcessor ip;

        int x, y;
        for (int s = 0; s < nest.size(); s++) {
            sH = nest.getHandler(s);
            for (int i = 1; i <= BOA_.boap.FRAMES; i++) {
                if (sH.isStoredAt(i)) {
                    snake = sH.getStoredSnake(i);
                    ip = stack.getProcessor(i);
                    ip.setColor(255);
                    ip.draw(snake.asFloatRoi());
                    x = (int) Math.round(snake.getHead().getX()) - 15;
                    y = (int) Math.round(snake.getHead().getY()) - 15;
                    ip.moveTo(x, y);
                    ip.drawString("   " + snake.snakeID);
                }
            }
        }
    }
}

/**
 * Calculate forces that affect the snake
 * 
 * @author rtyson
 */
class Constrictor {
    public Constrictor() {
    }

    public boolean constrict(final Snake snake, final ImageProcessor ip) {

        ExtendedVector2d F_temp; // temp vectors for forces
        ExtendedVector2d V_temp = new ExtendedVector2d();

        Node n = snake.getHead();
        do { // compute F_total

            if (!n.isFrozen()) {

                // compute F_central
                V_temp.setX(n.getNormal().getX() * BOA_.boap.segParam.f_central);
                V_temp.setY(n.getNormal().getY() * BOA_.boap.segParam.f_central);
                n.setF_total(V_temp);

                // compute F_contract
                F_temp = contractionForce(n);
                V_temp.setX(F_temp.getX() * BOA_.boap.segParam.f_contract);
                V_temp.setY(F_temp.getY() * BOA_.boap.segParam.f_contract);
                n.addF_total(V_temp);

                // compute F_image and F_friction
                F_temp = imageForce(n, ip);
                V_temp.setX(F_temp.getX() * BOA_.boap.segParam.f_image);// - n.getVel().getX() *
                // boap.f_friction);
                V_temp.setY(F_temp.getY() * BOA_.boap.segParam.f_image);// - n.getVel().getY() *
                // boap.f_friction);
                n.addF_total(V_temp);

                // compute new velocities of the node
                V_temp.setX(BOA_.boap.delta_t * n.getF_total().getX());
                V_temp.setY(BOA_.boap.delta_t * n.getF_total().getY());
                n.addVel(V_temp);

                // store the prelimanary point to move the node to
                V_temp.setX(BOA_.boap.delta_t * n.getVel().getX());
                V_temp.setY(BOA_.boap.delta_t * n.getVel().getY());
                n.setPrelim(V_temp);

                // add some friction
                n.getVel().multiply(BOA_.boap.f_friction);

                // freeze node if vel is below vel_crit
                if (n.getVel().length() < BOA_.boap.segParam.vel_crit) {
                    snake.freezeNode(n);
                }
            }
            n = n.getNext(); // move to next node
        } while (!n.isHead()); // continue while have not reached tail

        // update all nodes to new positions
        n = snake.getHead();
        do {
            n.update(); // use preliminary variables
            n = n.getNext();
        } while (!n.isHead());

        snake.updateNormales();

        return snake.isFrozen(); // true if all nodes frozen
    }

    /**
     * @deprecated Strictly related to absolute paths on disk. Probably for
     * testing purposes only
     */
    public boolean constrictWrite(final Snake snake, final ImageProcessor ip) {
        // for writing forces at each frame
        try {
            PrintWriter pw = new PrintWriter(
                    new FileWriter("/Users/rtyson/Documents/phd/tmp/test/forcesWrite/forces.txt"),
                    true); // auto flush
            ExtendedVector2d F_temp; // temp vectors for forces
            ExtendedVector2d V_temp = new ExtendedVector2d();

            Node n = snake.getHead();
            do { // compute F_total

                // if (!n.isFrozen()) {

                // compute F_central
                V_temp.setX(n.getNormal().getX() * BOA_.boap.segParam.f_central);
                V_temp.setY(n.getNormal().getY() * BOA_.boap.segParam.f_central);
                pw.print("\n" + n.getTrackNum() + "," + V_temp.length() + ",");
                n.setF_total(V_temp);

                // compute F_contract
                F_temp = contractionForce(n);
                if (n.getCurvatureLocal() > 0) {
                    pw.print(F_temp.length() + ",");
                } else {
                    pw.print((F_temp.length() * -1) + ",");
                }
                V_temp.setX(F_temp.getX() * BOA_.boap.segParam.f_contract);
                V_temp.setY(F_temp.getY() * BOA_.boap.segParam.f_contract);
                n.addF_total(V_temp);

                // compute F_image and F_friction
                F_temp = imageForce(n, ip);
                pw.print((F_temp.length() * -1) + ",");
                V_temp.setX(F_temp.getX() * BOA_.boap.segParam.f_image);// - n.getVel().getX()*
                // boap.f_friction);
                V_temp.setY(F_temp.getY() * BOA_.boap.segParam.f_image);// - n.getVel().getY()*
                // boap.f_friction);
                n.addF_total(V_temp);
                pw.print(n.getF_total().length() + "");

                // compute new velocities of the node
                V_temp.setX(BOA_.boap.delta_t * n.getF_total().getX());
                V_temp.setY(BOA_.boap.delta_t * n.getF_total().getY());
                n.addVel(V_temp);

                // add some friction
                n.getVel().multiply(BOA_.boap.f_friction);

                // store the prelimanary point to move the node to
                V_temp.setX(BOA_.boap.delta_t * n.getVel().getX());
                V_temp.setY(BOA_.boap.delta_t * n.getVel().getY());
                n.setPrelim(V_temp);

                // freeze node if vel is below vel_crit
                if (n.getVel().length() < BOA_.boap.segParam.vel_crit) {
                    snake.freezeNode(n);
                }
                // }
                n = n.getNext(); // move to next node
            } while (!n.isHead()); // continue while have not reached tail

            // update all nodes to new positions
            n = snake.getHead();
            do {
                n.update(); // use preliminary variables
                n = n.getNext();
            } while (!n.isHead());

            snake.updateNormales();

            pw.close();
            return snake.isFrozen(); // true if all nodes frozen
        } catch (Exception e) {
            return false;
        }
    }

    public ExtendedVector2d contractionForce(final Node n) {

        ExtendedVector2d R_result;
        ExtendedVector2d L_result;
        ExtendedVector2d force = new ExtendedVector2d();

        // compute the unit vector pointing to the left neighbor (static
        // method)
        L_result = ExtendedVector2d.unitVector(n.getPoint(), n.getPrev().getPoint());

        // compute the unit vector pointing to the right neighbor
        R_result = ExtendedVector2d.unitVector(n.getPoint(), n.getNext().getPoint());

        force.setX((R_result.getX() + L_result.getX()) * 0.5); // combine vector to left and right
        force.setY((R_result.getY() + L_result.getY()) * 0.5);

        return (force);
    }

    /**
     * @deprecated Probably old version of contractionForce(Node n)
     */
    public ExtendedVector2d imageForceOLD(final Node n, final ImageProcessor ip) {
        ExtendedVector2d result = new ExtendedVector2d();
        ExtendedVector2d tan; // Tangent
        int i, j;

        double a = 0.75; // subsampling factor
        double Delta_I; // intensity contrast
        double x, y; // co-ordinates of the local neighborhood
        double xt, yt; // co-ordinates of the tangent
        int I_inside = 0, I_outside = 0; // Intensity of the local
        // Neighborhood of a node (inside/outside of the chain)
        int I_in = 0, I_out = 0; // number of pixels in the local
        // Neighborhood of a node

        // compute normalized tangent (unit vector between neighbors) via
        // static method
        tan = n.getTangent();

        // determine local neighborhood: a rectangle with sample_tan x
        // sample_norm
        // tangent to the chain

        for (i = 0; i <= 1. / a * BOA_.boap.segParam.sample_tan; ++i) {
            // determine points on the tangent
            xt = n.getPoint().getX() + (a * i - BOA_.boap.segParam.sample_tan / 2) * tan.getX();
            yt = n.getPoint().getY() + (a * i - BOA_.boap.segParam.sample_tan / 2) * tan.getY();

            for (j = 0; j <= 1. / a * BOA_.boap.segParam.sample_norm / 2; ++j) {
                x = xt + a * j * n.getNormal().getX();
                y = yt + a * j * n.getNormal().getY();

                I_inside += ip.getPixel((int) x, (int) y);
                ++I_in;

                x = xt - a * j * n.getNormal().getX();
                y = yt - a * j * n.getNormal().getY();

                // check that pixel is inside frame
                if (x > 0 && y > 0 && x <= BOA_.boap.WIDTH && y <= BOA_.boap.HEIGHT) {
                    I_outside += ip.getPixel((int) x, (int) y);
                    ++I_out;
                }
            }
        }

        // if (I_out > boap.sample_norm / 2 * boap.sample_tan) //check that all
        // I_out pixels are inside the frame
        // {
        Delta_I = ((double) I_inside / I_in - (double) I_outside / I_out) / 255.;
        // }

        I_inside = 0;
        I_outside = 0; // Intensity of the local
        // neighbourhood of a node (insde/outside of the chain)
        I_in = 0;
        I_out = 0; // number of pixels in the local

        // rotate sample window and take the maximum contrast
        for (i = 0; i <= 1. / a * BOA_.boap.segParam.sample_norm; ++i) {
            // determine points on the tangent
            xt = n.getPoint().getX() + (a * i - BOA_.boap.segParam.sample_tan / 2) * tan.getX();
            yt = n.getPoint().getY() + (a * i - BOA_.boap.segParam.sample_tan / 2) * tan.getY();

            for (j = 0; j <= 1. / a * BOA_.boap.segParam.sample_tan / 2; ++j) {
                x = xt + a * j * n.getNormal().getX();
                y = yt + a * j * n.getNormal().getY();

                I_inside += ip.getPixel((int) x, (int) y);
                ++I_in;

                x = xt - a * j * n.getNormal().getX();
                y = yt - a * j * n.getNormal().getY();
                // check that pixel is inside frame
                if (x > 0 && y > 0 && x <= BOA_.boap.WIDTH && y <= BOA_.boap.HEIGHT) {
                    I_outside += ip.getPixel((int) x, (int) y);
                    ++I_out;
                }
            }
        }

        double Delta_I_r = ((double) I_inside / I_in - (double) I_outside / I_out) / 255.;
        System.out.println("Delta_I=" + Delta_I + ", Delta_I_r =" + Delta_I_r);

        if (I_out > BOA_.boap.segParam.sample_norm / 2 * BOA_.boap.segParam.sample_tan) // check
                                                                                        // that all
        // I_out pixels are
        // inside the frame
        {
            Delta_I = Math.max(Delta_I,
                    ((double) I_inside / I_in - (double) I_outside / I_out) / 255.);
        }

        // !!!!!!!!!!!
        // check if node erraneously got inside (pixel value > p.sensitivity)
        // if so, push node outside
        double check = (double) I_outside / I_out / 255.;

        if (check > BOA_.boap.sensitivity) // Delta_I += 0.5 * check;
        {
            Delta_I += 0.125 * check;
        }
        // if contrast is positive compute image force as square root of the
        // contrast
        if (Delta_I > 0.) { // else remains at zero
            result.setX(-Math.sqrt(Delta_I) * n.getNormal().getX());
            result.setY(-Math.sqrt(Delta_I) * n.getNormal().getY());
        }

        return (result);
    }

    public ExtendedVector2d imageForce(final Node n, final ImageProcessor ip) {
        ExtendedVector2d result = new ExtendedVector2d();
        ExtendedVector2d tan = n.getTangent(); // Tangent at node
        int i, j; // loop vars

        double a = 0.75; // subsampling factor
        double Delta_I; // intensity contrast
        double x, y; // co-ordinates of the norm
        double xt, yt; // co-ordinates of the tangent
        int I_inside = 0;
        int I_outside = 0; // Intensity of neighbourhood of a node
                           // (insde/outside of the chain)
        int I_in = 0, I_out = 0; // number of pixels in the neighbourhood of a
                                 // node

        // determine num pixels and total intensity of
        // neighbourhood: a rectangle with sample_tan x sample_norm
        for (i = 0; i <= 1. / a * BOA_.boap.segParam.sample_tan; i++) {
            // determine points on the tangent
            xt = n.getPoint().getX() + (a * i - BOA_.boap.segParam.sample_tan / 2) * tan.getX();
            yt = n.getPoint().getY() + (a * i - BOA_.boap.segParam.sample_tan / 2) * tan.getY();

            for (j = 0; j <= 1. / a * BOA_.boap.segParam.sample_norm / 2; ++j) {
                x = xt + a * j * n.getNormal().getX();
                y = yt + a * j * n.getNormal().getY();

                I_inside += ip.getPixel((int) x, (int) y);
                I_in++;

                x = xt - a * j * n.getNormal().getX();
                y = yt - a * j * n.getNormal().getY();

                I_outside += ip.getPixel((int) x, (int) y);
                I_out++;

                // if (x <= 0 || y <= 0 || x > width || y > height){
                // System.out.println("outer pixel I = " + ip.getPixel((int) x,
                // (int) y));
                // }

            }
        }

        Delta_I = ((double) I_inside / I_in - (double) I_outside / I_out) / 255.;

        // check if node erraneously got inside (pixel value > p.sensitivity)
        // if so, push node outside
        // double check = (double) I_outside / I_out / 255.;

        // if (check > boap.sensitivity) // Delta_I += 0.5 * check;
        // {
        // Delta_I += 0.125 * check;
        // }
        // if contrast is positive compute image force as square root of the
        // contrast
        if (Delta_I > 0.) { // else remains at zero
            result.setX(-Math.sqrt(Delta_I) * n.getNormal().getX());
            result.setY(-Math.sqrt(Delta_I) * n.getNormal().getY());
        }

        return (result);
    }

    /**
     * Expand all snakes while preventing overlaps. Dead snakes are ignored. Count snakes on frame
     * 
     * @param nest
     * @param frame
     * @throws Exception
     */
    public void loosen(final Nest nest, int frame) throws Exception {
        int N = nest.size();
        Snake snakeA, snakeB;

        double[][] prox = new double[N][N]; // dist beween snake centroids,
                                            // triangular
        for (int si = 0; si < N; si++) {
            snakeA = nest.getHandler(si).getLiveSnake();
            snakeA.calcCentroid();
            for (int sj = si + 1; sj < N; sj++) {
                snakeB = nest.getHandler(sj).getLiveSnake();
                snakeB.calcCentroid();
                prox[si][sj] =
                        ExtendedVector2d.lengthP2P(snakeA.getCentroid(), snakeB.getCentroid());
            }
        }

        double stepSize = 0.1;
        double steps = (double) BOA_.boap.segParam.blowup / stepSize;

        for (int i = 0; i < steps; i++) {
            // check for contacts, freeze nodes in contact.
            // Ignore snakes that begin after 'frame'
            for (int si = 0; si < N; si++) {
                snakeA = nest.getHandler(si).getLiveSnake();
                if (!snakeA.alive || frame < nest.getHandler(si).getStartframe()) {
                    continue;
                }
                for (int sj = si + 1; sj < N; sj++) {
                    snakeB = nest.getHandler(sj).getLiveSnake();
                    if (!snakeB.alive || frame < nest.getHandler(si).getStartframe()) {
                        continue;
                    }
                    if (prox[si][sj] > BOA_.boap.proximity) {
                        continue;
                    }
                    freezeProx(snakeA, snakeB);
                }

            }

            // scale up all snakes by one step (if node not frozen, or dead)
            // unless they start at this frame or after
            for (int s = 0; s < N; s++) {
                snakeA = nest.getHandler(s).getLiveSnake();
                if (snakeA.alive && frame > nest.getHandler(s).getStartframe()) {
                    snakeA.scale(stepSize, stepSize, true);
                }
            }

        }

        /*
         * //defreeze snakes for (int s = 0; s < snakes.length; s++) { snakes[s].defreeze(); }
         */
    }

    public void freezeProx(final Snake a, final Snake b) {

        Node bn;
        Node an = a.getHead();
        double prox;

        do {
            bn = b.getHead();
            do {
                if (an.isFrozen() && bn.isFrozen()) {
                    // an = an.getNext();
                    bn = bn.getNext();
                    continue;
                }
                // test proximity and freeze
                prox = ExtendedVector2d.distPointToSegment(an.getPoint(), bn.getPoint(),
                        bn.getNext().getPoint());
                if (prox < BOA_.boap.proxFreeze) {
                    an.freeze();
                    bn.freeze();
                    bn.getNext().freeze();
                    break;
                }
                bn = bn.getNext();
            } while (!bn.isHead());

            an = an.getNext();
        } while (!an.isHead());

    }

    public void implode(final Nest nest, int f) throws Exception {
        // System.out.println("imploding snake");
        SnakeHandler sH;
        Snake snake;
        for (int s = 0; s < nest.size(); s++) {
            sH = nest.getHandler(s);
            snake = sH.getLiveSnake();
            if (snake.alive && f > sH.getStartframe()) {
                snake.implode();
            }
        }
    }
}

/**
 * Represent collection of Snakes
 * 
 * @author rtyson
 *
 */
class Nest {

    private ArrayList<SnakeHandler> sHs;
    private int NSNAKES; /*!< Number of stored snakes in nest  */
    private int ALIVE;
    private int nextID; // handler ID's

    public Nest() {
        NSNAKES = 0;
        ALIVE = 0;
        nextID = 0;
        sHs = new ArrayList<SnakeHandler>();
    }

    public void addHandlers(Roi[] roiArray, int startFrame) {
        int i = 0;
        for (; i < roiArray.length; i++) {
            try {
                sHs.add(new SnakeHandler(roiArray[i], startFrame, nextID));
                nextID++;
                NSNAKES++;
                ALIVE++;
            } catch (Exception e) {
                BOA_.log("A snake failed to initilise");
            }
        }
        BOA_.log("Added " + roiArray.length + " cells at frame " + startFrame);
        BOA_.log("Cells being tracked: " + NSNAKES);
    }

    /**
     * Add ROI objects in Nest Snakes are stored in Nest object in form of
     * SnakeHandler objects kept in \c ArrayList<SnakeHandler> \c sHs field.
     * 
     * @param r ROI object that contain image object to be segmented
     * @param startFrame Current frame
     * @return SnakeHandler object that is also stored in Nest
     */
    public SnakeHandler addHandler(final Roi r, int startFrame) {
        SnakeHandler sH;
        try {
            sH = new SnakeHandler(r, startFrame, nextID);
            sHs.add(sH);
            nextID++;
            NSNAKES++;
            ALIVE++;
            BOA_.log("Added one cell, begining frame " + startFrame);
        } catch (Exception e) {
            BOA_.log("Added cell failed to initilise");
            return null;
        }
        BOA_.log("Cells being tracked: " + NSNAKES);
        return sH;
    }

    public SnakeHandler getHandler(int s) {
        return sHs.get(s);
    }

    public boolean writeSnakes() throws Exception {
        Iterator<SnakeHandler> sHitr = sHs.iterator();
        SnakeHandler sH;
        while (sHitr.hasNext()) {
            sH = (SnakeHandler) sHitr.next();
            sH.setEndFrame();
            if (sH.getStartframe() > sH.getEndFrame()) {
                IJ.error("Snake " + sH.getID() + " not written as its empty. Deleting it.");
                removeHandler(sH);
                continue;
            }
            if (!sH.writeSnakes()) {
                return false;
            }
        }
        return true;
    }

    public void kill(final SnakeHandler sH) {
        sH.kill();
        ALIVE--;
    }

    public void reviveNest() {
        Iterator<SnakeHandler> sHitr = sHs.iterator();
        while (sHitr.hasNext()) {
            SnakeHandler sH = (SnakeHandler) sHitr.next();
            sH.revive();
        }
        ALIVE = NSNAKES;
    }

    public boolean isVacant() {
        if (NSNAKES == 0) {
            return true;
        }
        return false;
    }

    public boolean allDead() {
        if (ALIVE == 0 || NSNAKES == 0) {
            return true;
        }
        return false;
    }

    public void analyse(final ImagePlus oi) {
        OutlineHandler outputH;
        SnakeHandler sH;
        Iterator<SnakeHandler> sHitr = sHs.iterator();
        while (sHitr.hasNext()) {
            sH = (SnakeHandler) sHitr.next();

            File pFile = new File(BOA_.boap.outFile.getParent(),
                    BOA_.boap.fileName + "_" + sH.getID() + ".paQP");
            QParams newQp = new QParams(pFile);
            newQp.readParams();
            outputH = new OutlineHandler(newQp);

            File statsFile = new File(BOA_.boap.outFile.getParent() + File.separator
                    + BOA_.boap.fileName + "_" + sH.getID() + ".stQP.csv");
            new CellStat(outputH, oi, statsFile, BOA_.boap.imageScale,
                    BOA_.boap.imageFrameInterval);
        }
    }

    public void resetNest() {
        // Rset live snakes to ROI's
        reviveNest();
        Iterator<SnakeHandler> sHitr = sHs.iterator();
        while (sHitr.hasNext()) {
            SnakeHandler sH = (SnakeHandler) sHitr.next();
            try {
                sH.reset();
            } catch (Exception e) {
                BOA_.log("Could not reset snake " + sH.getID());
                BOA_.log("Removeing snake " + sH.getID());
                removeHandler(sH);
            }
        }
    }

    public void removeHandler(final SnakeHandler sH) {
        if (sH.isLive()) {
            ALIVE--;
        }
        sHs.remove(sH);
        NSNAKES--;
    }

    int size() {
        return NSNAKES;
    }

    /**
     * Prepare for segmentation from frame \c f
     * 
     * @param f current frame under segmentation
     */
    void resetForFrame(int f) {
        reviveNest();
        Iterator<SnakeHandler> sHitr = sHs.iterator();
        // BOA_.log("Reseting for frame " + f);
        while (sHitr.hasNext()) {
            SnakeHandler sH = (SnakeHandler) sHitr.next();
            try {
                if (f <= sH.getStartframe()) {
                    // BOA_.log("Reset snake " + sH.getID() + " as Roi");
                    sH.reset();
                } else {
                    // BOA_.log("Reset snake " + sH.getID() + " as prev snake");
                    sH.resetForFrame(f);
                }
            } catch (Exception e) {
                BOA_.log("Could not reset snake " + sH.getID());
                BOA_.log("Removeing snake " + sH.getID());
                removeHandler(sH);
            }
        }
    }

    int nbSnakesAt(int frame) {
        // count the snakes that exist at, or after, frame
        int n = 0;
        for (int i = 0; i < NSNAKES; i++) {
            if (sHs.get(i).getStartframe() >= frame) {
                n++;
            }
        }
        return n;
    }

    void addOutlinehandler(final OutlineHandler oH) {
        SnakeHandler sH = addHandler(oH.indexGetOutline(0).asFloatRoi(), oH.getStartFrame());

        Outline o;
        for (int i = oH.getStartFrame(); i <= oH.getEndFrame(); i++) {
            o = oH.getOutline(i);
            sH.storeRoi((PolygonRoi) o.asFloatRoi(), i);
        }
    }
}

/**
 * Store all the snakes computed for one cell across frames and it is responsible
 * for writing them to file.
 * 
 * @author rtyson
 *
 */
class SnakeHandler {
    private static final Logger LOGGER = LogManager.getLogger(SnakeHandler.class.getName());
    private Roi roi; // inital ROI
    private int startFrame;
    private int endFrame;
    private Snake liveSnake;
    private Snake[] finalSnakes; /*!< series of snakes, result of cell segm. and plugin processing*/
    private Snake[] segSnakes; /*!< series of snakes, result of cell segmentation only  */
    private int ID;

    /**
     * Constructor of SnakeHandler. Stores ROI with object for segmentation
     * 
     * @param r ROI with selected object
     * @param frame Current frame for which the ROI is taken
     * @param id Unique Snake ID controlled by Nest object
     * @throws Exception
     */
    public SnakeHandler(final Roi r, int frame, int id) throws Exception {
        startFrame = frame;
        endFrame = BOA_.boap.FRAMES;
        roi = r;
        // snakes array keeps snakes across frames from current to end. Current
        // is that one for which cell has been added
        finalSnakes = new Snake[BOA_.boap.FRAMES - startFrame + 1]; // stored snakes
        segSnakes = new Snake[BOA_.boap.FRAMES - startFrame + 1]; // stored snakes
        ID = id;
        liveSnake = new Snake(r, ID, false);
        backupLiveSnake(frame);
    }

    /**
     * Make copy of \c liveSnake into \c final \c snakes array
     * 
     * @param frame Frame for which \c liveSnake will be copied to
     * @throws BoaException
     */
    public void storeLiveSnake(int frame) throws BoaException {
        // BOA_.log("Store snake " + ID + " at frame " + frame);
        finalSnakes[frame - startFrame] = null; // delete at current frame

        finalSnakes[frame - startFrame] = new Snake(liveSnake, ID);
    }

    /**
     * Stores \c liveSnake (currently processed) in \c segSnakes array. 
     * 
     * For one SnakeHandler there is only one \c liveSnake which is processed "in place" by
     * segmentation methods. It is virtually moved from frame to frame and copied to final snakes
     * after segmentation on current frame and processing by plugins. 
     * It must be backed up for every frame to make possible restoring  original snakes when 
     * active plugin has been deselected. 
     *  
     * @param frame current frame
     * @throws BoaException
     */
    public void backupLiveSnake(int frame) throws BoaException {

        LOGGER.debug("Stored live snake in frame " + frame + " ID " + ID);
        segSnakes[frame - startFrame] = null; // delete at current frame

        segSnakes[frame - startFrame] = new Snake(liveSnake, ID);
    }

    /**
     * Makes copy of \c snake and store it as final snake.
     * 
     * @param snake Snake to store
     * @param frame Frame for which \c liveSnake will be copied to
     * @throws BoaException
     */
    public void storeThisSnake(Snake snake, int frame) throws BoaException {
        // BOA_.log("Store snake " + ID + " at frame " + frame);
        finalSnakes[frame - startFrame] = null; // delete at current frame

        finalSnakes[frame - startFrame] = new Snake(snake, ID);

    }

    public boolean writeSnakes() throws Exception {

        String saveIn = BOA_.boap.orgFile.getParent();
        // System.out.println(boap.orgFile.getParent());
        // if (!boap.orgFile.exists()) {
        // BOA_.log("image is not saved to disk!");
        // saveIn = OpenDialog.getLastDirectory();
        // }

        if (!BOA_.boap.savedOne) {

            SaveDialog sd =
                    new SaveDialog("Save segmentation data...", saveIn, BOA_.boap.fileName, "");

            if (sd.getFileName() == null) {
                BOA_.log("Save canceled");
                return false;
            }
            BOA_.boap.outFile = new File(sd.getDirectory(), sd.getFileName() + "_" + ID + ".snQP");
            BOA_.boap.fileName = sd.getFileName();
            BOA_.boap.savedOne = true;
        } else {
            BOA_.boap.outFile = new File(BOA_.boap.outFile.getParent(),
                    BOA_.boap.fileName + "_" + ID + ".snQP");
        }

        PrintWriter pw = new PrintWriter(new FileWriter(BOA_.boap.outFile), true); // auto
        // flush

        pw.write("#QuimP11 Node data");
        pw.write("\n#Node Position\tX-coord\tY-coord\tOrigin\tG-Origin\tSpeed");
        pw.write("\tFluor_Ch1\tCh1_x\tCh1_y\tFluor_Ch2\tCh2_x\tCh2_y\tFluor_CH3\tCH3_x\tCh3_y\n#");

        Snake s;
        for (int i = startFrame; i <= endFrame; i++) {
            s = getStoredSnake(i);
            s.setPositions(); //
            pw.write("\n#Frame " + i);
            write(pw, i + 1, s.getNODES(), s.getHead());
        }
        pw.close();
        BOA_.boap.writeParams(ID, startFrame, endFrame);

        if (BOA_.boap.oldFormat) {
            writeOldFormats();
        }
        return true;
    }

    private void write(final PrintWriter pw, int frame, int NODES, Node n) {
        pw.print("\n" + NODES);

        do {
            // fluo values (x,y, itensity)
            pw.print("\n" + IJ.d2s(n.position, 6) + "\t" + IJ.d2s(n.getX(), 2) + "\t"
                    + IJ.d2s(n.getY(), 2) + "\t0\t0\t0" + "\t-2\t-2\t-2\t-2\t-2\t-2\t-2\t-2\t-2");
            n = n.getNext();
        } while (!n.isHead());

    }

    private void writeOldFormats() throws Exception {
        // create file to outpurt old format
        File OLD = new File(BOA_.boap.outFile.getParent(), BOA_.boap.fileName + ".dat");
        PrintWriter pw = new PrintWriter(new FileWriter(OLD), true); // auto
                                                                     // flush

        for (int i = 0; i < finalSnakes.length; i++) {
            if (finalSnakes[i] == null) {
                break;
            }
            if (i != 0) {
                pw.print("\n");
            } // no new line at top
            pw.print(finalSnakes[i].getNODES());

            Node n = finalSnakes[i].getHead();
            do {
                pw.print("\n" + IJ.d2s(n.getX(), 6));
                pw.print("\n" + IJ.d2s(n.getY(), 6));
                n = n.getNext();
            } while (!n.isHead());
        }
        pw.close();

        OLD = new File(BOA_.boap.outFile.getParent(), BOA_.boap.fileName + ".dat_tn");
        pw = new PrintWriter(new FileWriter(OLD), true); // auto flush

        for (int i = 0; i < finalSnakes.length; i++) {
            if (finalSnakes[i] == null) {
                break;
            }
            if (i != 0) {
                pw.print("\n");
            } // no new line at top
            pw.print(finalSnakes[i].getNODES());

            Node n = finalSnakes[i].getHead();
            do {
                pw.print("\n" + IJ.d2s(n.getX(), 6));
                pw.print("\n" + IJ.d2s(n.getY(), 6));
                pw.print("\n" + n.getTrackNum());
                n = n.getNext();
            } while (!n.isHead());
        }
        pw.close();

        OLD = new File(BOA_.boap.outFile.getParent(), BOA_.boap.fileName + ".dat1");
        pw = new PrintWriter(new FileWriter(OLD), true); // auto flush

        pw.print(IJ.d2s(BOA_.boap.NMAX, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.delta_t, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.max_iterations, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.getMin_dist(), 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.getMax_dist(), 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.blowup, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.sample_tan, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.sample_norm, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.vel_crit, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.f_central, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.f_contract, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.f_friction, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.segParam.f_image, 6) + "\n");
        pw.print(IJ.d2s(1.0, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.sensitivity, 6) + "\n");
        pw.print(IJ.d2s(BOA_.boap.cut_every, 6) + "\n");
        pw.print("100");

        pw.close();
    }

    public Snake getLiveSnake() {
        return liveSnake;
    }

    public Snake getBackupSnake(int f) {
        LOGGER.debug("Asked for backup snake at frame " + f + " ID " + ID);
        if (f - startFrame < 0) {
            LOGGER.warn("Tried to access negative frame store");
            return null;
        }
        return segSnakes[f - startFrame];
    }

    public Snake getStoredSnake(int f) {
        if (f - startFrame < 0) {
            BOA_.log("Tried to access negative frame store\n\tframe:" + f + "\n\tsnakeID:" + ID);
            return null;
        }
        // BOA_.log("Fetch stored snake " + ID + " frame " + f);
        return finalSnakes[f - startFrame];
    }

    boolean isStoredAt(int f) {
        if (f - startFrame < 0) {
            return false;
        } else if (finalSnakes[f - startFrame] == null) {
            return false;
        } else {
            return true;
        }
    }

    public int snakeReader(final File inFile) throws Exception {
        String thisLine;
        int N;
        int index;
        double x, y;
        Node head, n, prevn;
        int s = 0;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(inFile));

            while ((thisLine = br.readLine()) != null) {
                index = 0;
                head = new Node(index); // dummy head node
                head.setHead(true);
                prevn = head;
                index++;

                N = (int) Tool.s2d(thisLine);

                for (int i = 0; i < N; i++) {
                    x = Tool.s2d(br.readLine());
                    y = Tool.s2d(br.readLine());

                    n = new Node(index);
                    n.setX(x);
                    n.setY(y);
                    index++;

                    prevn.setNext(n);
                    n.setPrev(prevn);

                    prevn = n;

                }
                // link tail to head
                prevn.setNext(head);
                head.setPrev(prevn);

                finalSnakes[s] = new Snake(head, N + 1, ID); // dont forget the head
                // node
                s++;
            } // end while
        } catch (IOException e) {
            System.err.println("Error: " + e);
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        return 1;
    }

    public void revive() {
        liveSnake.alive = true;
    }

    public void kill() {
        liveSnake.alive = false;
    }

    public void reset() throws Exception {
        liveSnake = new Snake(roi, ID, false);
        // snakes = new Snake[boap.FRAMES - startFrame + 1]; // stored snakes
    }

    public int getID() {
        return ID;
    }

    public boolean isLive() {
        return liveSnake.alive;
    }

    void deleteStoreAt(int frame) {
        if (frame - startFrame < 0) {
            BOA_.log(
                    "Tried to delete negative frame store\n\tframe:" + frame + "\n\tsnakeID:" + ID);
        } else {
            finalSnakes[frame - startFrame] = null;
        }
    }

    void deleteStoreFrom(int frame) {
        for (int i = frame; i <= BOA_.boap.FRAMES; i++) {
            deleteStoreAt(i);
        }
    }

    void storeAt(final Snake s, int frame) {
        s.calcCentroid();
        if (frame - startFrame < 0) {
            BOA_.log("Tried to store at negative frame\n\tframe:" + frame + "\n\tsnakeID:" + ID);
        } else {
            // BOA_.log("Storing snake " + ID + " frame " + frame);
            finalSnakes[frame - startFrame] = s;
        }
    }

    int getStartframe() {
        return startFrame;
    }

    int getEndFrame() {
        return endFrame;
    }

    /**
     * Prepare current frame \c for segmentation
     * 
     * Create \c liveSnake using final snake stored in previous frame
     * 
     * @param f Current segmented frame
     */
    void resetForFrame(int f) {
        try {
            if (BOA_.boap.segParam.use_previous_snake) {
                // set to last segmentation ready for blowup
                liveSnake = new Snake((PolygonRoi) this.getStoredSnake(f - 1).asFloatRoi(), ID);
            } else {
                liveSnake = new Snake(roi, ID, false);
            }
        } catch (Exception e) {
            BOA_.log("Could not reset live snake form frame" + f);
        }
    }

    void storeRoi(final PolygonRoi r, int f) {
        try {
            Snake snake = new Snake(r, ID);
            snake.calcCentroid();
            this.deleteStoreAt(f);
            storeAt(snake, f);
            // BOA_.log("Storing ROI snake " + ID + " frame " + f);
        } catch (Exception e) {
            BOA_.log("Could not stor ROI");
            e.printStackTrace();
        }
    }

    void setEndFrame() {
        // find the first missing contour and set end frame to the previous one

        for (int i = startFrame; i <= BOA_.boap.FRAMES; i++) {
            if (!isStoredAt(i)) {
                endFrame = i - 1;
                return;
            }
        }
        endFrame = BOA_.boap.FRAMES;
    }
}

/**
 * Low level snake definition. Form snake from Node objects. Snake is defined by
 * first \c head node. Remaining nodes are in bidirectional linked list.
 * 
 * @author rtyson
 *
 */
class Snake {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger(Snake.class.getName());
    public boolean alive; // snake is alive
    public int snakeID;
    private int nextTrackNumber = 1; // node ID's
    private Node head; // first node in bidirectional linked list, always
                       // maintained
    private int NODES; // number of nodes
    public double startingNnodes; // how many nodes at start of segmentation
    // used as a reference for node limit
    private int FROZEN; // number of nodes frozen
    private double minX, minY, maxX, maxY;
    private Rectangle bounds = new Rectangle(); // snake bounds
    private ExtendedVector2d centroid;

    /**
     * Create a snake from existing linked list (at least one head node)
     * 
     * @param h Node of list
     * @param N Number of nodes
     * @param id Unique snake ID related to object being segmented.
     * @throws Exception
     */
    public Snake(final Node h, int N, int id) throws BoaException {
        //
        snakeID = id;
        head = h;
        NODES = N;
        FROZEN = N;
        nextTrackNumber = N + 1;
        // colour = QColor.lightColor();
        centroid = new ExtendedVector2d(0d, 0d);
        this.calcCentroid();

        removeNode(head);
        this.makeAntiClockwise();
        this.updateNormales();
        alive = true;
        startingNnodes = NODES / 100.; // as 1%. limit to X%
        // calcOrientation();
    }

    /**
     * Copy constructor
     * 
     * @param snake Snake to be duplicated
     * @param id New id
     * @throws BoaException 
     */
    public Snake(final Snake snake, int id) throws BoaException {

        head = new Node(0); // dummy head node
        head.setHead(true);

        Node prev = head;
        Node nn;
        Node sn = snake.getHead();
        do {
            nn = new Node(sn.getTrackNum());
            nn.setX(sn.getX());
            nn.setY(sn.getY());

            nn.setPrev(prev);
            prev.setNext(nn);

            prev = nn;
            sn = sn.getNext();
        } while (!sn.isHead());
        nn.setNext(head); // link round tail
        head.setPrev(nn);

        snakeID = id;
        NODES = snake.getNODES() + 1;
        FROZEN = NODES;
        nextTrackNumber = NODES + 1;
        centroid = new ExtendedVector2d(0d, 0d);
        removeNode(head);
        this.makeAntiClockwise();
        this.updateNormales();
        alive = snake.alive;
        startingNnodes = snake.startingNnodes;
        this.calcCentroid();
        /*
         * from initializearraylist head = new Node(0); NODES = 1; FROZEN = 0; head.setPrev(head);
         * head.setNext(head); head.setHead(true);
         * 
         * Node node; for (Point2d el : p) { node = new Node(el.getX(), el.getY(),
         * nextTrackNumber++); addNode(node); }
         * 
         * removeNode(head); this.makeAntiClockwise(); updateNormales();
         */
    }

    /**
     * Create snake from ROI
     * 
     * @param R ROI with object to be segmented
     * @param id Unique ID of snake related to object being segmented.
     * @param direct
     * @throws Exception
     */
    public Snake(final Roi R, int id, boolean direct) throws Exception {
        // place nodes in a circle
        snakeID = id;
        if (R.getType() == Roi.RECTANGLE || R.getType() == Roi.POLYGON) {
            if (direct) {
                intializePolygonDirect(R.getFloatPolygon());
            } else {
                intializePolygon(R.getFloatPolygon());
            }
        } else {
            Rectangle Rect = R.getBounds();
            int xc = Rect.x + Rect.width / 2;
            int yc = Rect.y + Rect.height / 2;
            int Rx = Rect.width / 2;
            int Ry = Rect.height / 2;

            intializeOval(0, xc, yc, Rx, Ry, BOA_.boap.segParam.getNodeRes() / 2);
        }
        startingNnodes = NODES / 100.; // as 1%. limit to X%
        alive = true;
        // colour = QColor.lightColor();
        // calcOrientation();
        this.calcCentroid();
    }

    /**
     * @see Snake(Roi, int, boolean)
     * @param R
     * @param id
     * @throws Exception
     */
    public Snake(final PolygonRoi R, int id) throws Exception {
        snakeID = id;
        intializeFloat(R.getFloatPolygon());
        startingNnodes = NODES / 100.; // as 1%. limit to X%
        alive = true;
        // colour = QColor.lightColor();
        // calcOrientation();
        this.calcCentroid();
    }

    /**
     * Construct Snake object from list of nodes
     * 
     * @param list list of nodes as Vector2d
     * @param id id of Snake
     * @throws Exception
     */
    public Snake(final List<Point2d> list, int id) throws Exception {
        snakeID = id;
        initializeArrayList(list);
        startingNnodes = NODES / 100;
        alive = true;
        this.calcCentroid();
    }

    /**
     * Initializes \c Node list from ROIs other than polygons For non-polygon
     * ROIs ellipse is used as first approximation of segmented shape.
     * Parameters of ellipse are estimated usually using parameters of bounding
     * box of user ROI This method differs from other \c initialize* methods by
     * input data which do not contain nodes but the are defined analytically
     * 
     * @param t index of node
     * @param xc center of ellipse
     * @param yc center of ellipse
     * @param Rx ellipse diameter
     * @param Ry ellipse diameter
     * @param s number of nodes
     * 
     * @throws Exception
     */
    private void intializeOval(int t, int xc, int yc, int Rx, int Ry, double s) throws Exception {
        head = new Node(t); // make a dummy head node for list initialization
        NODES = 1;
        FROZEN = 0;
        head.setPrev(head); // link head to itself
        head.setNext(head);
        head.setHead(true);

        double theta = 2.0 / (double) ((Rx + Ry) / 2);

        // nodes are added in behind the head node
        Node node;
        for (double a = 0.0; a < (2 * Math.PI); a += s * theta) {
            node = new Node(nextTrackNumber);
            nextTrackNumber++;
            node.getPoint().setX((int) (xc + Rx * Math.cos(a)));
            node.getPoint().setY((int) (yc + Ry * Math.sin(a)));
            addNode(node);
        }
        removeNode(head); // remove dummy head node
        this.makeAntiClockwise();
        updateNormales();
    }

    /**
     * Initializes \c Node list from polygon Each edge of input polygon is
     * divided on uk.ac.warwick.wsbc.QuimP.boap.nodeRes nodes
     * 
     * @param p Polygon extracted from IJ ROI
     * @throws Exception
     */
    private void intializePolygon(final FloatPolygon p) throws Exception {
        // System.out.println("poly with node distance");
        head = new Node(0); // make a dummy head node for list initialization
        NODES = 1;
        FROZEN = 0;
        head.setPrev(head); // link head to itself
        head.setNext(head);
        head.setHead(true);

        Node node;
        int j, nn;
        double x, y, spacing;
        ExtendedVector2d a, b, u;
        for (int i = 0; i < p.npoints; i++) {
            j = ((i + 1) % (p.npoints)); // for last i point we turn for first
                                         // one closing polygon
            a = new ExtendedVector2d(p.xpoints[i], p.ypoints[i]);// vectors ab
                                                                 // define edge
            b = new ExtendedVector2d(p.xpoints[j], p.ypoints[j]);

            nn = (int) Math
                    .ceil(ExtendedVector2d.lengthP2P(a, b) / BOA_.boap.segParam.getNodeRes());
            spacing = ExtendedVector2d.lengthP2P(a, b) / (double) nn;
            u = ExtendedVector2d.unitVector(a, b);
            u.multiply(spacing); // required distance between points

            for (int s = 0; s < nn; s++) { // place nodes along edge
                node = new Node(nextTrackNumber);
                nextTrackNumber++;
                x = a.getX() + (double) s * u.getX();
                y = a.getY() + (double) s * u.getY();
                node.setX(x);
                node.setY(y);
                addNode(node);
            }
        }
        removeNode(head); // remove dummy head node new head will be set
        this.makeAntiClockwise();
        updateNormales();
    }

    /**
     * Initializes \c Node list from polygon Does not refine points. Use only
     * those nodes available in polygon
     * 
     * @param p Polygon extracted from IJ ROI
     * @throws Exception
     * @see intializePolygon(FloatPolygon)
     */
    private void intializePolygonDirect(final FloatPolygon p) throws Exception {
        // System.out.println("poly direct");
        head = new Node(0); // make a dummy head node for list initialization
        NODES = 1;
        FROZEN = 0;
        head.setPrev(head); // link head to itself
        head.setNext(head);
        head.setHead(true);

        Node node;
        for (int i = 0; i < p.npoints; i++) {
            node = new Node((double) p.xpoints[i], (double) p.ypoints[i], nextTrackNumber++);
            addNode(node);
        }

        removeNode(head); // remove dummy head node
        this.makeAntiClockwise();
        updateNormales();
    }

    /**
     * @see intializePolygonDirect(FloatPolygon)
     * @param p
     * @throws Exception
     * @todo This method is the same as intializePolygonDirect(FloatPolygon)
     */
    private void intializeFloat(final FloatPolygon p) throws Exception {
        // System.out.println("poly direct");
        head = new Node(0); // make a dummy head node
        NODES = 1;
        FROZEN = 0;
        head.setPrev(head); // link head to itself
        head.setNext(head);
        head.setHead(true);

        Node node;
        for (int i = 0; i < p.npoints; i++) {
            node = new Node((double) p.xpoints[i], (double) p.ypoints[i], nextTrackNumber++);
            addNode(node);
        }

        removeNode(head); // remove dummy head node
        this.makeAntiClockwise();
        updateNormales();
    }

    /**
     * Initialize snake from List of Vector2d objects
     * 
     * @param p
     * @throws Exception
     */
    private void initializeArrayList(final List<Point2d> p) throws Exception {
        head = new Node(0);
        NODES = 1;
        FROZEN = 0;
        head.setPrev(head);
        head.setNext(head);
        head.setHead(true);

        Node node;
        for (Point2d el : p) {
            node = new Node(el.getX(), el.getY(), nextTrackNumber++);
            addNode(node);
        }

        removeNode(head);
        this.makeAntiClockwise();
        updateNormales();
    }

    public void printSnake() {
        System.out.println("Print Nodes (" + NODES + ")");
        int i = 0;
        Node n = head;
        do {
            int x = (int) n.getPoint().getX();
            int y = (int) n.getPoint().getY();
            System.out.println(i + " Node " + n.getTrackNum() + ", x:" + x + ", y:" + y + ", vel: "
                    + n.getVel().length());
            n = n.getNext();
            i++;
        } while (!n.isHead());
        if (i != NODES) {
            System.out.println("NODES and linked list dont tally!!");
        }
    }

    /**
     * Get head of current Snake
     * 
     * @return Node representing head of Snake
     */
    public Node getHead() {
        return head;
    }

    /**
     * Get number of nodes forming current Snake
     * 
     * @return number of nodes in current Snake
     */
    public int getNODES() {
        return NODES;
    }

    /**
     * Unfreeze all nodes
     */
    public void defreeze() {
        Node n = head;
        do {
            n.unfreeze();
            n = n.getNext();
        } while (!n.isHead());
        FROZEN = 0;
    }

    /**
     * Freeze a specific node
     * 
     * @param n Node to freeze
     */
    public void freezeNode(Node n) {
        if (!n.isFrozen()) {
            n.freeze();
            FROZEN++;
        }
    }

    /**
     * Unfreeze a specific node
     * 
     * @param n Node to unfreeze
     */
    public void unfreezeNode(Node n) {
        if (n.isFrozen()) {
            n.unfreeze();
            FROZEN--;
        }
    }

    /**
     * Check if all nodes are frozen
     * 
     * @return \c true if all nodes are frozen
     */
    public boolean isFrozen() {
        if (FROZEN == NODES) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add node before head node assuring that list has closed loop. If initial
     * list condition is defined in such way:
     * 
     * @code
     * head = new Node(0); //make a dummy head node NODES = 1; FROZEN = 0;
     * head.setPrev(head); // link head to itself head.setNext(head);
     * head.setHead(true);
     * @endcode
     * 
     * The \c addNode will produce closed bidirectional linked list.
     * From first Node it is possible to reach last one by calling
     * Node::getNext() and from the last one, first should be accessible
     * by calling Node::getPrev()
     * 
     * @param newNode Node to be added to list
     * 
     * @remarks For initialization only
     */
    private void addNode(final Node newNode) {
        Node prevNode = head.getPrev();
        newNode.setPrev(prevNode);
        newNode.setNext(head);

        head.setPrev(newNode);
        prevNode.setNext(newNode);
        NODES++;
    }

    /**
     * Remove selected node from list Check if removed node was head and if it
     * was, the new head is randomly selected
     * 
     * @param n Node to remove
     * 
     * @throws Exception
     */
    final public void removeNode(Node n) throws BoaException {
        if (NODES <= 3) {
            throw new BoaException(
                    "removeNode: Did not remove node. " + NODES + " nodes remaining.", 0, 2);
        }

        if (n.isFrozen()) {
            FROZEN--;
        }
        // removes node n and links neighbours together
        n.getPrev().setNext(n.getNext());
        n.getNext().setPrev(n.getPrev());

        // if removing head randomly assign a neighbour as new head
        if (n.isHead()) {
            if (Math.random() < 0.5) {
                head = n.getPrev();
            } else {
                head = n.getNext();
            }
            head.setHead(true);
        }

        NODES--;

        n.getPrev().updateNormale();
        n.getNext().updateNormale();
        n = null; // FIXME Does it have meaning here?
    }

    /**
     * Update all node normals Called after modification of Snake nodes
     */
    public void updateNormales() {
        Node n = head;
        do {
            n.updateNormale();
            n = n.getNext();
        } while (!n.isHead());
    }

    public void blowup() throws Exception {
        scale(BOA_.boap.segParam.blowup, 4, true);
    }

    public void shrinkSnake() throws BoaException {
        scale(-BOA_.boap.segParam.finalShrink, 0.5, false);
    }

    public void implode() throws Exception {
        // calculate centroid
        double cx;
        double cy;
        cx = 0.0;
        cy = 0.0;
        Node n = head;
        do {
            cx += n.getX();
            cy += n.getY();
            n = n.getNext();
        } while (!n.isHead());
        cx = cx / NODES;
        cy = cy / NODES;

        intializeOval(nextTrackNumber, (int) cx, (int) cy, 4, 4, 1);
    }

    private double calcArea() {
        double area, sum;
        sum = 0.0;
        Node n = head;
        Node np1 = n.getNext();
        do {
            sum += (n.getX() * np1.getY()) - (np1.getX() * n.getY());
            n = n.getNext();
            np1 = n.getNext(); // note: n is reset on prev line

        } while (!n.isHead());
        area = 0.5 * sum;
        return area;
    }

    public void scale(double amount, double stepSize, boolean correct) throws BoaException {
        if (amount == 0)
            return;
        // make sure snake access is clockwise
        Node.setClockwise(true);
        // scale the snake by 'amount', in increments of 'stepsize'
        if (amount > 0) {
            stepSize *= -1; // scale down if amount negative
        }
        double steps = Math.abs(amount / stepSize);
        // IJ.log(""+steps);
        Node n;
        int j;
        for (j = 0; j < steps; j++) {
            n = head;
            do {
                if (!n.isFrozen()) {
                    n.setX(n.getX() + stepSize * n.getNormal().getX());
                    n.setY(n.getY() + stepSize * n.getNormal().getY());
                }
                n = n.getNext();
            } while (!n.isHead());
            if (correct) {
                correctDistance(false);
            }
            cutLoops();
            updateNormales();
        }
    }

    /**
     * Cut out a loop Insert a new node at cut point
     */
    public void cutLoops() {
        // System.out.println("cutting loops");
        int MAXINTERVAL = 12; // how far ahead do you check for a loop
        int interval, state;

        Node nA, nB;
        double[] intersect = new double[2];
        Node newN;

        boolean cutHead;

        nA = head;
        do {
            cutHead = (nA.getNext().isHead()) ? true : false;
            nB = nA.getNext().getNext(); // don't check next edge as they can't
                                         // cross, but do touch

            // always leave 3 nodes, at least
            interval = (NODES > MAXINTERVAL + 3) ? MAXINTERVAL : (NODES - 3);

            for (int i = 0; i < interval; i++) {
                if (nB.isHead()) {
                    cutHead = true;
                }
                state = ExtendedVector2d.segmentIntersection(nA.getX(), nA.getY(),
                        nA.getNext().getX(), nA.getNext().getY(), nB.getX(), nB.getY(),
                        nB.getNext().getX(), nB.getNext().getY(), intersect);
                if (state == 1) {
                    // System.out.println("CutLoops: cut out a loop");
                    newN = this.insertNode(nA);
                    newN.setX(intersect[0]);
                    newN.setY(intersect[1]);

                    newN.setNext(nB.getNext());
                    nB.getNext().setPrev(newN);

                    newN.updateNormale();
                    nB.getNext().updateNormale();

                    // set velocity
                    newN.setVel(nB.getVel());
                    if (newN.getVel().length() < BOA_.boap.segParam.vel_crit) {
                        newN.getVel().makeUnit();
                        newN.getVel().multiply(BOA_.boap.segParam.vel_crit * 1.5);
                    }

                    if (cutHead) {
                        newN.setHead(true); // put a new head in
                        head = newN;
                    }

                    NODES -= (i + 2); // the one skipped and the current one
                    break;
                }
                nB = nB.getNext();
            }
            nA = nA.getNext();
        } while (!nA.isHead());

        // this.checkNodeNumber();
        // System.out.println("done cutting loops");
    }

    /**
     * Cut out intersects. Done once at the end of each frame to cut out any
     * parts of the contour that self intersect. Similar to cutLoops, but check
     * all edges (NODES / 2) and cuts out the smallest section
     * 
     * @see cutLoops()
     */
    public void cutIntersects() {

        int interval, state;

        Node nA, nB;
        double[] intersect = new double[2];
        Node newN;

        boolean cutHead;

        nA = head;
        do {
            cutHead = (nA.getNext().isHead()) ? true : false;
            nB = nA.getNext().getNext();// don't check next edge as they can't
                                        // cross, but do touch
            interval = (NODES > 6) ? NODES / 2 : 2; // always leave 3 nodes, at
                                                    // least

            for (int i = 2; i < interval; i++) {
                if (nB.isHead()) {
                    cutHead = true;
                }
                state = ExtendedVector2d.segmentIntersection(nA.getX(), nA.getY(),
                        nA.getNext().getX(), nA.getNext().getY(), nB.getX(), nB.getY(),
                        nB.getNext().getX(), nB.getNext().getY(), intersect);
                if (state == 1) {
                    // System.out.println("CutIntersect: cut out an intersect:
                    // x0: " +
                    // nA.getX() + ", y0:" + nA.getY()+ ", x1 :"
                    // +nA.getNext().getX()+ ", y1: " +nA.getNext().getY() +
                    // ", x2: "+nB.getX()+ ", y2: " + nB.getY()+ ", x3: "
                    // +nB.getNext().getX()+ ", y3: " + nB.getNext().getY());

                    newN = this.insertNode(nA);
                    newN.setX(intersect[0]);
                    newN.setY(intersect[1]);

                    newN.setNext(nB.getNext());
                    nB.getNext().setPrev(newN);

                    newN.updateNormale();
                    nB.getNext().updateNormale();

                    if (cutHead) {
                        newN.setHead(true); // put a new head in
                        head = newN;
                    }

                    NODES -= (i);
                    break;
                }
                nB = nB.getNext();
            }

            nA = nA.getNext();
        } while (!nA.isHead());
    }

    /**
     * @deprecated Old version of cutLoops()
     */
    public void cutLoopsOLD() {

        int i;

        double diffX, diffY, diffXp, diffYp;
        Node node1, node2, right1, right2;
        boolean ishead; // look for head node in section to be cut
        // check the next INTERVALL nodes for cross-overs
        int INTERVALL = 10; // 8 //20

        node1 = head;
        do {
            ishead = false;
            right1 = node1.getNext();
            node2 = right1.getNext();
            right2 = node2.getNext();

            diffX = right1.getPoint().getX() - node1.getPoint().getX();
            diffY = right1.getPoint().getY() - node1.getPoint().getY();

            for (i = 1; i <= INTERVALL; ++i) {
                // see if the head node will be cut out
                if (node2.isHead() || right1.isHead()) {
                    ishead = true;
                }
                diffXp = right2.getPoint().getX() - node2.getPoint().getX();
                diffYp = right2.getPoint().getY() - node2.getPoint().getY();

                if ((NODES - (i + 1)) < 4) {
                    break;
                }
                if (node1.getTrackNum() == right2.getTrackNum()) { // dont go
                                                                   // past node1
                    break;
                } else if (((diffX * node2.getY() - diffY * node2.getX()) < (diffX * node1.getY()
                        - diffY * node1.getX())
                        ^ (diffX * right2.getY() - diffY * right2.getX()) < (diffX * node1.getY()
                                - diffY * node1.getX()))
                        & ((diffXp * node1.getY() - diffYp * node1.getX()) < (diffXp * node2.getY()
                                - diffYp * node2.getX())
                                ^ (diffXp * right1.getY()
                                        - diffYp * right1.getX()) < (diffXp * node2.getY()
                                                - diffYp * node2.getX()))) {

                    // join node1 to right 2
                    // int node1index = Contour.getNodeIndex(node1); //debug
                    // int right2index = Contour.getNodeIndex(right2); //debug

                    // IJ.log("Cut Loop! cut from node1 " + node1index + " to
                    // right2 " + right2index + " interval " + i);
                    node1.setNext(right2);
                    right2.setPrev(node1);
                    node1.updateNormale();
                    right2.updateNormale();
                    NODES -= i + 1; // set number of nodes

                    if (ishead) {
                        head = right2;
                        right2.setHead(true);
                    }
                    break;
                }
                node2 = node2.getNext();
                right2 = right2.getNext();
            }
            node1 = node1.getNext(); // next node will be right2 if it cut

        } while (!node1.isHead());
        // if (NODES < 4) {
        // // System.out.println("CutLoops. Nodes left after cuts: " + NODES);
        // }
    }

    /**
     * @deprecated Old version of correctDistance(boolean)
     * @throws Exception
     */
    public void correctDistanceOLD() throws Exception {
        // ensure nodes are between maxDist and minDist apart, add remove nodes
        // as required

        double Di, avg_dist, InsX, InsY, InsNormX, InsNormY, rand;
        ExtendedVector2d tan;

        // choose a random direction to process the chain
        Node.randDirection();

        avg_dist = 0.5 * (BOA_.boap.getMin_dist() + BOA_.boap.getMax_dist()); // compute
        // average
        // distance

        Node n = head;
        Node n_neigh = n.getNext(); // either the left or right neighbour
        do {
            // compute tangent
            tan = ExtendedVector2d.vecP2P(n.getPoint(), n_neigh.getPoint());

            // compute Distance
            Di = tan.length();

            if (Di > 2. * avg_dist) { // distance greater than DistMax: add in
                                      // node
                Node nIns = insertNode(n);
                nIns.setVel(n.getVel());
                nIns.getVel().makeUnit();
                nIns.getVel().multiply(BOA_.boap.segParam.vel_crit * 2);

                // V2. random postion on average normale
                InsNormX = 0.5 * (n.getNormal().getX() + n_neigh.getNormal().getX());
                InsNormY = 0.5 * (n.getNormal().getY() + n_neigh.getNormal().getY());
                // move along -ve normale rand amount at least 0.05)
                rand = 0.05 + (-2. * Math.random());
                InsX = (rand * InsNormX) + (0.5 * (n.getX() + n_neigh.getX()));
                InsY = (rand * InsNormY) + (0.5 * (n.getY() + n_neigh.getY()));

                nIns.getPoint().setX(InsX);
                nIns.getPoint().setY(InsY);

                // update normals of those nodes effected
                nIns.updateNormale();
                n.updateNormale();
                n.getNext().updateNormale();
                n.getNext().getNext().updateNormale();
                n = nIns;

            } else if (Di < BOA_.boap.getMin_dist() && NODES >= 4) { // Minimum Nodes
                // is 3
                removeNode(n_neigh); // removes Node n_neigh
                n_neigh = n.getNext();
            }

            n = n.getNext();
            n_neigh = n_neigh.getNext();
        } while (!n.isHead());

        Node.setClockwise(true); // reset to clockwise (although shouldnt effect
                                 // things??)
    }

    /**
     * Ensure nodes are between \c maxDist and \c minDist apart, add remove
     * nodes as required
     * 
     * @param shiftNewNode
     * @throws Exception
     */
    public void correctDistance(boolean shiftNewNode) throws BoaException {
        Node.randDirection(); // choose a random direction to process the chain

        ExtendedVector2d tanL, tanR, tanLR, npos; //
        double dL, dR, dLR, tmp;

        Node nC = head;
        Node nL, nR; // neighbours

        do {

            nL = nC.getPrev(); // left neighbour
            nR = nC.getNext(); // left neighbour

            // compute tangent
            tanL = ExtendedVector2d.vecP2P(nL.getPoint(), nC.getPoint());
            tanR = ExtendedVector2d.vecP2P(nC.getPoint(), nR.getPoint());
            tanLR = ExtendedVector2d.vecP2P(nL.getPoint(), nR.getPoint());
            dL = tanL.length();
            dR = tanR.length();
            dLR = tanLR.length();

            if (dL < BOA_.boap.getMin_dist() || dR < BOA_.boap.getMin_dist()) {
                // nC is to close to a neigbour
                if (dLR > 2 * BOA_.boap.getMin_dist()) {

                    // move nC to middle
                    npos = new ExtendedVector2d(tanLR.getX(), tanLR.getY());
                    npos.multiply(0.501); // half
                    npos.addVec(nL.getPoint());

                    nC.setX(npos.getX());
                    nC.setY(npos.getY());

                    // tmp = Math.sqrt((dL*dL) - ((dLR/2.)*(dLR/2.)));
                    // System.out.println("too close, move to middle, tmp:
                    // "+tmp);

                    tmp = Math.sin(ExtendedVector2d.angle(tanL, tanLR)) * dL;
                    // tmp = Vec2d.distPointToSegment(nC.getPoint(),
                    // nL.getPoint(), nR.getPoint());
                    nC.getNormal().multiply(-tmp);
                    nC.getPoint().addVec(nC.getNormal());

                    nC.updateNormale();
                    nL.updateNormale();
                    nR.updateNormale();
                    this.unfreezeNode(nC);

                } else {
                    // delete nC
                    // System.out.println("delete node");
                    removeNode(nC);
                    nL.updateNormale();
                    nR.updateNormale();
                    if (nR.isHead())
                        break;
                    nC = nR.getNext();
                    continue;
                }
            }
            if (dL > BOA_.boap.getMax_dist()) {

                // System.out.println("1357-insert node");
                Node nIns = insertNode(nL);
                nIns.setVel(nL.getVel());
                nIns.getVel().addVec(nC.getVel());
                nIns.getVel().multiply(0.5);
                if (nIns.getVel().length() < BOA_.boap.segParam.vel_crit) {
                    nIns.getVel().makeUnit();
                    nIns.getVel().multiply(BOA_.boap.segParam.vel_crit * 1.5);
                }

                npos = new ExtendedVector2d(tanL.getX(), tanL.getY());
                npos.multiply(0.51);
                npos.addVec(nL.getPoint());

                nIns.setX(npos.getX());
                nIns.setY(npos.getY());
                nIns.updateNormale();
                if (shiftNewNode) {
                    nIns.getNormal().multiply(-2); // move out a bit
                    nIns.getPoint().addVec(nIns.getNormal());
                    nIns.updateNormale();
                }
                nL.updateNormale();
                nR.updateNormale();
                nC.updateNormale();

            }

            nC = nC.getNext();
        } while (!nC.isHead());

        Node.setClockwise(true); // reset to clockwise (although shouldnt effect
                                 // things??)
    }

    /**
     * Insert node after node \c n
     */
    public Node insertNode(final Node n) {
        Node newNode = new Node(nextTrackNumber);
        nextTrackNumber++;
        newNode.setNext(n.getNext());
        newNode.setPrev(n);
        n.getNext().setPrev(newNode);
        n.setNext(newNode);
        NODES++;

        return newNode;
    }

    /**
     * Return current \c snake as polygon
     */
    public Polygon asPolygon() {
        Polygon pol = new Polygon();
        Node n = head;

        do {
            pol.addPoint((int) Math.floor(n.getX() + 0.5), (int) Math.floor(n.getY() + 0.5));
            n = n.getNext();
        } while (!n.isHead());

        return pol;
    }

    public void setPositions() {
        double length = getLength();
        double d = 0.;

        Node v = head;
        do {
            v.position = d / length;
            d = d + ExtendedVector2d.lengthP2P(v.getPoint(), v.getNext().getPoint());
            v = v.getNext();
        } while (!v.isHead());
    }

    /**
     * Add up lengths between all verts
     * 
     * @return length of snake
     */
    public double getLength() {
        Node v = head;
        double length = 0.0;
        do {
            length += ExtendedVector2d.lengthP2P(v.getPoint(), v.getNext().getPoint());
            v = v.getNext();
        } while (!v.isHead());
        return length;
    }

    Roi asIntRoi() {
        Polygon p = asPolygon();
        Roi r = new PolygonRoi(p, PolygonRoi.POLYGON);
        return r;
    }

    Roi asFloatRoi() {

        float[] x = new float[NODES];
        float[] y = new float[NODES];

        Node n = head;
        int i = 0;
        do {
            x[i] = (float) n.getX();
            y[i] = (float) n.getY();
            i++;
            n = n.getNext();
        } while (!n.isHead());
        return new PolygonRoi(x, y, NODES, Roi.POLYGON);
    }

    Roi asPolyLine() {
        float[] x = new float[NODES];
        float[] y = new float[NODES];

        Node n = head;
        int i = 0;
        do {
            x[i] = (float) n.getX();
            y[i] = (float) n.getY();
            i++;
            n = n.getNext();
        } while (!n.isHead());
        return new PolygonRoi(x, y, NODES, Roi.POLYLINE);
    }

    /**
     * Returns current Snake as list of Nodes (copy)
     * 
     * @return List of Vector2d objects representing coordinates of Snake Nodes
     */
    public List<Point2d> asList() {
        List<Point2d> al = new ArrayList<Point2d>(NODES);
        // iterate over nodes at Snake
        Node n = head;
        do {
            al.add(new Point2d(n.getX(), n.getY()));
            n = n.getNext();
        } while (!n.isHead());
        return al;
    }

    public Rectangle getBounds() {
        // change tp asPolygon, and get bounds
        Node n = head;
        minX = n.getX();
        maxX = n.getX();
        minY = n.getY();
        maxY = n.getY();
        n = n.getNext();
        do {
            if (n.getX() > maxX) {
                maxX = n.getX();
            }
            if (n.getX() < minX) {
                minX = n.getX();
            }
            if (n.getY() > maxY) {
                maxY = n.getY();
            }
            if (n.getY() < minY) {
                minY = n.getY();
            }
            n = n.getNext();
        } while (!n.isHead());

        bounds.setBounds((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
        return bounds;
    }

    /**
     * Count the nodes and check that NODES matches
     * 
     * @return \c true if counted nodes matches \c NODES
     */
    public boolean checkNodeNumber() {
        Node n = head;
        int count = 0;
        do {
            count++;
            n = n.getNext();
        } while (!n.isHead());

        if (count != NODES) {
            System.out.println("Node number wrong. NODES:" + NODES + " .actual: " + count);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if there is a head node
     * 
     * @return \c true if there is head of snake
     */
    public boolean checkIsHead() {
        // make sure there is a head node
        Node n = head;
        int count = 0;
        do {
            if (count++ > 10000) {
                System.out.println("Head lost!!!!");
                return false;
            }
            n = n.getNext();
        } while (!n.isHead());
        return true;
    }

    public void editSnake() {
        System.out.println("Editing a snake");
    }

    public ExtendedVector2d getCentroid() {
        return centroid;
    }

    /**
     * Calculate centroid of Snake
     */
    public void calcCentroid() {
        centroid = new ExtendedVector2d(0, 0);
        Node v = this.head;
        double x, y, g;
        do {
            g = (v.getX() * v.getNext().getY()) - (v.getNext().getX() * v.getY());
            x = (v.getX() + v.getNext().getX()) * g;
            y = (v.getY() + v.getNext().getY()) * g;
            centroid.setX(centroid.getX() + x);
            centroid.setY(centroid.getY() + y);
            v = v.getNext();
        } while (!v.isHead());

        centroid.multiply(1d / (6 * this.calcArea()));
    }

    public void makeAntiClockwise() {
        // BOA_.log("Checking if clockwise...");
        double sum = 0;
        Node v = head;
        do {
            sum += (v.getNext().getX() - v.getX()) * (v.getNext().getY() + v.getY());
            v = v.getNext();
        } while (!v.isHead());
        if (sum > 0) {
            // BOA_.log("\tclockwise, reversed");
            this.reverseSnake();
        }
    }

    /**
     * Turn Snake back anti clockwise
     */
    public void reverseSnake() {
        Node tmp;
        Node v = head;
        do {
            tmp = v.getNext();
            v.setNext(v.getPrev());
            v.setPrev(tmp);
            v = v.getNext();
        } while (!v.isHead());
    }

}

/**
 * Represents a node in the snake - its basic component In fact this class
 * stands for bidirectional list containing Nodes. Every node has assigned 2D
 * position and several additional properties such as:
 * <ul>
 * <li>velocity of Node</li>
 * <li>total force of Node</li>
 * <li>normal vector</li>
 * </ul>
 * 
 * @author rtyson
 *
 */
class Node {
    private ExtendedVector2d point; // x,y co-ordinates of the node
    private ExtendedVector2d normal; // normals
    private ExtendedVector2d tan;
    private ExtendedVector2d vel; // velocity of the nodes
    private ExtendedVector2d F_total; // total force at node
    private ExtendedVector2d prelimPoint; // point to move node to after all new
                                          // node positions have been calc
    private boolean frozen; // flag which is set when the velocity is below the
                            // critical velocity
    private int tracknumber;
    double position = -1; // position value.
    private Node prev; // predecessor to current node
    private Node next; // successor to current node
    private boolean head;
    private static boolean clockwise = true; // access clockwise if true
    // public QColor colour;

    public Node(int t) {
        // t = tracking number
        point = new ExtendedVector2d();
        F_total = new ExtendedVector2d();
        vel = new ExtendedVector2d();
        normal = new ExtendedVector2d();
        tan = new ExtendedVector2d();
        prelimPoint = new ExtendedVector2d();
        frozen = false;
        head = false;
        tracknumber = t;
        // colour = QColor.lightColor();
    }

    Node(double xx, double yy, int t) {
        point = new ExtendedVector2d(xx, yy);
        F_total = new ExtendedVector2d();
        vel = new ExtendedVector2d();
        normal = new ExtendedVector2d();
        tan = new ExtendedVector2d();
        prelimPoint = new ExtendedVector2d();
        frozen = false;
        head = false;
        tracknumber = t;
        // colour = QColor.lightColor();
    }

    public double getX() {
        // get X space co-ordinate
        return point.getX();
    }

    public double getY() {
        // get X space co-ordinate
        return point.getY();
    }

    /**
     * Set \c X space co-ordinate
     * 
     * @param x coordinate
     */
    public void setX(double x) {
        point.setX(x);
    }

    /**
     * Set \c Y space co-ordinate
     * 
     * @param y coordinate
     */
    public void setY(double y) {
        point.setY(y);
    }

    /**
     * Update point and force with preliminary values, and reset.
     */
    public void update() {
        setX(getX() + prelimPoint.getX());
        setY(getY() + prelimPoint.getY());
        prelimPoint.setX(0);
        prelimPoint.setY(0);
    }

    /**
     * Get previous node in chain (next if not clockwise)
     * 
     * @return next or previous Node from list
     */
    public Node getPrev() {
        if (clockwise) {
            return prev;
        } else {
            return next;
        }
    }

    /**
     * Get next node in chain (previous if not clockwise)
     * 
     * @return previous or next Node from list
     */
    public Node getNext() {
        if (clockwise) {
            return next;
        } else {
            return prev;
        }
    }

    /**
     * Adds previous (or next if not clockwise) Node to list
     * 
     * @param n Node to add
     */
    public void setPrev(Node n) {
        if (clockwise) {
            prev = n;
        } else {
            next = n;
        }
    }

    /**
     * Adds next (or previous if not clockwise) Node to list
     * 
     * @param n Node to add
     */
    public void setNext(Node n) {
        if (clockwise) {
            next = n;
        } else {
            prev = n;
        }
    }

    public static void setClockwise(boolean b) {
        Node.clockwise = b;
    }

    public ExtendedVector2d getPoint() {
        return point;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public ExtendedVector2d getF_total() {
        return F_total;
    }

    public ExtendedVector2d getVel() {
        return vel;
    }

    public ExtendedVector2d getNormal() {
        return normal;
    }

    public ExtendedVector2d getTangent() {
        return tan;
    }

    public int getTrackNum() {
        return tracknumber;
    }

    /**
     * Sets total force for Node
     * 
     * @param f vector of force to assign to Node force
     */
    public void setF_total(ExtendedVector2d f) {
        F_total.setX(f.getX());
        F_total.setY(f.getY());
    }

    /**
     * Sets velocity for Node
     * 
     * @param v vector of velocity to assign to Node force
     */
    public void setVel(ExtendedVector2d v) {
        vel.setX(v.getX());
        vel.setY(v.getY());
    }

    /**
     * Updates total force for Node
     * 
     * @param f vector of force to add to Node force
     */
    public void addF_total(ExtendedVector2d f) {
        // add the xy values in f to xy F_total i.e updates total Force
        F_total.setX(F_total.getX() + f.getX());
        F_total.setY(F_total.getY() + f.getY());
    }

    /**
     * Updates velocity for Node
     * 
     * @param v vector of velocity to add to Node force
     */
    public void addVel(ExtendedVector2d v) {
        // adds the xy values in v to Vel i.e. updates velocity
        vel.setX(vel.getX() + v.getX());
        vel.setY(vel.getY() + v.getY());
    }

    public void setPrelim(ExtendedVector2d v) {
        prelimPoint.setX(v.getX());
        prelimPoint.setY(v.getY());
    }

    public void freeze() {
        frozen = true;
    }

    public void unfreeze() {
        frozen = false;
    }

    public boolean isHead() {
        return head;
    }

    public void setHead(boolean t) {
        head = t;
    }

    /**
     * Updates the normal (must point inwards)
     */
    public void updateNormale() {
        boolean c = clockwise;
        clockwise = true; // just in case
        tan = calcTan(); // tangent

        /*
         * // calc local orientation matrix double xa, ya, xb, yb, xc, yc; xa = prev.getX(); ya =
         * prev.getY(); xb = getX(); yb = getY(); xc = next.getX(); yc = next.getY();
         * 
         * double localO = (xb*yc + xa*yb + ya*xc) - (ya*xb + yb*xc + xa*yc); //determinant of
         * orientation if(localO==0){ IJ.log( "orientation is flat!"); normal.setX(-tan.getY());
         * normal.setY(tan.getX()); }else if(localO*detO > 0){ normal.setX(-tan.getY());
         * normal.setY(tan.getX()); }else{ normal.setX(tan.getY()); normal.setY(-tan.getX()); }
         * 
         * if (p.expandSnake) { // switch around if expanding snake normal.setX(-normal.getY());
         * normal.setY(-normal.getX()); }
         */

        if (!BOA_.boap.segParam.expandSnake) { // switch around if expanding snake
            normal.setX(-tan.getY());
            normal.setY(tan.getX());
        } else {
            normal.setX(tan.getY());
            normal.setY(-tan.getX());
        }
        clockwise = c;

    }

    /**
     * Calculate tangent at Node n (i.e. unit vector between neighbors)
     * 
     * Calculate a unit vector towards neighboring nodes and then a unit vector
     * between their ends. direction important for normale calculation. Always
     * calculate tan as if clockwise
     * 
     * @return Tangent at node
     */
    private ExtendedVector2d calcTan() {

        ExtendedVector2d unitVecLeft = ExtendedVector2d.unitVector(point, prev.getPoint());
        ExtendedVector2d unitVecRight = ExtendedVector2d.unitVector(point, next.getPoint());

        ExtendedVector2d pointLeft = new ExtendedVector2d();
        pointLeft.setX(getX());
        pointLeft.setY(getY());
        pointLeft.addVec(unitVecLeft);

        ExtendedVector2d pointRight = new ExtendedVector2d();
        pointRight.setX(getX());
        pointRight.setY(getY());
        pointRight.addVec(unitVecRight);

        tan = ExtendedVector2d.unitVector(pointLeft, pointRight);

        return tan;
    }

    public static void randDirection() {
        if (Math.random() < 0.5) {
            clockwise = true;
        } else {
            clockwise = false;
        }
    }

    public double getCurvatureLocal() {

        ExtendedVector2d edge1 =
                ExtendedVector2d.vecP2P(this.getPoint(), this.getPrev().getPoint());
        ExtendedVector2d edge2 =
                ExtendedVector2d.vecP2P(this.getPoint(), this.getNext().getPoint());

        double angle = ExtendedVector2d.angle(edge1, edge2) * (180 / Math.PI);

        if (angle > 360 || angle < -360) {
            System.out.println("Warning-angle out of range (Vert l:320)");
        }

        if (angle < 0)
            angle = 360 + angle;

        double curvatureLocal = 0;
        if (angle == 180) {
            curvatureLocal = 0;
        } else if (angle < 180) {
            curvatureLocal = -1 * (1 - (angle / 180));
        } else {
            curvatureLocal = (angle - 180) / 180;
        }
        return curvatureLocal;
    }

}

/**
 * Holds parameters defining snake and controlling contour matching algorithm.
 * BOAp is static class contains internal as well as external parameters used to
 * define snake and to control contour matching algorithm. There are also
 * several basic get/set methods for accessing selected parameters, setting
 * default {@link uk.ac.warwick.wsbc.QuimP.BOAp.SegParam.setDefaults() values} 
 * and writing/reading these (external) parameters to/from disk. File format used for
 * storing data in files is defined at {@link QParams} class.
 * 
 * External parameters are those related to algorithm options whereas internal
 * are those related to internal settings of algorithm, GUI and whole plugin
 * 
 * @author rtyson
 * @see QParams
 * @see Tool
 * @warning This class will be redesigned in future 
 */
class BOAp {

    File orgFile, outFile; /*!< paramFile; */
    String fileName; /*!< file name only, no extension */
    QParams readQp; /*!< read in parameter file */
    public SegParam segParam; /*!< Parameters of segmentation available for user (GUI)*/
    // internal parameters
    int NMAX; /*!< maximum number of nodes (% of starting nodes) */
    double delta_t;
    double sensitivity;
    double f_friction;
    int FRAMES; /*!< Number of frames in stack */
    int WIDTH, HEIGHT;
    int cut_every; /*!< cut loops in chain every X frames */
    boolean oldFormat; /*!< output old QuimP format? */
    boolean saveSnake; /*!< save snake data */
    private double min_dist; /*!< min distance between nodes */
    private double max_dist; /*!< max distance between nodes */
    double proximity; /*!< distance between centroids at which contact is tested for */
    double proxFreeze; /*!< proximity of nodes to freeze when blowing up */
    boolean savedOne;
    double imageScale; /*!< scale of image in */
    double imageFrameInterval;
    boolean scaleAdjusted;
    boolean fIAdjusted;
    boolean singleImage;
    String paramsExist; // on startup check if defaults are needed to set
    boolean zoom;
    boolean doDelete;
    boolean doDeleteSeg;
    boolean editMode; /*!< is select a cell for editing active? */
    int editingID; // currently editing cell iD. -1 if not editing
    boolean useSubPixel = true;
    boolean supressStateChangeBOArun = false;
    int callCount; // use to test how many times a method is called
    boolean SEGrunning; /*!< is seg running */

    /**
     * Hold user parameters of segmentation
     * 
     * @author p.baniukiewicz
     * @date 30 Mar 2016
     * @see BOAState
     */
    class SegParam {
        private double nodeRes; /*!< Number of nodes on ROI edge */
        int blowup; /*!< distance to blow up chain */
        double vel_crit;
        double f_central;
        double f_image; /*!< image force */
        int max_iterations; /*!< max iterations per contraction */
        int sample_tan;
        int sample_norm;
        double f_contract;
        double finalShrink;
        // Switch Params
        boolean use_previous_snake;/*!< next contraction begins with prev chain */
        boolean showPaths;
        boolean expandSnake; /*!< whether to act as an expanding snake */

        /**
         * Sets default values of parameters
         */
        public SegParam() {
            setDefaults();
        }

        /**
         * Return nodeRes
         * 
         * @return nodeRes field
         */
        public double getNodeRes() {
            return nodeRes;
        }

        /**
         * Set \c nodeRes field and calculate \c min_dist and \c max_dist
         * 
         * @param d
         */
        public void setNodeRes(double d) {
            nodeRes = d;
            if (nodeRes < 1) {
                min_dist = 1; // min distance between nodes
                max_dist = 2.3; // max distance between nodes
                return;
            }
            min_dist = nodeRes; // min distance between nodes
            max_dist = nodeRes * 1.9; // max distance between nodes
        }

        /**
         * Set default parameters for contour matching algorithm.
         * 
         * These parameters are external - available for user to set in GUI.
         */
        public void setDefaults() {
            setNodeRes(6.0);
            blowup = 20; // distance to blow up chain
            vel_crit = 0.005;
            f_central = 0.04;
            f_image = 0.2; // image force
            max_iterations = 4000; // max iterations per contraction
            sample_tan = 4;
            sample_norm = 12;
            f_contract = 0.04;
            finalShrink = 3d;
        }
    } // end of SegParam

    /**
     * Default constructor
     */
    public BOAp() {
        segParam = new SegParam(); // build segmentation parameters object wit default values
    }

    /**
     * Plot or not snakes after processing by plugins. If \c yes both snakes, after 
     * segmentation and after filtering are plotted.
     */
    boolean isProcessedSnakePlotted = true;

    /**
     * When any plugin fails this field defines how QuimP should behave. When
     * it is \c true QuimP breaks process of segmentation and do not store
     * filtered snake in SnakeHandler
     * @warning Currently not used
     * @todo TODO Implement this feature
     * @see http://www.trac-wsbc.linkpc.net:8080/trac/QuimP/ticket/81
     */
    boolean stopOnPluginError = true;

    public double getMax_dist() {
        return max_dist;
    }

    public double getMin_dist() {
        return min_dist;
    }

    /**
     * Initialize internal parameters of BOA plugin
     * 
     * Most of these parameters are related to state machine of BOA. There are
     * also parameters related to internal state of Active Contour algorithm.
     * Defaults for parameters available for user are set in
     * {@link uk.ac.warwick.wsbc.QuimP.BOAp.SegParam.setDefaults()}
     * 
     * @param ip Reference to segmented image passed from IJ
     * @see setDefaults()
     */
    public void setup(final ImagePlus ip) {
        FileInfo fileinfo = ip.getOriginalFileInfo();
        if (fileinfo == null) {
            // System.out.println("1671-No file Info, use " +
            // orgIpl.getTitle());
            orgFile = new File(File.separator, ip.getTitle());
        } else {
            // System.out.println("1671-file Info, filename: " +
            // fileinfo.fileName);
            orgFile = new File(fileinfo.directory, fileinfo.fileName);
        }
        fileName = Tool.removeExtension(orgFile.getName());

        FRAMES = ip.getStackSize(); // get number of frames
        imageFrameInterval = ip.getCalibration().frameInterval;
        imageScale = ip.getCalibration().pixelWidth;
        scaleAdjusted = false;
        fIAdjusted = false;
        if (imageFrameInterval == 0) {
            imageFrameInterval = 1;
            fIAdjusted = true;
            // BOA_.log("Warning. Frame interval was 0 sec. Using 1 sec instead"
            // + "\n\t[set in 'image->Properties...']");
        }
        if (imageScale == 0) {
            imageScale = 1;
            scaleAdjusted = true;
            // BOA_.log("Warning. Scale was 1 pixel == 0 \u00B5m. Using 1
            // \u00B5m instead"
            // + "\n\t(set in 'Analyze->Set Scale...')");
        }

        savedOne = false;
        segParam.showPaths = false;
        segParam.use_previous_snake = true; // next contraction begins with last chain
        segParam.expandSnake = false; // set true to act as an expanding snake
        // nestSize = 0;
        WIDTH = ip.getWidth();
        HEIGHT = ip.getHeight();
        paramsExist = "YES";

        // internal parameters
        NMAX = 250; // maximum number of nodes (% of starting nodes)
        delta_t = 1.;
        sensitivity = 0.5;
        cut_every = 8; // cut loops in chain every X interations
        oldFormat = false; // output old QuimP format?
        saveSnake = true; // save snake data
        proximity = 150; // distance between centroids at
                         // which contact is tested for
        proxFreeze = 1; // proximity of nodes to freeze when blowing up
        f_friction = 0.6;
        doDelete = false;
        doDeleteSeg = false;
        zoom = false;
        editMode = false;
        editingID = -1;
        callCount = 0;
        SEGrunning = false;

    }

    /**
     * Write set of snake parameters to disk.
     * 
     * writeParams method creates \a paQP master file, referencing other
     * associated files and \a csv file with statistics.
     * 
     * @param sID ID of cell. If many cells segmented in one time, QuimP
     * produces separate parameter file for every of them
     * @param startF Start frame (typically beginning of stack)
     * @param endF End frame (typically end of stack)
     * @see QParams
     */
    public void writeParams(int sID, int startF, int endF) {
        if (saveSnake) {
            File paramFile = new File(outFile.getParent(), fileName + "_" + sID + ".paQP");
            File statsFile = new File(
                    outFile.getParent() + File.separator + fileName + "_" + sID + ".stQP.csv");

            QParams qp = new QParams(paramFile);
            qp.segImageFile = orgFile;
            qp.snakeQP = outFile;
            qp.statsQP = statsFile;
            qp.imageScale = imageScale;
            qp.frameInterval = imageFrameInterval;
            qp.startFrame = startF;
            qp.endFrame = endF;
            qp.NMAX = NMAX;
            qp.blowup = segParam.blowup;
            qp.max_iterations = segParam.max_iterations;
            qp.sample_tan = segParam.sample_tan;
            qp.sample_norm = segParam.sample_norm;
            qp.delta_t = delta_t;
            qp.nodeRes = segParam.nodeRes;
            qp.vel_crit = segParam.vel_crit;
            qp.f_central = segParam.f_central;
            qp.f_contract = segParam.f_contract;
            qp.f_image = segParam.f_image;
            qp.f_friction = f_friction;
            qp.finalShrink = segParam.finalShrink;
            qp.sensitivity = sensitivity;

            qp.writeParams();
        }
    }

    /**
     * Read set of snake parameters from disk.
     * 
     * readParams method reads \a paQP master file, referencing other associated
     * files.
     * 
     * @return Status of operation
     * @retval true when file has been loaded successfully
     * @retval false when file has not been opened correctly or
     * QParams.readParams() returned \c false
     * @see QParams
     */
    public boolean readParams() {
        OpenDialog od = new OpenDialog("Open paramater file (.paQP)...", "");
        if (od.getFileName() == null) {
            return false;
        }
        readQp = new QParams(new File(od.getDirectory(), od.getFileName()));

        if (!readQp.readParams()) {
            BOA_.log("Failed to read parameter file");
            return false;
        }
        NMAX = readQp.NMAX;
        segParam.blowup = readQp.blowup;
        segParam.max_iterations = readQp.max_iterations;
        segParam.sample_tan = readQp.sample_tan;
        segParam.sample_norm = readQp.sample_norm;
        delta_t = readQp.delta_t;
        segParam.nodeRes = readQp.nodeRes;
        segParam.vel_crit = readQp.vel_crit;
        segParam.f_central = readQp.f_central;
        segParam.f_contract = readQp.f_contract;
        segParam.f_image = readQp.f_image;

        if (readQp.newFormat) {
            segParam.finalShrink = readQp.finalShrink;
        }
        BOA_.log("Successfully read parameters");
        return true;
    }
}

/**
 * Extended exception class.
 * 
 * Contains additional information on frame and type
 * 
 * @author rtyson
 *
 */
class BoaException extends Exception {

    private static final long serialVersionUID = 1L;
    private int frame;
    private int type;

    public BoaException(String msg, int f, int t) {
        super(msg);
        frame = f;
        type = t;
    }

    public int getFrame() {
        return frame;
    }

    public int getType() {
        return type;
    }
}