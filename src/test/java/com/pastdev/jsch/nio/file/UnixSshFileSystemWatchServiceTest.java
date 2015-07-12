package com.pastdev.jsch.nio.file;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.pastdev.jsch.IOUtils;


public class UnixSshFileSystemWatchServiceTest extends FileSystemTestUtils {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemWatchServiceTest.class );

    @AfterClass
    public static void afterClass() {
        try {
            closeFileSystem();
        }
        catch ( AssumptionViolatedException e ) {
            Assume.assumeNoException( e );
        }
    }

    @BeforeClass
    public static void beforeClass() {
        try {
            initializeFileSystem( UnixSshFileSystemProvider.SCHEME_SSH_UNIX );
        }
        catch ( AssumptionViolatedException e ) {
            Assume.assumeNoException( e );
        }
    }

    @Test
    public void testWatchService() {
        final String root = UUID.randomUUID().toString();
        UnixSshFileSystem fileSystem = (UnixSshFileSystem)FileSystems.getFileSystem( uri );

        String filename = "newfile.txt";
        Path rootFilePath = Paths.get( filesystemPath, root );
        File rootFile = rootFilePath.toFile();
        Path newFile = rootFilePath.resolve( filename );
        rootFile.mkdirs();

        Path rootPath = fileSystem.getPath( root );
        WatchService watcher = null;
        try {
            try {
                watcher = fileSystem.newWatchService();
                UnixSshPathWatchKey watchKey = (UnixSshPathWatchKey)rootPath
                        .register( watcher, StandardWatchEventKinds.ENTRY_CREATE );
                assertEquals( rootPath, watchKey.watchable() );
                assertTrue( watchKey.waitForInitialization( 10, TimeUnit.SECONDS ) );

                String expected = "i know you expected this";
                try (OutputStream outputStream = newFile.getFileSystem().provider().newOutputStream( newFile )) {
                    IOUtils.copyFromString( expected, outputStream );
                }

                watchKey.runImmediately();
                WatchKey took = null;
                try {
                    took = watcher.take();
                }
                catch ( InterruptedException e ) {
                    fail( "interrupted during take" );
                }

                assertEquals( watchKey, took );
                boolean first = true;
                for ( WatchEvent<?> event : took.pollEvents() ) {
                    assertTrue( first );
                    assertEquals( StandardWatchEventKinds.ENTRY_CREATE, event.kind() );

                    Path eventPath = (Path)event.context();
                    assertEquals( filename, eventPath.toString() );

                    try (InputStream inputStream = eventPath.getFileSystem().provider().newInputStream(
                            rootPath.resolve( eventPath ) )) {
                        assertEquals( expected, IOUtils.copyToString( inputStream ) );
                    }
                    first = false;
                }
            }
            catch ( IOException e ) {
                logger.error( "could not obtain watch service from {}: {}", fileSystem, e );
                logger.debug( "could not obtain watch service:", e );
                fail( "could not obtain watch service from " + fileSystem + ": " + e.getMessage() );
            }

        }
        finally {
            if ( watcher != null ) {
                IOUtils.closeAndLogException( watcher );
            }
            IOUtils.deleteFiles( newFile.toFile(), rootFile );
        }
    }
}
