package com.pastdev.jsch.nio.file;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.pastdev.jsch.IOUtils;


public class UnixSshSftpHybridFileSystemTest extends UnixSshFileSystemTest {
    private static final Logger logger = LoggerFactory.getLogger( UnixSshSftpHybridFileSystemTest.class );

    @BeforeClass
    public static void beforeClass() {
        initializeFileSystem( UnixSshSftpHybridFileSystemProvider.SCHEME_SSH_SFTP_HYBRID_UNIX );
    }

    @Test
    public void testCreateDirectory() {
        String root = UUID.randomUUID().toString();
        String dir = "testcreatdir";

        File rootDir = new File( filesystemPath, root );
        Path path = FileSystems.getFileSystem( uri ).getPath( root ).resolve( dir );
        try {
            logger.debug( "making root dir {}", rootDir );
            rootDir.mkdirs();

            logger.trace( "creating subdirectory {}", path );
            path.getFileSystem().provider().createDirectory( path );
            logger.trace( "created" );

            assertTrue( Files.isDirectory( Paths.get( rootDir.getAbsolutePath(), dir ) ) );
        }
        catch ( IOException e ) {
            logger.error( "failed for {}: {}", path, e );
            logger.debug( "failed:", e );
            fail( "failed for " + path + ": " + e.getMessage() );
        }
        finally {
            logger.trace( "deleting [{}]", rootDir );
            IOUtils.deleteFiles( new File( rootDir, dir ), rootDir );
        }
    }
}
