/**
 */
package uk.ac.warwick.wsbc.quimp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.warwick.wsbc.quimp.QColor;

// TODO: Auto-generated Javadoc
/**
 * Test of QColor class
 * 
 * Test if it is necessary to override equals() method. It is because by default equals only compare
 * objects references.
 * 
 * @author p.baniukiewicz
 *
 */
public class QColorTest {

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
   * Test of equals()
   * 
   * Post: Both objects are the same
   */
  @Test
  public void qColorTest() {
    QColor c = new QColor(0.1, 0.2, 0.3);
    QColor c1 = new QColor(0.1, 0.2, 0.3);
    assertThat(c, is((c1)));
  }

}