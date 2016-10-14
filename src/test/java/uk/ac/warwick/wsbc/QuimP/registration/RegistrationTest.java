package uk.ac.warwick.wsbc.QuimP.registration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author p.baniukiewicz
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RegistrationTest {

    static Object accessPrivate(String name, Registration obj, Object[] param, Class<?>[] paramtype)
            throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method prv = obj.getClass().getDeclaredMethod(name, paramtype);
        prv.setAccessible(true);
        return prv.invoke(obj, param);
    }

    private ModalityType modalityType;

    private Window owner;
    private String title;
    @InjectMocks
    private Registration registration;

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
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.registration.Registration#validateRegInfo(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testValidateRegInfo() throws Exception {
        Registration obj = new Registration(null, "");
        String email = "";
        String key = "";
        boolean ret = (boolean) accessPrivate("validateRegInfo", obj, new Object[] { email, key },
                new Class<?>[] { String.class, String.class });
        assertFalse(ret);
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.registration.Registration#validateRegInfo(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testValidateRegInfo_1() throws Exception {
        Registration obj = new Registration(null, "");
        String email = " ";
        String key = "";
        boolean ret = (boolean) accessPrivate("validateRegInfo", obj, new Object[] { email, key },
                new Class<?>[] { String.class, String.class });
        assertFalse(ret);
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.registration.Registration#validateRegInfo(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testValidateRegInfo_2() throws Exception {
        Registration obj = new Registration(null, "");
        String email = "baniuk1@gmail.com";
        String key = "d2264e17765b74627e67e73dcad1d9d4";
        boolean ret = (boolean) accessPrivate("validateRegInfo", obj, new Object[] { email, key },
                new Class<?>[] { String.class, String.class });
        assertTrue(ret);
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.registration.Registration#validateRegInfo(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testValidateRegInfo_3() throws Exception {
        Registration obj = new Registration(null, "");
        String email = " baniuk1@gmail.com";
        String key = "d2264e17765b74627e67e73dcad1d9d4 ";
        boolean ret = (boolean) accessPrivate("validateRegInfo", obj, new Object[] { email, key },
                new Class<?>[] { String.class, String.class });
        assertTrue(ret);
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.registration.Registration#registerUser(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testRegisterUser() throws Exception {
        Registration obj = new Registration(null, "");
        String email = " baniuk1@gmail.com";
        String key = "d2264e17765b74627e67e73dcad1d9d4 ";
        accessPrivate("registerUser", obj, new Object[] { email, key },
                new Class<?>[] { String.class, String.class });
    }

}