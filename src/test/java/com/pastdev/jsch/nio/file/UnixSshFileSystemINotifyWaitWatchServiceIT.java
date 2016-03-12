package com.pastdev.jsch.nio.file;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.IOUtils;


public class UnixSshFileSystemINotifyWaitWatchServiceIT extends FileSystemTestUtils {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemINotifyWaitWatchServiceIT.class );

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
            initializeFileSystem( UnixSshFileSystemProvider.SCHEME_SSH_UNIX, "it.ssh" );
        }
        catch ( AssumptionViolatedException e ) {
            Assume.assumeNoException( e );
        }
    }

    private void runCommand( String command ) {
        UnixSshFileSystem fileSystem = (UnixSshFileSystem)FileSystems.getFileSystem( uri );
        try {
            fileSystem.getCommandRunner().execute( command );
        }
        catch ( JSchException | IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Test
    public void testWatchService() {
        final String root = UUID.randomUUID().toString();
        UnixSshFileSystem fileSystem = (UnixSshFileSystem)FileSystems.getFileSystem( uri );

        String filename = "newfile.txt";
        String rootFilePath = filesystemPath + "/" + root;
        Path rootPath = fileSystem.getPath( root );
        WatchService watcher = null;
        runCommand( "mkdir -p '" + rootFilePath + "'" );
        try {
            try {
                watcher = fileSystem.newWatchService();
                UnixSshPathWatchKey watchKey = (UnixSshPathWatchKey)rootPath
                        .register( watcher, StandardWatchEventKinds.ENTRY_CREATE );
                assertEquals( rootPath, watchKey.watchable() );
                assertTrue( watchKey.waitForInitialization( 10, TimeUnit.SECONDS ) );

                String expected = "i know you expected this";
                runCommand( "printf '" + expected + "' | dd of=" + rootFilePath + "/" + filename );

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
            runCommand( "rm -rf '" + rootFilePath + "'" );
        }
    }

    @Test
    public void testWatchServiceClose() {
        final String root = UUID.randomUUID().toString();
        UnixSshFileSystem fileSystem = (UnixSshFileSystem)FileSystems.getFileSystem( uri );

        String rootFilePath = filesystemPath + "/" + root;
        Path rootPath = fileSystem.getPath( root );
        WatchService watcher = null;
        runCommand( "mkdir -p '" + rootFilePath + "'" );
        try {
            watcher = fileSystem.newWatchService();
            UnixSshPathWatchKey watchKey = (UnixSshPathWatchKey)rootPath
                    .register( watcher, StandardWatchEventKinds.ENTRY_CREATE );
            assertEquals( rootPath, watchKey.watchable() );
            assertTrue( watchKey.waitForInitialization( 10, TimeUnit.SECONDS ) );

            watchKey.cancel();
        }
        catch ( IOException e ) {
            fail( e.getMessage() );
        }
        finally {
            if ( watcher != null ) {
                IOUtils.closeAndLogException( watcher );
            }
            runCommand( "rm -rf '" + rootFilePath + "'" );
        }
    }
}
