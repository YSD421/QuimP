package com.github.celldynamics.quimp.utils.graphics.vrml;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

/**
 *
 * @author rtyson
 */
public class VRMLobject {

  private Point3f[] coords;
  private Color3f[] colorsF;
  private int[] coordIndices;

  private boolean ccw = true;
  private boolean colorPerVertex = true;
  private boolean normalPerVertex = true;
  private boolean convex = true;
  private float creaseAngle = 2.0f;
  private boolean solid = true;

  /**
   * Instantiates a new VRM lobject.
   *
   * @param p the p
   * @param c the c
   * @param i the i
   */
  public VRMLobject(Point3f[] p, Color3f[] c, int[] i) {
    coords = p;
    colorsF = c;
    coordIndices = i;
  }

  /**
   * Transform.
   *
   * @param x the x
   * @param y the y
   * @param z the z
   */
  public void transform(float x, float y, float z) {
    Point3f tpf = new Point3f(x, y, z);

    for (int i = 0; i < coords.length; i++) {
      coords[i].add(tpf);
    }
  }

  /**
   * Scale.
   *
   * @param x the x
   */
  public void scale(float x) {
    for (int i = 0; i < coords.length; i++) {
      coords[i].scale(x);
    }
  }

  /**
   * Write.
   *
   * @param out the out
   */
  public void write(File out) {
    try {
      PrintWriter pw = new PrintWriter(new FileWriter(out), true);

      pw.print("#VRML V2.0 utf8\nShape {\n\tgeometry IndexedFaceSet {");
      pw.print("\n\t\tccw ");
      writeBoolean(pw, ccw);
      pw.print("\n\t\tcolorPerVertex ");
      writeBoolean(pw, colorPerVertex);
      pw.print("\n\t\tnormalPerVertex ");
      writeBoolean(pw, normalPerVertex);
      pw.print("\n\t\tconvex ");
      writeBoolean(pw, convex);
      pw.print("\n\t\tcreaseAngle " + creaseAngle);
      pw.print("\n\t\tsolid ");
      writeBoolean(pw, solid);

      pw.print("\n\t\tcoord Coordinate {\n\t\t\tpoint [");
      // write coords
      for (int i = 0; i < coords.length; i++) {
        pw.print("\n\t\t\t\t" + coords[i].x + " " + coords[i].y + " " + coords[i].z);
        if (i != coords.length - 1) {
          pw.print(",");
          // break;
        }
      }
      pw.print("\n\t\t\t]\n\t\t}");

      pw.print("\n\t\tcolor Color {\n\t\t\tcolor [");
      // write colors
      for (int i = 0; i < colorsF.length; i++) {
        pw.print("\n\t\t\t\t" + colorsF[i].x + " " + colorsF[i].y + " " + colorsF[i].z);
        if (i != colorsF.length - 1) {
          pw.print(",");
          // break;
        }
      }
      pw.print("\n\t\t\t]\n\t\t}");

      pw.print("\n\t\tcoordIndex [");
      // write coordIndices
      for (int i = 0; i < coordIndices.length; i += 4) {
        pw.print("\n\t\t\t" + coordIndices[i] + ", " + coordIndices[i + 1] + ", "
                + coordIndices[i + 2] + ", " + coordIndices[i + 3] + ", -1");
        if (i != coordIndices.length - 4) {
          pw.print(",");
          // break;
        }
      }
      pw.print("\n\t\t]");

      pw.print("\n\t}\n}");

      pw.close();

    } catch (Exception e) {
      System.out.println("Could not write VMRL object to " + out.getName());
    }
  }

  private void writeBoolean(PrintWriter pw, boolean b) {
    if (b) {
      pw.print("TRUE");
    } else {
      pw.print("FALSE");
    }
  }
}
