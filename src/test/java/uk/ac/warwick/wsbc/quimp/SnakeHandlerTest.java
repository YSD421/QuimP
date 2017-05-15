package uk.ac.warwick.wsbc.quimp;

/**
 * Verify keys in JSon for tested class (field naming).
 * 
 * @author p.baniukiewicz
 *
 */
public class SnakeHandlerTest extends JsonKeyMatchTemplate<SnakeHandler> {

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.JsonKeyMatchTemplate#setUp()
   */
  @Override
  public void setUp() throws Exception {
    obj = new SnakeHandler();
    indir = "uk.ac.warwick.wsbc.quimp.SnakeHandler";
  }

  /*
   * (non-Javadoc)
   * 
   * @see uk.ac.warwick.wsbc.quimp.JsonKeyMatchTemplate#prepare()
   */
  @Override
  protected void prepare() throws Exception {
    ser.doBeforeSerialize = false;
    super.prepare();
  }

}
