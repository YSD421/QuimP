package com.github.celldynamics.quimp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.geom.ExtendedVector2d;

/**
 * Represents a node in the snake - its basic component In fact this class stands for bidirectional
 * list containing Nodes. Every node has assigned 2D position and several additional properties such
 * as:
 * <ul>
 * <li>velocity of Node</li>
 * <li>total force of Node</li>
 * <li>normal vector</li>
 * </ul>
 * 
 * @author rtyson
 * @author p.baniukiewicz
 *
 */
public class Node extends PointsList<Node> {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(Node.class.getName());
  /**
   * Velocity of the nodes, initialised in
   * {@link Constrictor#constrict(Snake, ij.process.ImageProcessor)}.
   */
  private ExtendedVector2d vel;
  /**
   * Total force at node, initialized in
   * {@link Constrictor#constrict(Snake, ij.process.ImageProcessor)}.
   */
  private ExtendedVector2d F_total;
  /**
   * Point to move node to after all new node positions have been calc initialized in
   * {@link Constrictor#constrict(Snake, ij.process.ImageProcessor)}.
   */
  private ExtendedVector2d prelimPoint;

  /**
   * Default constructor. Create empty Node with default parameters, not linked to other Nodes
   */
  public Node() {
    super();
    F_total = new ExtendedVector2d();
    vel = new ExtendedVector2d();
    prelimPoint = new ExtendedVector2d();
  }

  /**
   * Create Node with given id.
   * 
   * @param t id of Node
   * @see com.github.celldynamics.quimp.PointsList
   */
  public Node(int t) {
    super(t);
    F_total = new ExtendedVector2d();
    vel = new ExtendedVector2d();
    prelimPoint = new ExtendedVector2d();
  }

  /**
   * Copy constructor. Copy properties of Node
   * 
   * <p>Previous or next points are not copied
   * 
   * @param src Source Node
   */
  public Node(final Node src) {
    super(src);
    this.vel = new ExtendedVector2d(src.vel);
    this.F_total = new ExtendedVector2d(src.F_total);
    this.prelimPoint = new ExtendedVector2d(src.prelimPoint);
  }

  /**
   * Create Node from coordinates.
   * 
   * @param xx x-axis coordinate
   * @param yy y-axis coordinate
   * @param t id of Node
   */
  public Node(double xx, double yy, int t) {
    super(xx, yy, t);
    F_total = new ExtendedVector2d();
    vel = new ExtendedVector2d();
    prelimPoint = new ExtendedVector2d();
  }

  /**
   * (non-Javadoc) Compare only current Node, no neighbours.
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((F_total == null) ? 0 : F_total.hashCode());
    result = prime * result + ((prelimPoint == null) ? 0 : prelimPoint.hashCode());
    result = prime * result + ((vel == null) ? 0 : vel.hashCode());
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
    if (!(obj instanceof Node)) {
      return false;
    }
    Node other = (Node) obj;
    if (F_total == null) {
      if (other.F_total != null) {
        return false;
      }
    } else if (!F_total.equals(other.F_total)) {
      return false;
    }
    if (prelimPoint == null) {
      if (other.prelimPoint != null) {
        return false;
      }
    } else if (!prelimPoint.equals(other.prelimPoint)) {
      return false;
    }
    if (vel == null) {
      if (other.vel != null) {
        return false;
      }
    } else if (!vel.equals(other.vel)) {
      return false;
    }
    return true;
  }

  /**
   * Update point and force with preliminary values, and reset.
   */
  public void update() {
    setX(getX() + prelimPoint.getX());
    setY(getY() + prelimPoint.getY());
    prelimPoint.setX(0);
    prelimPoint.setY(0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Node [vel=" + vel + ", F_total=" + F_total + ", prelimPoint=" + prelimPoint + ", point="
            + point + ", normal=" + normal + ", tan=" + tan + ", head=" + head + ", tracknumber="
            + tracknumber + ", position=" + position + ", frozen=" + isFrozen() + "]";
  }

  /**
   * Setter to F_total field.
   * 
   * @return F_total
   */
  public ExtendedVector2d getF_total() {
    return F_total;
  }

  /**
   * Setter to vel field.
   * 
   * @return vel
   */
  public ExtendedVector2d getVel() {
    return vel;
  }

  /**
   * Set total force for Node.
   * 
   * @param f vector of force to assign to Node force
   */
  public void setF_total(ExtendedVector2d f) {
    F_total.setX(f.getX());
    F_total.setY(f.getY());
  }

  /**
   * Set velocity for Node.
   * 
   * @param v vector of velocity to assign to Node force
   */
  public void setVel(ExtendedVector2d v) {
    vel.setX(v.getX());
    vel.setY(v.getY());
  }

  /**
   * Update total force for Node.
   * 
   * @param f vector of force to add to Node force
   */
  public void addF_total(ExtendedVector2d f) {
    // add the xy values in f to xy F_total i.e updates total Force
    F_total.setX(F_total.getX() + f.getX());
    F_total.setY(F_total.getY() + f.getY());
  }

  /**
   * Update velocity for Node.
   * 
   * @param v vector of velocity to add to Node force
   */
  public void addVel(ExtendedVector2d v) {
    // adds the xy values in v to Vel i.e. updates velocity
    vel.setX(vel.getX() + v.getX());
    vel.setY(vel.getY() + v.getY());
  }

  /**
   * Set preliminary point for Node.
   * 
   * @param v vector of preliminary point to assign to Node force
   */
  public void setPrelim(ExtendedVector2d v) {
    prelimPoint.setX(v.getX());
    prelimPoint.setY(v.getY());
  }
}