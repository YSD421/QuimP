package uk.ac.warwick.wsbc.quimp.utils.graphics.svg;

import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

// TODO: Auto-generated Javadoc
/**
 * @author p.baniukiewicz
 *
 */
public class SVGwritterTest {

  /**
   * The tmpdir.
   */
  static String tmpdir = System.getProperty("java.io.tmpdir") + File.separator;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for
   * {@link SVGwritter#writeHeader(OutputStreamWriter, Rectangle)}.
   * 
   * @throws Exception
   */
  @Test
  public void testWriteHeader() throws Exception {
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpdir + "t.svg"));
    OutputStreamWriter osw = new OutputStreamWriter(out);
    SVGwritter.writeHeader(osw, new Rectangle(-10, -10, 10, 10));
    SVGwritter.QPolarAxes qc = new SVGwritter.QPolarAxes(new Rectangle(-10, -10, 10, 10));
    qc.draw(osw);
    osw.write("</svg>\n");
    osw.close();
  }

}
