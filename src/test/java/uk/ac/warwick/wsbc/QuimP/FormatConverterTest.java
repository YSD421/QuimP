package uk.ac.warwick.wsbc.QuimP;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import uk.ac.warwick.wsbc.QuimP.filesystem.QconfLoader;

/**
 * @author p.baniukiewicz
 *
 */
public class FormatConverterTest {

    static Object accessPrivate(String name, FormatConverter obj, Object[] param,
            Class<?>[] paramtype) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method prv = obj.getClass().getDeclaredMethod(name, paramtype);
        prv.setAccessible(true);
        return prv.invoke(obj, param);
    }

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
        Files.deleteIfExists(Paths.get(Paths.get(".").toAbsolutePath().normalize().toString()
                + "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0.snQP"));
        Files.deleteIfExists(Paths.get(Paths.get(".").toAbsolutePath().normalize().toString()
                + "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0.paQP"));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.FormatConverter#generatepaQP()}.
     */
    @Test
    @Ignore
    public void testGeneratepaQP() throws Exception {
        FormatConverter fC = new FormatConverter(
                new File("src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth.QCONF"));

        accessPrivate("generatepaQP", fC, new Object[] {}, new Class<?>[] {});

        Thread.sleep(1000);
        // compare paQP
        // manualy generated one
        BufferedReader readerexpected = new BufferedReader(new FileReader(
                "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0_expected.paQP"));
        // expected
        BufferedReader readertest = new BufferedReader(new FileReader(
                "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0.paQP"));

        readerexpected.readLine(); // skip header with random controlsum
        readertest.readLine();
        readerexpected.readLine();
        readertest.readLine();
        readerexpected.readLine();
        readertest.readLine();
        char[] expected = new char[800];
        char[] test = new char[800];
        readerexpected.read(expected, 0, expected.length);
        readerexpected.close();
        readertest.read(test, 0, test.length);
        readertest.close();

        assertThat(test, is(expected));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.FormatConverter#generatesnQP()}.
     */
    @Test
    @Ignore
    public void testGeneratesnQP() throws Exception {
        FormatConverter fC = new FormatConverter(
                new File("src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth.QCONF"));
        accessPrivate("generatesnQP", fC, new Object[] {}, new Class<?>[] {});
        Thread.sleep(1000);
        // compare paQP
        // manualy generated one
        BufferedReader readerexpected = new BufferedReader(new FileReader(
                "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0_expected.paQP"));
        // expected
        BufferedReader readertest = new BufferedReader(new FileReader(
                "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0.paQP"));

        readerexpected.readLine(); // skip header with random controlsum
        readertest.readLine();
        readerexpected.readLine();
        readertest.readLine();
        readerexpected.readLine();
        readertest.readLine();
        char[] expected = new char[800];
        char[] test = new char[800];
        readerexpected.read(expected, 0, expected.length);
        readerexpected.close();
        readertest.read(test, 0, test.length);
        readertest.close();

        assertThat(test, is(expected));
    }

    /**
     * Test method for
     * {@link uk.ac.warwick.wsbc.QuimP.FormatConverter#FormatConverter(uk.ac.warwick.wsbc.QuimP.QParamsQconf, java.nio.file.Path)}.
     */
    @Test
    @Ignore
    public void testFormatConverterQParamsQconfPath() throws Exception {
        QconfLoader qC = new QconfLoader(
                Paths.get("src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth.QCONF")
                        .toFile());
        FormatConverter fC = new FormatConverter(qC);
        fC.generateOldDataFile();
        Thread.sleep(1000);
        // compare paQP
        // manualy generated one
        BufferedReader readerexpected = new BufferedReader(new FileReader(
                "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0_expected.paQP"));
        // expected
        BufferedReader readertest = new BufferedReader(new FileReader(
                "src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth_0.paQP"));

        readerexpected.readLine(); // skip header with random controlsum
        readertest.readLine();
        readerexpected.readLine();
        readertest.readLine();
        readerexpected.readLine();
        readertest.readLine();
        char[] expected = new char[800];
        char[] test = new char[800];
        readerexpected.read(expected, 0, expected.length);
        readerexpected.close();
        readertest.read(test, 0, test.length);
        readertest.close();

        assertThat(test, is(expected));
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.FormatConverter#generateNewDataFile()}.
     */
    @Test
    public void testGenerateNewDataFile() throws Exception {
        QconfLoader qC = new QconfLoader(new File(
                "/home/baniuk/Documents/Repos/QuimP/src/test/resources/FormatConverter/res/0pt7%agar_bleb2_0.paQP"));
        FormatConverter fC = new FormatConverter(qC);
        accessPrivate("generateNewDataFile", fC, new Object[] {}, new Class<?>[] {});
    }

    /**
     * Test method for {@link uk.ac.warwick.wsbc.QuimP.FormatConverter#generateOldDataFile()}.
     */
    @Test
    public void testGenerateOldDataFile() throws Exception {
        QconfLoader qC = new QconfLoader(new File(
                "/home/baniuk/Documents/Repos/QuimP/src/test/resources/FormatConverter/fluoreszenz-test_eq_smooth.QCONF"));
        FormatConverter fC = new FormatConverter(qC);
        fC.generateOldDataFile();
    }

}
