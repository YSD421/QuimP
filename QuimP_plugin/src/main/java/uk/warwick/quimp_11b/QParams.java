/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.warwick.quimp_11b;

import ij.IJ;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

/**
 * Container class for parameters defining the whole process of analysis in QuimP. 
 * Stores also BOA parameters and supports writing and reading segmentation 
 * parameters from files (paQP).
 * This class defines file format used for storing parameters in file. Process 
 * only main paQP file. 
 * QuimP uses several files to store segmentation results and algorithm 
 * parameters:
 * <ul>
 * <li> .paQP - core file, contains reference to images and parameters of algorithm. This file is saved and processed by QParams class </li>
 * <li> .snQP - contains positions of all nodes for every frame </li>
 * <li> .stQP - basic shape statistics for every frame </li>
 * </ul>
 * <p>
 * These files are generated for every one segmented object.
 * @author rtyson
 * @see BOAp
 */
public class QParams {

   private File paramFile;
   private File[] otherPaFiles;
   boolean newFormat;

   String prefix;
   String path;
   File segImageFile, snakeQP, statsQP;
   File[] fluTiffs;

   File convexFile, coordFile, motilityFile, originFile, xFile, yFile;
   File[] fluFiles;

   double imageScale;
   double frameInterval;
   int startFrame, endFrame;
   int NMAX, blowup, max_iterations, sample_tan, sample_norm;
   double delta_t, nodeRes, vel_crit, f_central, f_contract, f_image, f_friction;
   double finalShrink, cortexWidth;
   long key;
   double sensitivity; // no longer used. blank holder
   boolean ecmmHasRun = false;

   QParams(File p) {
      paramFile = p;
      path = paramFile.getParent();
      prefix = Tool.removeExtension( paramFile.getName());
      
      newFormat = true;

      segImageFile = new File("/");
      snakeQP = new File("/");
      statsQP = new File("/");

      fluTiffs = new File[3];
      fluTiffs[0] = new File("/");
      fluTiffs[1] = new File("/");
      fluTiffs[2] = new File("/");

      imageScale = -1;
      frameInterval = -1;
      startFrame = -1;
      endFrame = -1;
      NMAX = -1;
      blowup = -1;
      max_iterations = -1;
      sample_tan = -1;
      sample_norm = -1;
      delta_t = -1;
      nodeRes = -1;
      vel_crit = -1;
      f_central = -1;
      f_contract = -1;
      f_image = -1;
      f_friction = -1;
      finalShrink = -1;
      cortexWidth = 0.7;
      key = -1;
      sensitivity = -1;
   }

   void setParamFile(File p) {
      paramFile = p;
      path = paramFile.getParent();
      prefix = Tool.removeExtension( paramFile.getName());
   }

   boolean readParams() {
      // reads the paQP file specified by paramFile. Returns true if successful.
      newFormat = false;
      try {
         BufferedReader d = new BufferedReader(new FileReader(paramFile));

         String l = d.readLine();
         if (!(l.length() < 2)) {
            String fileID = l.substring(0, 2);
            if (!fileID.equals("#p")) {
               IJ.error("Not a compatible paramater file");
               d.close();
               return false;
            }
         } else {
            IJ.error("Not a compatible paramater file");
            d.close();
            return false;
         }
         key = (long) Tool.s2d(d.readLine()); // key
         segImageFile = new File(d.readLine()); // image file name

         String sn = d.readLine();
         //fileName = sn;
         if (!l.substring(0, 3).equals("#p2")) { // old format, fix file names
            sn = sn.substring(1); // strip the dot off snQP file name
            //fileName = fileName.substring(1); // strip the dot off file name
            int lastDot = sn.lastIndexOf(".");

            String tempS = sn.substring(0, lastDot+1);
            //System.out.println("tempS: " + tempS+", ld = " + lastDot);
            statsQP = new File(paramFile.getParent() + tempS + "stQP.csv");
            //System.out.println("stats file: " + statsQP.getAbsolutePath());
         }
         snakeQP = new File(paramFile.getParent() + "" + sn); // snQP file
         System.out.println("snake file: " + snakeQP.getAbsolutePath());

         d.readLine(); // # blank line
         imageScale = Tool.s2d(d.readLine());
         if(imageScale==0){
            IJ.log("Warning. Image scale was zero. Set to 1");
            imageScale = 1;
         }
         frameInterval = Tool.s2d(d.readLine());

         d.readLine(); //skip #segmentation parameters
         NMAX = (int) Tool.s2d(d.readLine());
         delta_t = Tool.s2d(d.readLine());
         max_iterations = (int) Tool.s2d(d.readLine());
         nodeRes = Tool.s2d(d.readLine());
         blowup = (int) Tool.s2d(d.readLine());
         sample_tan = (int) Tool.s2d(d.readLine());
         sample_norm = (int) Tool.s2d(d.readLine());
         vel_crit = Tool.s2d(d.readLine());

         f_central = Tool.s2d(d.readLine());
         f_contract = Tool.s2d(d.readLine());
         f_friction = Tool.s2d(d.readLine());
         f_image = Tool.s2d(d.readLine());
         sensitivity = Tool.s2d(d.readLine());

         if (l.substring(0, 3).equals("#p2")) { // new format
            newFormat = true;
            // new params
            d.readLine(); // # - new parameters (cortext width, start frame, end frame, final shrink, statsQP, fluImage)
            cortexWidth = Tool.s2d(d.readLine());
            startFrame = (int) Tool.s2d(d.readLine());
            endFrame = (int) Tool.s2d(d.readLine());
            finalShrink = Tool.s2d(d.readLine());
            statsQP = new File(paramFile.getParent() + "" + d.readLine());

            d.readLine(); // # fluo channel tiffs
            fluTiffs[0] = new File(d.readLine());
            fluTiffs[1] = new File(d.readLine());
            fluTiffs[2] = new File(d.readLine());
         }
         d.close();
         this.guessOtherFileNames();
         checkECMMrun();
         return true;

      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   void writeParams() {
      newFormat = true;

      try {
         if (paramFile.exists()) {
            paramFile.delete();
         }

         Random generator = new Random();
         double d = generator.nextDouble() * 1000000;    // 6 digit key to ID job
         key = Math.round(d);

         PrintWriter pPW = new PrintWriter(new FileWriter(paramFile), true); //auto flush

         pPW.print("#p2 - QuimP parameter file (QuimP11). Created " +Tool.dateAsString() +"\n");
         pPW.print(IJ.d2s(key, 0) + "\n");
         pPW.print(segImageFile.getAbsolutePath() + "\n");
         pPW.print(File.separator + snakeQP.getName() + "\n");
         //pPW.print(outFile.getAbsolutePath() + "\n");

         pPW.print("#Image calibration (scale, frame interval)\n");
         pPW.print(IJ.d2s(imageScale, 6) + "\n");
         pPW.print(IJ.d2s(frameInterval, 3) + "\n");

         pPW.print("#segmentation parameters\n");
         pPW.print(IJ.d2s(NMAX, 0) + "\n");
         pPW.print(IJ.d2s(delta_t, 6) + "\n");
         pPW.print(IJ.d2s(max_iterations, 6) + "\n");
         pPW.print(IJ.d2s(nodeRes, 6) + "\n");
         pPW.print(IJ.d2s(blowup, 6) + "\n");
         pPW.print(IJ.d2s(sample_tan, 0) + "\n");
         pPW.print(IJ.d2s(sample_norm, 0) + "\n");
         pPW.print(IJ.d2s(vel_crit, 6) + "\n");
         pPW.print(IJ.d2s(f_central, 6) + "\n");
         pPW.print(IJ.d2s(f_contract, 6) + "\n");
         pPW.print(IJ.d2s(f_friction, 6) + "\n");
         pPW.print(IJ.d2s(f_image, 6) + "\n");
         pPW.print(IJ.d2s(sensitivity, 6) + "\n");

         pPW.print("# - new parameters (cortext width, start frame, end frame, final shrink, statsQP, fluImage)\n");
         pPW.print(IJ.d2s(cortexWidth, 2) + "\n");
         pPW.print(IJ.d2s(startFrame, 0) + "\n");
         pPW.print(IJ.d2s(endFrame, 0) + "\n");
         pPW.print(IJ.d2s(finalShrink, 2) + "\n");
         pPW.print(File.separator + statsQP.getName() + "\n");

         pPW.print("# - Fluorescence channel tiff's\n");
         pPW.print(fluTiffs[0].getAbsolutePath() + "\n");
         pPW.print(fluTiffs[1].getAbsolutePath() + "\n");
         pPW.print(fluTiffs[2].getAbsolutePath() + "\n");

         pPW.print("#END");

         pPW.close();
      } catch (Exception e) {
         IJ.error("Could not write parameter file! " + e.getMessage());
         e.printStackTrace();
      }
   }

   public File[] findParamFiles(){
      File directory = new File(paramFile.getParent());
      ArrayList<String> paFiles = new ArrayList<String>();

      if(directory.isDirectory()) {
         String filenames[] = directory.list();
         String extension;
         
         for(int i=0; i < filenames.length ;i++ ){
            if(filenames[i].matches(".") || filenames[i].matches("..") || filenames[i].matches(paramFile.getName())){
                    continue;
            }
            extension = Tool.getFileExtension(filenames[i]);
            if(extension.matches("paQP")){
               paFiles.add(filenames[i]);
               System.out.println("paFile: "+ filenames[i]);
            }
         }
      }
      if(paFiles.isEmpty()){
         otherPaFiles = new File[0];
         return otherPaFiles;
      }else{
         otherPaFiles = new File[paFiles.size()];
         for(int j = 0; j < otherPaFiles.length; j++){
            otherPaFiles[j] = new File(directory.getAbsolutePath() +File.separator+ (String)paFiles.get(j));
         }
         return otherPaFiles;
      }
   }

   File getParamFile(){
      return paramFile;
   }

   void guessOtherFileNames(){
      System.out.println("prefix: " + prefix);

      convexFile = new File( path + File.separator + prefix + "_convexityMap.maQP");
      
      coordFile = new File( path + File.separator + prefix + "_coordMap.maQP");
      motilityFile = new File( path + File.separator + prefix + "_motilityMap.maQP");
      originFile = new File( path + File.separator + prefix + "_originMap.maQP");
      xFile = new File( path + File.separator + prefix + "_xMap.maQP");
      yFile = new File( path + File.separator + prefix + "_yMap.maQP");

      fluFiles = new File[3];
      fluFiles[0] = new File( path + File.separator + prefix + "_fluoCH1.maQP");
      fluFiles[1] = new File( path + File.separator + prefix + "_fluoCH2.maQP");
      fluFiles[2] = new File( path + File.separator + prefix + "_fluoCH3.maQP");
       
   }

   void checkECMMrun(){
	   BufferedReader br = null;
	   try {
            br = new BufferedReader(new FileReader(snakeQP));
            String line = br.readLine();  // read first line

            String sub = line.substring(line.length()-4, line.length());
            System.out.println("sub string: "+ sub);
            if(sub.matches("ECMM")){
               System.out.println("ECMM has been run on this paFile data");
               ecmmHasRun = true;
            }else{
               ecmmHasRun = false;
            }
       }catch(Exception e){
    	   System.err.println("Error: " + e);
       } finally {
    	   if(br!=null)
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
       }
   }
}