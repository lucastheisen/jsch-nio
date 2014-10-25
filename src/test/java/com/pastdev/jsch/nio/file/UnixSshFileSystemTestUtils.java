package com.pastdev.jsch.nio.file;


import static org.junit.Assert.fail;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.IOUtils;


public class UnixSshFileSystemTestUtils {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemWatchServiceTest.class );
    protected static String filesystemPath;
    protected static String hostname;
    protected static int port;
    protected static Properties properties;
    protected static String sshPath;
    protected static URI uri;
    protected static String username;
    public static final Charset UTF8 = Charset.forName( "UTF-8" );

    static {
        java.security.Security.insertProviderAt(
                new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1 );
    }

    public static void closeFileSystem() {
        if ( uri != null ) {
            IOUtils.closeAndLogException( FileSystems.getFileSystem( uri ) );
        }
    }

    public static void initializeFileSystem() {
        InputStream inputStream = null;
        try {
            inputStream = ClassLoader.getSystemResourceAsStream( "configuration.properties" );
            Assume.assumeNotNull( inputStream );
            properties = new Properties();
            properties.load( inputStream );
        }
        catch ( IOException e ) {
            logger.warn( "cant find properties file (tests will be skipped): {}", e.getMessage() );
            logger.debug( "cant find properties file:", e );
            Assume.assumeNoException( e );
        }
        finally {
            if ( inputStream != null ) {
                try {
                    inputStream.close();
                }
                catch ( IOException e ) {
                    // really, i dont care...
                }
            }
        }

        String knownHosts = properties.getProperty( "ssh.knownHosts" );
        String privateKey = properties.getProperty( "ssh.privateKey" );
        username = properties.getProperty( "ssh.username" );
        hostname = "localhost";
        port = Integer.parseInt( properties.getProperty( "ssh.port" ) );

        sshPath = properties.getProperty( "ssh.test.sshPath" );
        filesystemPath = properties.getProperty( "ssh.test.filesystemPath" );

        Map<String, Object> environment = new HashMap<String, Object>();
        String environmentPrefix = "ssh.environment.";
        for ( Object keyObject : properties.keySet() ) {
            String key = (String)keyObject;
            if ( key.startsWith( environmentPrefix ) ) {
                environment.put( key.substring( environmentPrefix.length() ), properties.getProperty( key ) );
            }
        }

        DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory( username, hostname, port );
        try {
            defaultSessionFactory.setKnownHosts( knownHosts );
            defaultSessionFactory.setIdentityFromPrivateKey( privateKey );
            environment.put( "defaultSessionFactory", defaultSessionFactory );

            uri = new URI( "ssh.unix://" + username + "@" + hostname + ":" + port + sshPath );
            FileSystems.newFileSystem( uri, environment );
        }
        catch ( JSchException e ) {
            Assume.assumeNoException( e );
        }
        catch ( URISyntaxException e ) {
            logger.error( "unable to build default filesystem uri: {}", e.getMessage() );
            logger.error( "unable to build default filesystem uri: ", e );
            fail( "unable to build default filesystem uri: " + e.getMessage() );
        }
        catch ( IOException e ) {
            Assume.assumeNoException( e );
        }
    }
}
