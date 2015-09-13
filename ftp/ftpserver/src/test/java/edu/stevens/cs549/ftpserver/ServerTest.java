package edu.stevens.cs549.ftpserver;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Properties;

import edu.stevens.cs549.ftpinterface.IServer;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ServerTest 
    extends TestCase
{
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     * @throws IOException 
     */
    public ServerTest(String testName) throws IOException
    {
        super( testName );
        
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ServerTest.class );
    }

    /**
     * Rigourous Test :-)
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public void testApp() throws FileNotFoundException, IOException
    {
    	Assert.assertTrue(true);
    }
    

}
