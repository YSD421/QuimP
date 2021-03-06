package com.github.celldynamics.quimp;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.scijava.vecmath.Tuple2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.filesystem.IQuimpSerialize;
import com.github.celldynamics.quimp.geom.ExtendedVector2d;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Low level snake definition. Form snake from Node objects. Snake is defined by first head node.
 * Remaining nodes are in bidirectional linked list.
 * 
 * <p>Node list may be modified externally but then method such as findNode(), updateNormals(),
 * calcCentroid() should be called to update internal fields of Snake. If number of nodes changes it
 * is recommended to create new object.
 * 
 * @author rtyson
 * @author baniuk
 *
 */
public class Snake extends Shape<Node> implements IQuimpSerialize {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(Snake.class.getName());
  /**
   * true if snake is alive Changed during segmentation and user interaction.
   * 
   * <p>snake is killed by {@link SnakeHandler#kill()} method usually called after unsuccessful
   * segmentation.
   */
  public boolean alive;
  /**
   * unique ID of snake Given during Snake creation by SnakeHandler. It is possible to set id
   * explicitly by com.github.celldynamics.quimp.Snake.setSnakeID(int)
   */
  private int snakeID;
  /**
   * how many nodes at start of segmentation.
   */
  public double startingNnodes;
  /**
   * number of nodes frozen Changed during segmentation.
   */
  private int FROZEN; // name related to QCONF file do not change
  /**
   * Snake bounds, updated only on use getBounds(). Even though this field is serialised it is
   * recalculated in afterSerialzie() and beforeSerialzie()
   */
  private Rectangle bounds = new Rectangle();

  /**
   * Create a snake from existing linked list (at least one head node).
   * 
   * <p>List is referenced only not copied Behaviour of this method was changed. Now it does not
   * make
   * copy of Node. In old approach there was dummy node deleted in this constructor.
   * 
   * @param h Node of list
   * @param n Number of nodes
   * @param id Unique snake ID related to object being segmented.
   */
  public Snake(final Node h, int n, int id) {
    super(h, n);
    snakeID = id;
    this.makeAntiClockwise(); // can affect centroid on last positions, so calculate it afterwards
    this.updateNormals(BOA_.qState.segParam.expandSnake);
    alive = true;
    startingNnodes = POINTS / 100.; // as 1%. limit to X%
    countFrozen(); // set FROZEN
    // calcOrientation();
    getBounds();
  }

  /**
   * Copy constructor with new ID.
   * 
   * @param src Snake to be duplicated
   * @param id New id
   */
  public Snake(final Snake src, int id) {
    super(src);
    alive = src.alive;
    snakeID = id;
    startingNnodes = src.startingNnodes;
    countFrozen();
    bounds = new Rectangle(src.bounds);
  }

  /**
   * Copy constructor.
   * 
   * @param src to be duplicated
   */
  public Snake(final Snake src) {
    this(src, src.getSnakeID());
  }

  /**
   * Create snake from ROI
   * 
   * @param r ROI with object to be segmented
   * @param id Unique ID of snake related to object being segmented.
   * @param direct direct
   * @throws BoaException on wrong number of polygon points
   */
  public Snake(final Roi r, int id, boolean direct) throws BoaException {
    // place nodes in a circle
    snakeID = id;
    if (r.getType() == Roi.RECTANGLE || r.getType() == Roi.POLYGON) {
      if (direct) {
        intializePolygonDirect(r.getFloatPolygon());
      } else {
        intializePolygon(r.getFloatPolygon());
      }
    } else {
      Rectangle rect = r.getBounds();
      int xc = rect.x + rect.width / 2;
      int yc = rect.y + rect.height / 2;
      int rx = rect.width / 2;
      int ry = rect.height / 2;

      intializeOval(0, xc, yc, rx, ry, BOA_.qState.segParam.getNodeRes() / 2);
    }
    startingNnodes = POINTS / 100.; // as 1%. limit to X%
    alive = true;
    // colour = QColor.lightColor();
    // calcOrientation();
    calcCentroid();
    getBounds();
  }

  /**
   * Initialises snake from PolyginRoi.
   * 
   * @see #Snake(Roi, int, boolean)
   * @param r polygon to initialise Snake
   * @param id id of Snake
   * @throws BoaException on wrong number of polygon points
   */
  public Snake(final PolygonRoi r, int id) throws BoaException {
    snakeID = id;
    intializeFloat(r.getFloatPolygon());
    startingNnodes = POINTS / 100.; // as 1%. limit to X%
    alive = true;
    // colour = QColor.lightColor();
    // calcOrientation();
    calcCentroid();
    getBounds();
  }

  /**
   * Construct Snake object from list of nodes. Head node is always first element from array.
   * 
   * @param list list of nodes as Vector2d
   * @param id id of Snake
   * @throws BoaException on wrong number of array points (<3).
   */
  public Snake(final List<? extends Tuple2d> list, int id) throws BoaException {
    super(list, new Node(0), BOA_.qState.segParam.expandSnake);
    if (list.size() <= 3) { // compatibility
      throw new BoaException("Not enough points provided");
    }
    snakeID = id;
    this.makeAntiClockwise(); // specific to snake can affect updateNormals
    updateNormals(BOA_.qState.segParam.expandSnake); // called in super, here just in case
    startingNnodes = POINTS / 100;
    alive = true;
    getBounds();
  }

  /**
   * Construct Snake object from X and Y arrays. Head node is always first element from array.
   * 
   * @param x x coordinates of nodes
   * @param y y coordinates of nodes
   * @param id id of Snake
   * @throws BoaException on wrong number of array points (<3).
   */
  public Snake(final double[] x, final double[] y, int id) throws BoaException {
    super(x, y, new Node(0), BOA_.qState.segParam.expandSnake);
    if ((x.length != y.length) || x.length <= 3) {
      throw new BoaException(
              "Lengths of X and Y arrays are not equal or there is less than 3 nodes");
    }
    snakeID = id;
    this.makeAntiClockwise(); // specific to snake can affect updateNormals
    updateNormals(BOA_.qState.segParam.expandSnake); // called in super, here just in case
    startingNnodes = POINTS / 100;
    alive = true;
    getBounds();
  }

  /**
   * Construct empty Snake object.
   * 
   */
  public Snake() {
    super();
    alive = false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + FROZEN;
    result = prime * result + (alive ? 1231 : 1237);
    result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
    result = prime * result + snakeID;
    long temp;
    temp = Double.doubleToLongBits(startingNnodes);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof Snake)) {
      return false;
    }
    Snake other = (Snake) obj;
    if (FROZEN != other.FROZEN) {
      return false;
    }
    if (alive != other.alive) {
      return false;
    }
    if (bounds == null) {
      if (other.bounds != null) {
        return false;
      }
    } else if (!bounds.equals(other.bounds)) {
      return false;
    }
    if (snakeID != other.snakeID) {
      return false;
    }
    if (Double.doubleToLongBits(startingNnodes) != Double.doubleToLongBits(other.startingNnodes)) {
      return false;
    }
    return true;
  }

  /**
   * Get ID of Snake.
   * 
   * @return the snakeID
   */
  public int getSnakeID() {
    return snakeID;
  }

  /**
   * Change current snakeID. Should be used carefully.
   * 
   * @param snakeID the snakeID to set
   */
  protected void setSnakeID(int snakeID) {
    this.snakeID = snakeID;
  }

  /**
   * Initialises Node list from ROIs other than polygons For non-polygon ROIs ellipse is used
   * as first approximation of segmented shape. Parameters of ellipse are estimated usually using
   * parameters of bounding box of user ROI This method differs from other initialize* methods
   * by input data which do not contain nodes but the are defined analytically
   * 
   * @param t index of node
   * @param xc center of ellipse
   * @param yc center of ellipse
   * @param rx ellipse diameter
   * @param ry ellipse diameter
   * @param s number of nodes
   * 
   * @throws BoaException Exception if polygon contains too little nodes.
   */
  private void intializeOval(int t, int xc, int yc, int rx, int ry, double s) throws BoaException {
    head = new Node(t); // make a dummy head node for list initialization
    POINTS = 1;
    FROZEN = 0;
    head.setPrev(head); // link head to itself
    head.setNext(head);
    head.setHead(true);

    double theta = 2.0 / (double) ((rx + ry) / 2);

    // nodes are added in behind the head node
    Node node;
    for (double a = 0.0; a < (2 * Math.PI); a += s * theta) {
      node = new Node(nextTrackNumber);
      nextTrackNumber++;
      node.getPoint().setX((int) (xc + rx * Math.cos(a)));
      node.getPoint().setY((int) (yc + ry * Math.sin(a)));
      addPoint(node);
    }
    removeNode(head); // remove dummy head node
    this.makeAntiClockwise();
    updateNormals(BOA_.qState.segParam.expandSnake);
  }

  /**
   * Initialises Node list from polygon Each edge of input polygon is divided on
   * {@link BOAState.SegParam#getNodeRes()}
   * 
   * @param p Polygon extracted from IJ ROI
   * @throws BoaException if polygon contains too little nodes.
   */
  private void intializePolygon(final FloatPolygon p) throws BoaException {
    // System.out.println("poly with node distance");
    head = new Node(0); // make a dummy head node for list initialization
    POINTS = 1;
    FROZEN = 0;
    head.setPrev(head); // link head to itself
    head.setNext(head);
    head.setHead(true);

    Node node;
    int j;
    int nn;
    double x;
    double y;
    double spacing;
    ExtendedVector2d a;
    ExtendedVector2d b;
    ExtendedVector2d u;
    for (int i = 0; i < p.npoints; i++) {
      j = ((i + 1) % (p.npoints)); // for last i point we turn for first one closing polygon
      a = new ExtendedVector2d(p.xpoints[i], p.ypoints[i]);// vectors ab define edge
      b = new ExtendedVector2d(p.xpoints[j], p.ypoints[j]);

      nn = (int) Math.ceil(ExtendedVector2d.lengthP2P(a, b) / BOA_.qState.segParam.getNodeRes());
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
        addPoint(node);
      }
    }
    removeNode(head); // remove dummy head node new head will be set
    setPositions();
    this.makeAntiClockwise();
    updateNormals(BOA_.qState.segParam.expandSnake);
  }

  /**
   * Initializes Node list from polygon Does not refine points. Use only those nodes available in
   * polygon.
   * 
   * @param p Polygon extracted from IJ ROI
   * @throws BoaException if polygon contains too little nodes.
   * @see #intializePolygon(FloatPolygon)
   */
  private void intializePolygonDirect(final FloatPolygon p) throws BoaException {
    // System.out.println("poly direct");
    head = new Node(0); // make a dummy head node for list initialization
    POINTS = 1;
    FROZEN = 0;
    head.setPrev(head); // link head to itself
    head.setNext(head);
    head.setHead(true);

    Node node;
    for (int i = 0; i < p.npoints; i++) {
      node = new Node((double) p.xpoints[i], (double) p.ypoints[i], nextTrackNumber++);
      addPoint(node);
    }

    removeNode(head); // remove dummy head node
    setPositions();
    this.makeAntiClockwise();
    updateNormals(BOA_.qState.segParam.expandSnake);
  }

  /**
   * Create Snake from polygon.
   * 
   * @param p polygon to initialise snake from
   * @see #intializePolygonDirect(FloatPolygon)
   * @throws BoaException if polygon contains too little nodes.
   */
  private void intializeFloat(final FloatPolygon p) throws BoaException {
    // FIXME This method is the same as intializePolygonDirect(FloatPolygon)
    head = new Node(0); // make a dummy head node
    POINTS = 1;
    FROZEN = 0;
    head.setPrev(head); // link head to itself
    head.setNext(head);
    head.setHead(true);

    Node node;
    for (int i = 0; i < p.npoints; i++) {
      node = new Node((double) p.xpoints[i], (double) p.ypoints[i], nextTrackNumber++);
      addPoint(node);
    }

    removeNode(head); // remove dummy head node
    setPositions();
    this.makeAntiClockwise();
    updateNormals(BOA_.qState.segParam.expandSnake);
  }

  /**
   * Prints the snake.
   */
  public void printSnake() {
    System.out.println("Print Nodes (" + POINTS + ")");
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
    if (i != POINTS) {
      System.out.println("NODES and linked list dont tally!!");
    }
  }

  /**
   * Unfreeze all nodes.
   */
  @Override
  public void unfreezeAll() {
    super.unfreezeAll();
    FROZEN = 0;
  }

  /**
   * Go through whole list and count Nodes that are frozen. Set FREEZE variable
   */
  private void countFrozen() {
    Node n = head;
    FROZEN = 0;
    do {
      if (n.isFrozen()) {
        FROZEN++;
      }
      n = n.getNext();
    } while (!n.isHead());
  }

  /**
   * Freeze a specific node.
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
   * Unfreeze a specific node.
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
   * Check if all nodes are frozen.
   * 
   * @return true if all nodes are frozen
   */
  public boolean isFrozen() {
    if (FROZEN == POINTS) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Remove selected node from list.
   * 
   * <p>Perform check if removed node was head and if it was, the new head is randomly selected.
   * Neighbors are linked together
   * 
   * @param n Node to remove
   * 
   * @throws BoaException on insufficient number of nodes
   */
  public final void removeNode(Node n) throws BoaException {
    if (POINTS <= 3) {
      throw new BoaException("removeNode: Did not remove node. " + POINTS + " nodes remaining.", 0,
              2);
    }
    if (n.isFrozen()) {
      FROZEN--;
    }
    super.removePoint(n, BOA_.qState.segParam.expandSnake);
  }

  /**
   * Implode.
   *
   * @throws BoaException if oval could not be initialized.
   */
  public void implode() throws BoaException {
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
    cx = cx / POINTS;
    cy = cy / POINTS;

    intializeOval(nextTrackNumber, (int) cx, (int) cy, 4, 4, 1);
  }

  /**
   * Blowup.
   *
   * @throws Exception the exception
   */
  @Deprecated
  public void blowup() throws Exception {
    scaleSnake(BOA_.qState.segParam.blowup, 4, true);
  }

  /**
   * Scale current Snake by amount in increments of stepSize.
   * 
   * <p>Updates centroid and normalised <tt>position</tt>.
   * 
   * @param amount scale
   * @param stepRes increment
   * @param correct if true it corrects the node distance
   * @throws BoaException if node distance correction failed
   * @see com.github.celldynamics.quimp.Shape#scale(double)
   * @see Outline#scaleOutline(double, double, double, double)
   */
  public void scaleSnake(double amount, double stepRes, boolean correct) throws BoaException {
    if (amount == 0) {
      return;
    }
    // make sure snake access is clockwise
    Node.setClockwise(true);
    // scale the snake by 'amount', in increments of 'stepsize'
    if (amount > 0) {
      stepRes *= -1; // scale down if amount negative
    }
    double steps = Math.abs(amount / stepRes);
    // IJ.log(""+steps);
    int j;
    for (j = 0; j < steps; j++) {
      super.scale(stepRes);
      if (correct) {
        correctDistance(false);
      }
      cutLoops();
      updateNormals(BOA_.qState.segParam.expandSnake);
    }
    calcCentroid();
    setPositions();
  }

  /**
   * Cut out a loop Insert a new node at cut point.
   */
  public void cutLoops() {
    final int maxInterval = 12; // how far ahead do you check for a loop
    int interval;
    int state;

    Node nodeA;
    Node nodeB;
    double[] intersect = new double[2];
    Node newN;

    boolean cutHead;

    nodeA = head;
    do {
      cutHead = (nodeA.getNext().isHead()) ? true : false;
      nodeB = nodeA.getNext().getNext(); // don't check next edge as they can't cross, but do touch

      // always leave 3 nodes, at least
      interval = (POINTS > maxInterval + 3) ? maxInterval : (POINTS - 3);

      for (int i = 0; i < interval; i++) {
        if (nodeB.isHead()) {
          cutHead = true;
        }
        state = ExtendedVector2d.segmentIntersection(nodeA.getX(), nodeA.getY(),
                nodeA.getNext().getX(), nodeA.getNext().getY(), nodeB.getX(), nodeB.getY(),
                nodeB.getNext().getX(), nodeB.getNext().getY(), intersect);
        if (state == 1) {
          // System.out.println("CutLoops: cut out a loop");
          newN = this.insertNode(nodeA);
          newN.setX(intersect[0]);
          newN.setY(intersect[1]);

          newN.setNext(nodeB.getNext());
          nodeB.getNext().setPrev(newN);

          newN.updateNormale(BOA_.qState.segParam.expandSnake);
          nodeB.getNext().updateNormale(BOA_.qState.segParam.expandSnake);

          // set velocity
          newN.setVel(nodeB.getVel());
          if (newN.getVel().length() < BOA_.qState.segParam.vel_crit) {
            newN.getVel().makeUnit();
            newN.getVel().multiply(BOA_.qState.segParam.vel_crit * 1.5);
          }

          if (cutHead) {
            newN.setHead(true); // put a new head in
            head = newN;
          }

          POINTS -= (i + 2); // the one skipped and the current one
          break;
        }
        nodeB = nodeB.getNext();
      }
      nodeA = nodeA.getNext();
    } while (!nodeA.isHead());
  }

  /**
   * Cut out intersects. Done once at the end of each frame to cut out any parts of the contour
   * that self intersect. Similar to cutLoops, but check all edges (NODES / 2) and cuts out the
   * smallest section
   * 
   * @see #cutLoops()
   * @see com.github.celldynamics.quimp.Outline#cutSelfIntersects()
   */
  public void cutIntersects() {

    int interval;
    int state;

    Node nodeA;
    Node nodeB;
    double[] intersect = new double[2];
    Node newN;

    boolean cutHead;

    nodeA = head;
    do {
      cutHead = (nodeA.getNext().isHead()) ? true : false;
      nodeB = nodeA.getNext().getNext();// don't check next edge as they can't cross, but do touch
      interval = (POINTS > 6) ? POINTS / 2 : 2; // always leave 3 nodes, at least

      for (int i = 2; i < interval; i++) {
        if (nodeB.isHead()) {
          cutHead = true;
        }

        state = ExtendedVector2d.segmentIntersection(nodeA.getX(), nodeA.getY(),
                nodeA.getNext().getX(), nodeA.getNext().getY(), nodeB.getX(), nodeB.getY(),
                nodeB.getNext().getX(), nodeB.getNext().getY(), intersect);

        if (state == 1) {
          newN = this.insertNode(nodeA);
          newN.setX(intersect[0]);
          newN.setY(intersect[1]);

          newN.setNext(nodeB.getNext());
          nodeB.getNext().setPrev(newN);

          newN.updateNormale(BOA_.qState.segParam.expandSnake);
          nodeB.getNext().updateNormale(BOA_.qState.segParam.expandSnake);

          if (cutHead) {
            newN.setHead(true); // put a new head in
            head = newN;
          }

          POINTS -= (i);
          break;
        }
        nodeB = nodeB.getNext();
      }

      nodeA = nodeA.getNext();
    } while (!nodeA.isHead());
  }

  /**
   * Ensure nodes are between maxDist and minDist apart, add remove nodes as required.
   * 
   * @param shiftNewNode shiftNewNode
   * @throws BoaException when number of nodes is less than 3 after removal
   */
  public void correctDistance(boolean shiftNewNode) throws BoaException {
    Node.randDirection(); // choose a random direction to process the chain

    ExtendedVector2d tanL;
    ExtendedVector2d tanR;
    ExtendedVector2d tanLR;
    ExtendedVector2d npos;
    double dl;
    double dr;
    double dlr;
    double tmp;

    Node nc = head;
    Node nl;
    Node nr; // neighbours

    do {

      nl = nc.getPrev(); // left neighbour
      nr = nc.getNext(); // left neighbour

      // compute tangent
      tanL = ExtendedVector2d.vecP2P(nl.getPoint(), nc.getPoint());
      tanR = ExtendedVector2d.vecP2P(nc.getPoint(), nr.getPoint());
      tanLR = ExtendedVector2d.vecP2P(nl.getPoint(), nr.getPoint());
      dl = tanL.length();
      dr = tanR.length();
      dlr = tanLR.length();

      if (dl < BOA_.qState.segParam.getMin_dist() || dr < BOA_.qState.segParam.getMin_dist()) {
        // nC is to close to a neigbour
        if (dlr > 2 * BOA_.qState.segParam.getMin_dist()) {

          // move nC to middle
          npos = new ExtendedVector2d(tanLR.getX(), tanLR.getY());
          npos.multiply(0.501); // half
          npos.addVec(nl.getPoint());

          nc.setX(npos.getX());
          nc.setY(npos.getY());

          // tmp = Math.sqrt((dL*dL) - ((dLR/2.)*(dLR/2.)));
          // System.out.println("too close, move to middle, tmp:
          // "+tmp);

          tmp = Math.sin(ExtendedVector2d.angle(tanL, tanLR)) * dl;
          // tmp = Vec2d.distPointToSegment(nC.getPoint(),
          // nL.getPoint(), nR.getPoint());
          nc.getNormal().multiply(-tmp);
          nc.getPoint().addVec(nc.getNormal());

          nc.updateNormale(BOA_.qState.segParam.expandSnake);
          nl.updateNormale(BOA_.qState.segParam.expandSnake);
          nr.updateNormale(BOA_.qState.segParam.expandSnake);
          this.unfreezeNode(nc);

        } else {
          // delete nC
          // System.out.println("delete node");
          removeNode(nc);
          nl.updateNormale(BOA_.qState.segParam.expandSnake);
          nr.updateNormale(BOA_.qState.segParam.expandSnake);
          if (nr.isHead()) {
            break;
          }
          nc = nr.getNext();
          continue;
        }
      }
      if (dl > BOA_.qState.segParam.getMax_dist()) {

        // System.out.println("1357-insert node");
        Node nins = insertNode(nl);
        nins.setVel(nl.getVel());
        nins.getVel().addVec(nc.getVel());
        nins.getVel().multiply(0.5);
        if (nins.getVel().length() < BOA_.qState.segParam.vel_crit) {
          nins.getVel().makeUnit();
          nins.getVel().multiply(BOA_.qState.segParam.vel_crit * 1.5);
        }

        npos = new ExtendedVector2d(tanL.getX(), tanL.getY());
        npos.multiply(0.51);
        npos.addVec(nl.getPoint());

        nins.setX(npos.getX());
        nins.setY(npos.getY());
        nins.updateNormale(BOA_.qState.segParam.expandSnake);
        if (shiftNewNode) {
          nins.getNormal().multiply(-2); // move out a bit
          nins.getPoint().addVec(nins.getNormal());
          nins.updateNormale(BOA_.qState.segParam.expandSnake);
        }
        nl.updateNormale(BOA_.qState.segParam.expandSnake);
        nr.updateNormale(BOA_.qState.segParam.expandSnake);
        nc.updateNormale(BOA_.qState.segParam.expandSnake);

      }

      nc = nc.getNext();
    } while (!nc.isHead());

    Node.setClockwise(true); // reset to clockwise (although shouldnt effect things??)
  }

  /**
   * Insert default Node after Node v.
   * 
   * @param n Node to insert new Node after
   * @return Inserted Node
   */
  public Node insertNode(final Node n) {
    return insertPoint(n, new Node());
  }

  /**
   * Return current Snake as \b POLYLINE
   * 
   * @return ij.gui.PolygonRoi.PolygonRoi as \b POLYLINE type
   */
  Roi asPolyLine() {
    float[] x = new float[POINTS];
    float[] y = new float[POINTS];

    Node n = head;
    int i = 0;
    do {
      x[i] = (float) n.getX();
      y[i] = (float) n.getY();
      i++;
      n = n.getNext();
    } while (!n.isHead());
    return new PolygonRoi(x, y, POINTS, Roi.POLYLINE);
  }

  /**
   * Gets bounds of snake.
   * 
   * @return Bounding box of current Snake object
   */
  public Rectangle getBounds() {

    Rectangle2D.Double rect = getDoubleBounds();

    bounds.setBounds((int) rect.getMinX(), (int) rect.getMinY(), (int) rect.getWidth(),
            (int) rect.getHeight());
    return bounds;
  }

  /**
   * Edits the snake.
   */
  public void editSnake() {
    System.out.println("Editing a snake");
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Snake [alive=" + alive + ", snakeID=" + snakeID + ", startingNnodes=" + startingNnodes
            + ", FROZEN=" + FROZEN + ", bounds=" + bounds + ", POINTS=" + POINTS + ", centroid="
            + centroid + ", toString()=" + super.toString() + "]";
  }

  /**
   * Call super and then oo Snake related actions
   * 
   * @see com.github.celldynamics.quimp.Shape#beforeSerialize()
   */
  @Override
  public void beforeSerialize() {
    super.beforeSerialize();
    getBounds();
  }

  /**
   * Call super and then oo Snake related actions
   * 
   * @see com.github.celldynamics.quimp.Shape#afterSerialize()
   */
  @Override
  public void afterSerialize() throws Exception {
    super.afterSerialize();
    getBounds();
  }

}