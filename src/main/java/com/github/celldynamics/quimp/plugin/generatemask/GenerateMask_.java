package com.github.celldynamics.quimp.plugin.generatemask;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JOptionPane;

import com.github.celldynamics.quimp.BOAState;
import com.github.celldynamics.quimp.Nest;
import com.github.celldynamics.quimp.QParamsQconf;
import com.github.celldynamics.quimp.QuimP;
import com.github.celldynamics.quimp.QuimpException;
import com.github.celldynamics.quimp.QuimpException.MessageSinkTypes;
import com.github.celldynamics.quimp.Snake;
import com.github.celldynamics.quimp.SnakeHandler;
import com.github.celldynamics.quimp.filesystem.FileExtensions;
import com.github.celldynamics.quimp.plugin.AbstractPluginQconf;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;
import com.github.celldynamics.quimp.utils.QuimpToolsCollection;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

/**
 * Convert QCONF files to BW masks.
 * 
 * <p>Use Snake data produced by BOA and stored in QCONF file. If run from macro it shows image and
 * save it to folder where QCONF resides. If run as API it does not do any of these two actions, use
 * {@link #getRes()} to obtain image.
 * 
 * @author p.baniukiewicz
 *
 */
public class GenerateMask_ extends AbstractPluginQconf {

  private static String thisPluginName = "Generate mask";

  /**
   * Resulting image. (Not saved or shown if apiCall==true)
   */
  private ImagePlus res;

  /**
   * Executed if plugin is run from IJ. Set apiCall to false and redirect exception to IJ.
   */
  public GenerateMask_() {
    super(new GenerateMaskOptions(), thisPluginName);
  }

  /**
   * Constructor that allows to provide own parameters.
   * 
   * @param paramString it can be null to ask user for file or it can be parameters string like that
   *        passed in macro.
   * @throws QuimpPluginException on any error
   */
  public GenerateMask_(String paramString) throws QuimpPluginException {
    super(paramString, new GenerateMaskOptions(), thisPluginName); // will parse and fill options
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.PluginTemplate#run(java.lang.String)
   */
  @Override
  public void run(String arg) {
    super.run(arg);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpPlugin#about()
   */
  @Override
  public String about() {
    return "Generate mask plugin.\n" + "Author: Piotr Baniukiewicz\n"
            + "mail: p.baniukiewicz@warwick.ac.uk";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.PluginTemplate#runFromQCONF()
   */
  @Override
  protected void runFromQconf() throws QuimpException {
    GenerateMaskOptions opts = (GenerateMaskOptions) options;
    BOAState bs = qconfLoader.getBOA();
    Nest nest = bs.nest;
    int currentColor;
    // create output image
    res = NewImage.createShortImage("test", bs.boap.getWidth(), bs.boap.getHeight(),
            bs.boap.getFrames(), NewImage.FILL_BLACK);
    // get stacks reference
    ImageStack contourStack = res.getStack();
    res.setSlice(1); // set for first
    int frame; // frames counter (from 1)
    Snake snake;
    ImageProcessor contourIp; // processor taken from stack (ref)
    for (frame = 1; frame <= bs.boap.getFrames(); frame++) { // iterate over frames
      List<Integer> snakes = nest.getSnakesforFrame(frame); // find all SnakeHandlers on frame
      contourIp = contourStack.getProcessor(frame); // get processor from stack for frame
      currentColor = 1;
      for (Integer snakeID : snakes) { // iterate over SnakeHandlers
        SnakeHandler snakeH = nest.getHandlerofId(snakeID); // get SH of snakeID
        if (snakeH != null) {
          snake = snakeH.getStoredSnake(frame); // get snake from this handler and current frame
          Roi roi = snake.asFloatRoi(); // convert to ROI
          contourIp.setColor(currentColor++);
          contourIp.fill(roi); // plot on current slice
        }
      }
    }
    if (opts.binary) {
      for (int i = 1; i <= res.getImageStackSize(); i++) {
        res.getStack().getProcessor(i).threshold(0);
      }
      ImageConverter.setDoScaling(false);
      new StackConverter(res).convertToGray8();
    }
    if (!apiCall) { // do not show nor save if called from api
      res.show();
      // save in QCONF folder
      QParamsQconf qp = (QParamsQconf) qconfLoader.getQp();
      Path filename = Paths.get(qp.getPath(), qp.getFileName() + FileExtensions.generateMaskSuffix);
      IJ.saveAsTiff(res, filename.toString());
      IJ.log("Saved in: " + filename.toString());
      if (errorSink == MessageSinkTypes.GUI) { // run form IJ not from macro
        JOptionPane.showMessageDialog(
                IJ.getInstance(), QuimpToolsCollection
                        .stringWrap("Image saved! (" + filename.toString() + ")", QuimP.LINE_WRAP),
                "Saved!", JOptionPane.INFORMATION_MESSAGE);
      } else {
        IJ.log("Mask generated!");
      }
    }

  }

  /**
   * Return image generated by plugin. Use with API calls.
   * 
   * <p>Return 16bit image if {@link GenerateMaskOptions#binary} is set to false with grayscale
   * indexed objects.
   * 
   * @return the res Mask image generated from loaded Qconf.
   */
  public ImagePlus getRes() {
    return res;
  }

  @Override
  protected void runFromPaqp() throws QuimpException {
    throw new QuimpException("Old file format is not supported");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.PluginTemplate#showUi(boolean)
   */
  @Override
  public void showUi(boolean val) throws Exception {
    // this method is called when no options were provided to run, paramFile is empty or null
    loadFile(options.paramFile); // if no options (run from menu) let qconfloader show file selector
    // fill this for macro recorder
    options.paramFile = qconfLoader.getQp().getParamFile().getAbsolutePath();
  }
}
