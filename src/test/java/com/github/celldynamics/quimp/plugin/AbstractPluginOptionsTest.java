package com.github.celldynamics.quimp.plugin;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractPluginOptionsTest.
 * 
 * @author p.baniukiewicz
 *
 */
public class AbstractPluginOptionsTest {

  /**
   * The Constant logger.
   */
  public final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  /**
   * Test of serialise().
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerialise() throws Exception {
    Options opt = new Options();
    String json = opt.serialize();
    logger.debug("Ser: " + json);
    logger.debug(opt.paramFile);
    assertThat(opt.otherPath, is("space space")); // not modified in main object
    assertThat(opt.paramFile, is("path/to/file with spaces.qconf"));
    assertThat(json, containsString("(space space)")); // escaped in json
    assertThat(json, containsString("(path/to/file with spaces.qconf)"));
    assertThat(StringUtils.countMatches(json, "("), is(2));
    assertThat(StringUtils.countMatches(json, ")"), is(2));
  }

  /**
   * Test of serialise().
   * 
   * <p>Refer to bug https://github.com/CellDynamics/QuimP/issues/299, problem with Windows paths.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerialise4() throws Exception {
    Options4 opt = new Options4();
    String json = opt.serialize2Macro();
    logger.debug("Ser: " + json);
    logger.debug(opt.paramFile);
    assertThat(opt.otherPath, is("space space")); // not modified in main object
    assertThat(opt.paramFile, is("c:\\path\\to\\file with spaces.qconf"));
    assertThat(json, containsString("(space space)")); // escaped in json
    assertThat(json, containsString("(c:\\\\path\\\\to\\\\file with spaces.qconf)"));
    assertThat(StringUtils.countMatches(json, "("), is(2));
    assertThat(StringUtils.countMatches(json, ")"), is(2));
  }

  /**
   * Test of deserialise().
   * 
   * @throws Exception on error
   */
  @Test
  public void testDeserialise() throws Exception {
    String json = "{\"paramFile\":\"file/test.qconf\"}";
    Options opt;
    opt = Options.deserialize(json, new Options());
    assertThat(opt.paramFile, is("file/test.qconf"));

  }

  /**
   * Test of deserialise().
   * 
   * <p>Refer to bug https://github.com/CellDynamics/QuimP/issues/299, problem with Windows paths.
   * 
   * @throws Exception on error
   */
  @Test
  public void testDeserialise4() throws Exception {
    String json = "{paramFile:(c:\\\\path\\\\to\\\\file with spacessss.qconf)}";
    Options4 opt;
    opt = Options4.deserialize2Macro(json, new Options4());
    assertThat(opt.paramFile, is("c:\\path\\to\\file with spacessss.qconf"));

  }

  /**
   * Test of serialise deserialise compatibility.
   * 
   * <p>Refer to bug https://github.com/CellDynamics/QuimP/issues/299, problem with Windows paths.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerialiseDeserialise4() throws Exception {
    Options4 opt = new Options4();
    opt.paramFile = "c:\\path\\to\\file with spacesSS.qconf";
    String json = opt.serialize2Macro();
    Options4 retopt = Options4.deserialize2Macro(json, new Options4());
    assertThat(retopt.paramFile, is(opt.paramFile));
  }

  /**
   * Test of {@link AbstractPluginOptions#removeSpacesMacro(String)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testRemoveSpacesMacro() throws Exception {
    String test1 = "{param:test , param2 : test2, param3: (path to file),{ param5: (other path)} }";
    String ret = "{param:test,param2:test2,param3:(path to file),{param5:(other path)}}";
    assertThat(AbstractPluginOptions.removeSpacesMacro(test1), is(ret));
  }

  /**
   * Test of {@link AbstractPluginOptions#serialize2Macro()}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerialize2Macro() throws Exception {
    Options opt = new Options();
    String json = opt.serialize2Macro();
    logger.debug(json);
    String ret = "{param2:10,param3:3.14,otherPath:(space space),param4:{internal1:20},paramFile:"
            + "(path/to/file with spaces.qconf)}";
    assertThat(json, is(ret));
  }

  /**
   * Test of {@link AbstractPluginOptions#deserialize2Macro(String, AbstractPluginOptions)}.
   * 
   * @throws Exception on error
   */
  @Test
  public void testDeserialize2Macro() throws Exception {
    String ret = "{param2:10,param3:3.14,otherPath:(space space),param4:{internal1:20},paramFile:"
            + "(path/to/file with spaces.qconf)}";
    Options des = Options.deserialize2Macro(ret, new Options());
    assertThat(des.otherPath, is("space space")); // escaping chars removed
    assertThat(des.paramFile, is("path/to/file with spaces.qconf")); // escaping chars removed
  }

  /**
   * Test of {@link AbstractPluginOptions#deserialize2Macro(String, AbstractPluginOptions)}. Bad
   * parameter string.
   * 
   * <p>If ( missing - json is correct but spaces can be removed from escaped string. Exception in
   * other case.
   * 
   * @throws Exception on error
   */
  @Test(expected = QuimpPluginException.class)
  public void testDeserialize2Macro_1() throws Exception {
    // missing :
    String ret = "{param2:10, param3 3.14,otherPath:(space space),param4: {internal1:20},paramFile:"
            + "(path/to/file with spaces.qconf)}";
    Options des = Options.deserialize2Macro(ret, new Options());
    assertThat(des.otherPath, is("space space")); // escaping chars removed
    assertThat(des.paramFile, is("path/to/file with spaces.qconf"));
    assertThat(des.param4.internal1, is(20));// escaping chars removed
  }

  /**
   * Test of serialization->deserialization for non processing serialzers.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerDeser_1() throws Exception {
    Options opt = new Options();
    String js = opt.serialize(); // change to json
    Options ret = AbstractPluginOptions.deserialize(js, new Options()); // back to object
    assertThat(ret.param2, is(opt.param2));
    assertThat(ret.paramFile, is(opt.paramFile));
    assertThat(ret.param3, is(opt.param3));
    assertThat(ret.otherPath, is(opt.otherPath));
    assertThat(ret.param4.internal1, is(opt.param4.internal1));
  }

  /**
   * Test of serialization->deserialization for macro processing serialzers.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerDeser_2() throws Exception {
    Options opt = new Options();
    String js = opt.serialize2Macro(); // change to json
    assertThat(js, containsString("(space space)")); // escaped in json
    assertThat(js, containsString("(path/to/file with spaces.qconf)"));
    assertThat(StringUtils.countMatches(js, "("), is(2));
    assertThat(StringUtils.countMatches(js, ")"), is(2));
    Options ret = AbstractPluginOptions.deserialize2Macro(js, new Options()); // back to object
    assertThat(ret.param2, is(opt.param2));
    assertThat(ret.paramFile, is(opt.paramFile));
    assertThat(ret.param3, is(opt.param3));
    assertThat(ret.otherPath, is(opt.otherPath)); // no escape chars
    assertThat(ret.param4.internal1, is(opt.param4.internal1));
  }

  /**
   * Test of serialization->deserialization for macro processing serialzers.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerDeser_arrays() throws Exception {
    Options2 opt = new Options2();
    String js = opt.serialize2Macro(); // change to json
    assertThat(js, containsString("(space space)")); // escaped in json
    assertThat(js, containsString("(path/to/file with spaces.qconf)"));
    assertThat(StringUtils.countMatches(js, "("), is(2));
    assertThat(StringUtils.countMatches(js, ")"), is(2));
    Options2 ret = AbstractPluginOptions.deserialize2Macro(js, new Options2()); // back to object
    assertThat(ret.param2, is(opt.param2));
    assertThat(ret.paramFile, is(opt.paramFile));
    assertThat(ret.param3, is(opt.param3));
    assertThat(ret.otherPath, is(opt.otherPath)); // no escape chars
    assertThat(ret.param4.internal1, is(opt.param4.internal1));
    assertThat(ret.tab, is(opt.tab));
    assertThat(ret.param4.tabint, is(opt.param4.tabint));

  }

  /**
   * Test of serialization->deserialization for macro processing serialzers.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerDeser_arrays_1() throws Exception {
    Options3 opt = new Options3();
    String js = opt.serialize2Macro(); // change to json
    logger.debug(js);
    assertThat(js, containsString("(space space)")); // escaped in json
    assertThat(js, containsString("(path/to/file with spaces.qconf)"));
    assertThat(StringUtils.countMatches(js, "("), is(2));
    assertThat(StringUtils.countMatches(js, ")"), is(2));
    Options3 ret = AbstractPluginOptions.deserialize2Macro(js, new Options3()); // back to object
    assertThat(ret.param2, is(opt.param2));
    assertThat(ret.paramFile, is(opt.paramFile));
    assertThat(ret.param3, is(opt.param3));
    assertThat(ret.otherPath, is(opt.otherPath)); // no escape chars
    assertThat(ret.param4.internal1, is(opt.param4.internal1));
    assertThat(ret.tab, is(opt.tab));
    assertThat(ret.param4.tabint, is(opt.param4.tabint));
    assertThat(ret.param4.tabstr, is(opt.param4.tabstr));

  }

  /**
   * Test of serialization->deserialization for macro processing serialzers.
   * 
   * <p>Empty array in json.
   * 
   * @throws Exception on error
   */
  @Test
  public void testSerDeser_arrays_2() throws Exception {
    String js = "{param2:10,param3:3.14,otherPath:(space space),"
            + "param4:{tabint:[],internal1:20," + "tabstr:[aba,vddd,ffgth]},param5:{},"
            + "tab:[3.14,2.14]," + "paramFile:(path/to/file with spaces.qconf)}";
    AbstractPluginOptions.deserialize2Macro(js, new Options3()); // back to object
  }

  /**
   * Test of serialization->deserialization for macro processing serialzers.
   * 
   * <p>No array closing
   * 
   * @throws Exception on error
   */
  @Test(expected = QuimpPluginException.class)
  public void testSerDeser_arrays_3() throws Exception {
    String js = "{param2:10,param3:3.14,otherPath:(space space),"
            + "param4:{tabint:[0,0,internal1:20," + "tabstr:[aba,vddd,ffgth]},param5:{},"
            + "tab:[3.14,2.14]," + "paramFile:(path/to/file with spaces.qconf)}";
    AbstractPluginOptions.deserialize2Macro(js, new Options3()); // back to object
  }

  /**
   * Test of serialization->deserialization for macro processing serialzers.
   * 
   * <p>No array closing
   * 
   * @throws Exception on error
   */
  @Test(expected = QuimpPluginException.class)
  public void testSerDeser_arrays_4() throws Exception {
    String js = "{param2:10,param3:3.14,otherPath:(space space),"
            + "param4:{tabint:[0,0],internal1:20," + "tabstr:[aba,vddd,ffgth]},param5:{},"
            + "tab:[3.14,2.14," + "paramFile:(path/to/file with spaces.qconf)}";
    AbstractPluginOptions.deserialize2Macro(js, new Options3()); // back to object
  }

  /**
   * Test class.
   * 
   * @author p.baniukiewicz
   *
   */
  class Options extends AbstractPluginOptions {

    /** The param 2. */
    int param2 = 10;

    /** The param 3. */
    @EscapedPath // should be ignored
    double param3 = 3.14;

    /** The other path. */
    @EscapedPath()
    String otherPath = "space space";

    /** The param 4. */
    Internal param4 = new Internal();

    /**
     * The Class Internal.
     */
    class Internal {

      /** The internal 1. */
      public int internal1 = 20;

    }

    /**
     * Instantiates a new options.
     */
    public Options() {
      paramFile = "path/to/file with spaces.qconf";
    }
  }

  /**
   * The Class Empty.
   */
  class Empty {

  }

  /**
   * Test class.
   * 
   * @author p.baniukiewicz
   *
   */
  class Options2 extends AbstractPluginOptions {

    /** The param 2. */
    int param2 = 10;

    /** The param 3. */
    @EscapedPath // should be ignored
    double param3 = 3.14;

    /** The other path. */
    @EscapedPath()
    String otherPath = "space space";

    /** The param 4. */
    Internal param4 = new Internal();

    /** The param 5. */
    Empty param5 = new Empty(); // test {}

    /** The tab. */
    double[] tab = new double[2];

    /**
     * The Class Internal.
     */
    class Internal {

      /** The tabint. */
      int[] tabint = new int[2];

      /** The internal 1. */
      public int internal1 = 20;

      /**
       * Instantiates a new internal.
       */
      public Internal() {
        tabint[0] = 1;
        tabint[1] = 2;
      }
    }

    /**
     * Instantiates a new options 2.
     */
    public Options2() {
      paramFile = "path/to/file with spaces.qconf";
      tab[0] = 3.14;
      tab[1] = 2.14;
    }
  }

  /**
   * Test class.
   * 
   * @author p.baniukiewicz
   *
   */
  class Options3 extends AbstractPluginOptions {

    /** The param 2. */
    int param2 = 10;

    /** The param 3. */
    @EscapedPath // should be ignored
    double param3 = 3.14;

    /** The other path. */
    @EscapedPath()
    String otherPath = "space space";

    /** The param 4. */
    Internal param4 = new Internal();

    /** The param 5. */
    Empty param5 = new Empty(); // test {}

    /** The tab. */
    double[] tab = new double[2]; // empty

    /**
     * The Class Internal.
     */
    class Internal {

      /** The tabint. */
      int[] tabint = new int[2];

      /** The internal 1. */
      public int internal1 = 20;

      /** The tabstr. */
      String[] tabstr;

      /**
       * Instantiates a new internal.
       */
      public Internal() {
        tabstr = new String[3];
        tabstr[0] = "aba";
        tabstr[1] = "vddd";
        tabstr[2] = "ffgth";
      }
    }

    /**
     * Instantiates a new options 3.
     */
    public Options3() {
      paramFile = "path/to/file with spaces.qconf";
      tab[0] = 3.14;
      tab[1] = 2.14;
    }
  }

  /**
   * Test class.
   * 
   * @author p.baniukiewicz
   *
   */
  class Options4 extends AbstractPluginOptions {

    /** The param 2. */
    int param2 = 10;

    /** The param 3. */
    @EscapedPath // should be ignored
    double param3 = 3.14;

    /** The other path. */
    @EscapedPath()
    String otherPath = "space space";

    /** The param 4. */
    Internal param4 = new Internal();

    /**
     * The Class Internal.
     */
    class Internal {

      /** The internal 1. */
      public int internal1 = 20;

    }

    /**
     * Instantiates a new options.
     */
    public Options4() {
      paramFile = "c:\\path\\to\\file with spaces.qconf";
    }
  }
}
