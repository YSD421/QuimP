package com.github.celldynamics.quimp.plugin.dic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

// TODO: Auto-generated Javadoc
/**
 * @author p.baniukiewicz
 *
 */
public class DicLidReconstruction_Test {
  private DicLidReconstruction_ inst;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    inst = new DicLidReconstruction_();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.plugin.dic.DicLidReconstruction_#showUi(boolean)}.
   * 
   * @throws Exception
   */
  @Test
  @Ignore
  public void testShowDialog() throws Exception {
    new DicLidReconstruction_().showUi(true);
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.plugin.dic.DicLidReconstruction_#roundtofull(double)}.
   * 
   * @throws Exception
   */
  @Test
  public void testRoundtofull() throws Exception {
    String ret = inst.roundtofull(0);
    assertThat(ret, is("0"));

    ret = inst.roundtofull(90);
    assertThat(ret, is("90"));

    ret = inst.roundtofull(45);
    assertThat(ret, is("45"));

    ret = inst.roundtofull(135);
    assertThat(ret, is("135"));

    ret = inst.roundtofull(180);
    assertThat(ret, is("0"));

    ret = inst.roundtofull(225);
    assertThat(ret, is("45"));

    ret = inst.roundtofull(270);
    assertThat(ret, is("90"));

    ret = inst.roundtofull(20);
    assertThat(ret, is("0"));

    ret = inst.roundtofull(50);
    assertThat(ret, is("45"));

    ret = inst.roundtofull(80);
    assertThat(ret, is("90"));

    ret = inst.roundtofull(100);
    assertThat(ret, is("90"));

    ret = inst.roundtofull(125);
    assertThat(ret, is("135"));

    ret = inst.roundtofull(141);
    assertThat(ret, is("135"));

    ret = inst.roundtofull(160);
    assertThat(ret, is("0"));

  }

}
