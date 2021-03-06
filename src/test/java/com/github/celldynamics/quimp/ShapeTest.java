package com.github.celldynamics.quimp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.vecmath.Point2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.geom.ExtendedVector2d;

/**
 * The Class ShapeTest.
 *
 * @author p.baniukiewicz
 * @see com.github.celldynamics.quimp.SnakeTest
 */
public class ShapeTest {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(ShapeTest.class.getName());

  /** The head. */
  private Vert head;

  /** The v 1. */
  private Vert v1;

  /** The v 2. */
  private Vert v2;

  /** The v 3. */
  private Vert v3;

  /** The test. */
  TestShape test;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    List<Vert> ret = com.github.celldynamics.quimp.VertTest.getRandomVertPointList();

    head = ret.get(0);
    v1 = ret.get(1);
    v2 = ret.get(2);
    v3 = ret.get(3);

    test = new TestShape(head, 4);
  }

  /**
   * Tear down.
   *
   * @throws Exception the exception
   */
  @After
  public void tearDown() throws Exception {
    head = null;
    v1 = null;
    v2 = null;
    v3 = null;
    test = null;
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.Shape#setHead(com.github.celldynamics.quimp.PointsList)}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSetHead() throws Exception {

    assertThat(head.isHead(), is(true));

    test.setHead(v1);
    assertThat(head.isHead(), is(false));
    assertThat(v1.isHead(), is(true));
    assertThat(test.getHead(), is(v1));

    test.setHead(v3);
    assertThat(head.isHead(), is(false));
    assertThat(v1.isHead(), is(false));
    assertThat(v3.isHead(), is(true));
    assertThat(test.getHead(), is(v3));
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.Shape#setHead(com.github.celldynamics.quimp.PointsList)}.
   * 
   * <p>Set same head as it was.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSetHead_1() throws Exception {

    assertThat(head.isHead(), is(true));

    test.setHead(head);
    assertThat(head.isHead(), is(true));
    assertThat(test.getHead(), is(head));

  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.Shape#setHead(com.github.celldynamics.quimp.PointsList)}.
   * 
   * @throws Exception Exception
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSetHead_noVert() throws Exception {
    assertThat(head.isHead(), is(true));

    Vert dummy = new Vert();
    dummy.setHead(true);
    test.setHead(dummy);
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Shape#checkIsHead()}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCheckIsHead() throws Exception {
    assertThat(test.checkIsHead(), is(not(nullValue())));
    assertThat(test.checkIsHead(), is(head));
    head.setHead(false); // accidently remove head marker from wrapped list
    assertThat(test.checkIsHead(), is(nullValue()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Shape#setHead(int)}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSetNewHead() throws Exception {
    assertThat(test.getHead(), is(head));
    test.setHead(23569856); // non existing
    assertThat(test.getHead(), is(head));

    test.setHead(3); // set to id=3
    assertThat(test.getHead(), is(v2));
    assertThat(head.isHead(), is(false));
  }

  /**
   * Test of Shape constructor.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testShape() throws Exception {
    // looped list with head
    TestShape ts = new TestShape(head, 4);
    assertThat(ts.getHead(), is(head));

    // looped list with head but given other vertex
    TestShape ts1 = new TestShape(v1, 4);
    assertThat(ts1.getHead(), is(head)); // original head is discovered

    // looped list no head
    head.setHead(false);
    TestShape ts2 = new TestShape(v1, 4);
    assertThat(ts2.getHead(), is(v1)); // head is set to current

    // only one vertex
    Vert v = com.github.celldynamics.quimp.VertTest.getRandomVert(1);
    v.setNext(null);
    v.setPrev(null);
    TestShape ts3 = new TestShape(v);
    assertThat(ts3.getHead(), is(v));
    assertThat(ts3.getHead().getNext(), is(v));
    assertThat(ts3.getHead().getPrev(), is(v));
    assertThat(ts3.getCentroid(), is(new ExtendedVector2d(v.getX(), v.getY())));

  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Shape#validateShape()}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testValidateShape() throws Exception {
    assertThat(test.validateShape(), is(Shape.LIST_OK));
    test.getHead().head = false; // cancel head tag for head
    assertThat(test.validateShape(), is(Shape.BAD_HEAD | Shape.NO_HEAD));
    test.getHead().head = true; // restore it
    head.getNext().setNext(null); // but break linking
    assertThat(test.validateShape(), is(Shape.BAD_LINKING));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Shape#validateShape()}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testValidateShape_1() throws Exception {
    assertThat(test.validateShape(), is(Shape.LIST_OK));
    test.getHead().head = false; // cancel head tag for head
    assertThat(test.validateShape(), is(Shape.BAD_HEAD | Shape.NO_HEAD));
    test.getHead().head = true; // restore it
    head.getNext().setNext(null); // but break linking
    assertThat(test.validateShape(), is(Shape.BAD_LINKING));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Shape#validateShape()}. Wrong number of
   * given elements.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testValidateShape_2() throws Exception {
    TestShape test1 = new TestShape(head, 2); // wrong number of points in list
    assertThat(test1.validateShape(), is(Shape.BAD_NUM_POINTS));
  }

  /**
   * Test of Shape constructor.
   * 
   * @throws Exception Exception
   */
  @Test(expected = IllegalArgumentException.class)
  public void testShape_1() throws Exception {
    // not looped list no head
    head.setHead(false);
    head.setPrev(null);
    v3.setNext(null);
    TestShape ts2 = new TestShape(v1, 4);
  }

  /**
   * Test iterator.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testIterator() throws Exception {
    Iterator<Vert> iter = test.iterator();
    int tracknumber = 1;
    while (iter.hasNext()) {
      Vert v = iter.next();
      LOGGER.debug(v.toString());
      assertThat(v.getTrackNum(), is(tracknumber++)); // elements in order
    }
    assertThat(tracknumber, is(test.POINTS + 1)); // 4 points in list
  }

  /**
   * Test iterator. One element - looped
   * 
   * @throws Exception Exception
   */
  @Test
  public void testIterator_1() throws Exception {
    Vert vinit = com.github.celldynamics.quimp.VertTest.getRandomVert(1);
    vinit.setHead(true);
    vinit.setNext(vinit);
    vinit.setPrev(vinit);

    TestShape test = new TestShape(vinit, 1);
    Iterator<Vert> iter = test.iterator();
    int tracknumber = 1;
    while (iter.hasNext()) {
      Vert v = iter.next();
      LOGGER.debug(v.toString());
      assertThat(v.getTrackNum(), is(tracknumber++)); // elements in order
    }
    assertThat(tracknumber, is(test.POINTS + 1)); // 4 points in list
  }

  /**
   * Test iterator. Default constructor
   * 
   * @throws Exception Exception
   */
  @Test
  public void testIterator_2() throws Exception {
    TestShape test = new TestShape();
    Iterator<Vert> iter = test.iterator();
    int tracknumber = 0;
    while (iter.hasNext()) {
      ;
      tracknumber++; // never enter here
    }
    assertThat(tracknumber, is(test.POINTS)); // 0 points in list
  }

  /**
   * Test class.
   * 
   * @author p.baniukiewicz
   *
   */
  class TestShape extends Shape<Vert> {

    /**
     * Instantiates a new test shape.
     */
    /*
     * 
     */
    public TestShape() {
      super();
    }

    /**
     * Instantiates a new test shape.
     *
     * @param src the src
     * @param destType the dest type
     */
    /*
     * 
     */
    public TestShape(Shape<Vert> src, Vert destType) {
      super(src, destType);
    }

    /**
     * Instantiates a new test shape.
     *
     * @param src the src
     */
    /*
     * 
     */
    public TestShape(Shape<Vert> src) {
      super(src);
    }

    /**
     * Instantiates a new test shape.
     *
     * @param h the h
     * @param n the n
     */
    /*
     * 
     */
    public TestShape(Vert h, int n) {
      super(h, n);
    }

    /**
     * Instantiates a new test shape.
     *
     * @param h the h
     */
    /*
     * 
     */
    public TestShape(Vert h) {
      super(h);
    }
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Shape#countPoints()}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCountPoints() throws Exception {
    assertThat(test.countPoints(), is(test.POINTS));
  }

  /**
   * Test method for {@link Shape#reverseShape()}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testReverseShape() throws Exception {
    List<Point2d> p = AbstractCircularShape.getCircle();
    Snake s = new Snake(p, 0);
    AbstractCircularShape.validateShapeGeomProperties(s, true, BOA_.qState.segParam.expandSnake);
    s.reverseShape(); // check revese but id does not update normals
    AbstractCircularShape.validateShapeGeomProperties(s, false, !BOA_.qState.segParam.expandSnake);
  }

}