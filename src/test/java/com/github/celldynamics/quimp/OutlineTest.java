package com.github.celldynamics.quimp;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.scijava.vecmath.Point2d;

import com.github.celldynamics.quimp.plugin.utils.QuimpDataConverter;

import ij.gui.PolygonRoi;

/**
 * The Class OutlineTest.
 *
 * @author p.baniukiewicz
 * @see com.github.celldynamics.quimp.geom.filters.OutlineProcessorTest#testShrinkLin()
 */
public class OutlineTest extends JsonKeyMatchTemplate<Outline> {

  /**
   * Configure test.
   * 
   * <p>do not use randomizer in JsonKeyMatchTemplate (we build object already.
   */
  public OutlineTest() {
    super(1, true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.JsonKeyMatchTemplate#setUp()
   */
  @Override
  public void setUp() throws Exception {
    obj = getRandomOutline(); // build outline
    indir = "com.github.celldynamics.quimp.Outline";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.JsonKeyMatchTemplate#prepare()
   */
  @Override
  protected void prepare() throws Exception {
    super.prepare();
  }

  /**
   * Test of {@link Outline#Outline(Outline)}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testOutline() throws Exception {
    Outline copy = new Outline(obj);
    assertThat(copy.POINTS, is(obj.POINTS));
    assertThat(copy, is(obj));
    assertThat(EqualsBuilder.reflectionEquals(obj, copy, false), is(true)); // copy is same
  }

  /**
   * Test method for
   * {@link com.github.celldynamics.quimp.Outline#Outline(com.github.celldynamics.quimp.Snake)}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testOutlineSnake() throws Exception {
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(0, 0));
    list.add(new Point2d(1, 10));
    list.add(new Point2d(5, 9));
    list.add(new Point2d(6, 2));
    list.add(new Point2d(7, -1));
    Snake snake = new Snake(list, 10);
    Outline outline = new Outline(snake);

    assertThat(outline.getNumPoints(), is(snake.getNumPoints()));
    assertThat(outline.getCentroid(), is(snake.getCentroid()));
    assertThat(outline.nextTrackNumber, is(snake.nextTrackNumber));
    assertThat(outline.getHead(), instanceOf(Vert.class));
    Node n = snake.getHead();
    Vert v = outline.getHead();
    do {
      assertThat(v.getCurvatureLocal(), is(n.getCurvatureLocal()));
      assertThat(v.isHead(), is(n.isHead()));
      n = n.getNext();
      v = v.getNext();
    } while (!n.isHead());
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#correctDensity(double, double)}.
   * 
   * <p>Looped list with distances of 1.0. Lack of two points separated
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCorrectDensity_1() throws Exception {
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(1, 0));
    list.add(new Point2d(2, 0));
    // ?
    list.add(new Point2d(4, 0));
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    list.add(new Point2d(4, 1));
    // ?
    list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    Outline outline;
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(1.9, 0.9);

    List<Point2d> expList = new ArrayList<>();
    expList.add(new Point2d(1, 0));
    expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    expList.add(new Point2d(1, 1));
    Outline outlineExp = new QuimpDataConverter(expList).getOutline();

    assertThat(outline.asList(), is(outlineExp.asList()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#correctDensity(double, double)}.
   * 
   * <p>Looped list with distances of 1.0. Lack of three points neighbours just after head
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCorrectDensity_2() throws Exception {
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(1, 0));
    // ?
    // ?
    // ?
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    list.add(new Point2d(4, 1));
    list.add(new Point2d(3, 1));
    list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    Outline outline;
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(1.9, 0.9);

    List<Point2d> expList = new ArrayList<>();
    expList.add(new Point2d(1, 0));
    expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    expList.add(new Point2d(1, 1));
    Outline outlineExp = new QuimpDataConverter(expList).getOutline();

    assertThat(outline.asList(), is(outlineExp.asList()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#correctDensity(double, double)}.
   * 
   * <p>Looped list with distances of 1.0. Lack of three points neighbours far after head
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCorrectDensity_3() throws Exception {
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(1, 0));
    list.add(new Point2d(2, 0));
    list.add(new Point2d(3, 0));
    list.add(new Point2d(4, 0));
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    // list.add(new Point2d(4, 1));
    // list.add(new Point2d(3, 1));
    // list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    Outline outline;
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(1.9, 0.9);

    List<Point2d> expList = new ArrayList<>();
    expList.add(new Point2d(1, 0));
    expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    expList.add(new Point2d(1, 1));
    Outline outlineExp = new QuimpDataConverter(expList).getOutline();

    assertThat(outline.asList(), is(outlineExp.asList()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#correctDensity(double, double)}.
   * 
   * <p>Looped list with distances of 1.0. All point every second to be removed (min distance 1.1)
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCorrectDensity_4() throws Exception {
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(1, 0));
    list.add(new Point2d(2, 0));
    list.add(new Point2d(3, 0));
    list.add(new Point2d(4, 0));
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    list.add(new Point2d(4, 1));
    list.add(new Point2d(3, 1));
    list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    Outline outline;
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(1.9, 1.1);

    List<Point2d> expList = new ArrayList<>();
    expList.add(new Point2d(1, 0));
    // expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    // expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    // expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    // expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    // expList.add(new Point2d(1, 1));
    Outline outlineExp = new QuimpDataConverter(expList).getOutline();

    assertThat(outline.asList(), is(outlineExp.asList()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#correctDensity(double, double)}.
   * 
   * <p>Looped list with distances of 1.0. Point head to remove (point before too close)
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCorrectDensity_5() throws Exception {
    Field f = Shape.class.getDeclaredField("threshold");
    f.setAccessible(true);
    f.setDouble(Shape.class, 0.0); // head next
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(1, 0));
    list.add(new Point2d(2, 0));
    list.add(new Point2d(3, 0));
    list.add(new Point2d(4, 0));
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    list.add(new Point2d(4, 1));
    list.add(new Point2d(3, 1));
    list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    list.add(new Point2d(1, 0.1));
    Outline outline;
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(1.9, 0.2);

    List<Point2d> expList = new ArrayList<>();
    // expList.add(new Point2d(1, 0));
    expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    expList.add(new Point2d(1, 1));
    expList.add(new Point2d(1, 0.1));
    Outline outlineExp = new QuimpDataConverter(expList).getOutline();

    assertThat(outline.asList(), is(outlineExp.asList()));

    f.setDouble(Shape.class, 0.0); // head prev
    list.clear();
    list.add(new Point2d(1, 0));
    list.add(new Point2d(2, 0));
    list.add(new Point2d(3, 0));
    list.add(new Point2d(4, 0));
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    list.add(new Point2d(4, 1));
    list.add(new Point2d(3, 1));
    list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    list.add(new Point2d(1, 0.1));
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(1.9, 0.2);

    expList.clear();
    // expList.add(new Point2d(1, 0));
    expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    expList.add(new Point2d(1, 1));
    expList.add(new Point2d(1, 0.1));
    outlineExp = new QuimpDataConverter(expList).getOutline();

    f.setDouble(Shape.class, 0.5);
    assertThat(outline.asList(), is(outlineExp.asList()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#correctDensity(double, double)}.
   * 
   * <p>Looped list with distances of 1.0. removing over inserting, inserted vertex can be next
   * removed if it is too close. Does not work viceversa as inserting does not update current vertex
   * but removing does. And removing is checked first
   * 
   * @throws Exception Exception
   */
  @Test
  public void testCorrectDensity_6() throws Exception {
    Field f = Shape.class.getDeclaredField("threshold");
    f.setAccessible(true);
    f.setDouble(Shape.class, 0.0); // head next
    List<Point2d> list = new ArrayList<>();
    list.add(new Point2d(1, 0));
    list.add(new Point2d(2, 0));
    list.add(new Point2d(3, 0));
    list.add(new Point2d(4, 0));
    list.add(new Point2d(5, 0));
    list.add(new Point2d(5, 1));
    list.add(new Point2d(4, 1));
    list.add(new Point2d(3, 1));
    list.add(new Point2d(2, 1));
    list.add(new Point2d(1, 1));
    list.add(new Point2d(1, 0.1));
    Outline outline;
    outline = new QuimpDataConverter(list).getOutline();
    outline.correctDensity(0.8, 0.7);

    List<Point2d> expList = new ArrayList<>();
    // expList.add(new Point2d(1, 0));
    expList.add(new Point2d(2, 0));
    expList.add(new Point2d(3, 0));
    expList.add(new Point2d(4, 0));
    expList.add(new Point2d(5, 0));
    expList.add(new Point2d(5, 1));
    expList.add(new Point2d(4, 1));
    expList.add(new Point2d(3, 1));
    expList.add(new Point2d(2, 1));
    expList.add(new Point2d(1, 1));
    expList.add(new Point2d(1, 0.1));
    Outline outlineExp = new QuimpDataConverter(expList).getOutline();
    f.setDouble(Shape.class, 0.5); // restore
    assertThat(outline.asList(), is(outlineExp.asList()));
  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#Outline(List)}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testOutlineDoubleArrayDoubleArray() throws Exception {
    // retrieve arrays from random 4 element snake
    double[] x = new QuimpDataConverter(obj).getX();
    double[] y = new QuimpDataConverter(obj).getY();

    // cant compare using equal as obj is completely random
    Outline newOutline = new Outline(x, y);
    assertThat(newOutline.getNumPoints(), is(obj.getNumPoints()));
    assertThat(newOutline.xtoArr(), is(x));
    assertThat(newOutline.ytoArr(), is(y));
    assertThat(newOutline.getHead().getPoint(), is(obj.getHead().getPoint()));

  }

  /**
   * Test method for {@link com.github.celldynamics.quimp.Outline#Outline(double[], double[])}.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testOutlineList() throws Exception {
    // retrieve list from random 4 element snake
    List<Point2d> list = new QuimpDataConverter(obj).getList();

    // cant compare using equal as obj is completely random
    Outline newOutline = new Outline(list);
    assertThat(newOutline.getNumPoints(), is(obj.getNumPoints()));
    assertThat(newOutline.asList(), is(obj.asList()));
    assertThat(newOutline.getHead().getPoint(), is(obj.getHead().getPoint()));

  }

  /**
   * Return random 4 element outline. All nodes are random.
   * 
   * @return 4 elements outline.
   */
  public static Outline getRandomOutline() {
    List<Vert> list = VertTest.getRandomVertPointList(); // get list of random vertexes
    Vert head = list.get(0); // get head of list

    return new Outline(head, list.size()); // build outline
  }

  /**
   * Test if initialised outline has all geometric properties set up correctly.
   * 
   * @throws Exception on error
   */
  @Test
  public void testOutlineInitGeomProperties() throws Exception {
    List<Point2d> p = AbstractCircularShape.getCircle();
    Outline s = new Outline(p);
    AbstractCircularShape.validateOutlineGeomProperties(s);
  }

  /**
   * Test if initialised outline has all geometric properties set up correctly.
   * 
   * @throws Exception on error
   */
  @Test
  public void testOutlineInitGeomProperties_1() throws Exception {
    Outline s = new Outline(AbstractCircularShape.getX(), AbstractCircularShape.getY());
    AbstractCircularShape.validateOutlineGeomProperties(s);
  }

  /**
   * Test if initialised outline has all geometric properties set up correctly.
   * 
   * @throws Exception on error
   */
  @Test
  public void testOutlineInitGeomProperties_2() throws Exception {
    Field f = Shape.class.getDeclaredField("threshold");
    f.setAccessible(true);
    f.setDouble(Shape.class, 0.0);
    PolygonRoi pr = new PolygonRoi(AbstractCircularShape.getXfloat(),
            AbstractCircularShape.getYfloat(), PolygonRoi.FREEROI);
    Outline s = new Outline(pr);
    AbstractCircularShape.validateOutlineGeomProperties(s);
    f.setDouble(Shape.class, 0.5);
  }

  /**
   * Test if initialised outline has all geometric properties set up correctly.
   * 
   * @throws Exception on error
   */
  @Test
  public void testOutlineInitGeomProperties_3() throws Exception {
    Outline s = new Outline(AbstractCircularShape.getX(), AbstractCircularShape.getY());
    Outline cp = new Outline(s);
    AbstractCircularShape.validateOutlineGeomProperties(cp);
  }

  /**
   * Test if initialised outline has all geometric properties set up correctly.
   * 
   * @throws Exception on error
   */
  @Test
  public void testOutlineInitGeomProperties_4() throws Exception {
    Field f = Shape.class.getDeclaredField("threshold");
    f.setAccessible(true);
    f.setDouble(Shape.class, 0.0);
    Outline s =
            new Outline(AbstractCircularShape.getVertList(false), AbstractCircularShape.NUMVERT);
    AbstractCircularShape.validateOutlineGeomProperties(s);
    f.setDouble(Shape.class, 0.5);
  }

}
