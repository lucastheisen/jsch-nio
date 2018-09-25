package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.pastdev.jsch.IOUtils;


public class UnixSshFileSystemTest extends FileSystemTestUtils {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemTest.class );
    private static final String expected = "Lets give em something to talk about.";

    @AfterClass
    public static void afterClass() {
        closeFileSystem();
    }

    @BeforeClass
    public static void beforeClass() {
        initializeFileSystem( UnixSshFileSystemProvider.SCHEME_SSH_UNIX );
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

    @Test
    public void testDirectoryStreamEmptyDir() throws IOException {
        final String root = UUID.randomUUID().toString();
        Path rootPath = Paths.get( filesystemPath, root );

        // create test dir
        assertFalse( Files.exists( rootPath ) );
        Files.createDirectories( rootPath );
        assertTrue( Files.exists( rootPath ) );
        assertTrue( Files.isDirectory( rootPath ) );

        // test dirstream
        DirectoryStream<Path> ds = Files.newDirectoryStream(
                FileSystems.getFileSystem( uri ).getPath( root ) );
        try {
            Iterator<Path> iter = ds.iterator();
            assertFalse( iter.hasNext() );
            try {
                iter.next();
                fail( "expected an exception" );
            }
            catch ( NoSuchElementException e ) {
                // pass
            }
        }
        finally {
            ds.close();
        }
    }

    @Test
    public void testExists() {
        Path defaultPath = Paths.get( uri );
        assertTrue( Files.exists( defaultPath ) );
        assertTrue( Files.isDirectory( defaultPath ) );
        assertEquals( sshPath, defaultPath.toString() );
        Path rootPath = defaultPath.resolve( "/" );
        assertTrue( Files.exists( rootPath ) );
        assertTrue( Files.isDirectory( defaultPath ) );
        assertEquals( "/", rootPath.toString() );
    }

    @Test
    public void testGetPath() {
        FileSystem fileSystem = FileSystems.getFileSystem( uri );
        assertTrue( UnixSshFileSystem.class.isAssignableFrom( fileSystem.getClass() ) );
        assertEquals( "/", fileSystem.getPath( "/" ).toString() );
        assertEquals( "/", fileSystem.getPath( "/" ).toAbsolutePath().toString() );
        assertEquals( "aaa", fileSystem.getPath( "aaa" ).toString() );
        assertEquals( sshPath + "/aaa", fileSystem.getPath( "aaa" ).toAbsolutePath().toString() );
        assertEquals( "/aaa", fileSystem.getPath( "/aaa" ).toString() );
        assertEquals( "/aaa", fileSystem.getPath( "/aaa" ).toAbsolutePath().toString() );
    }

    @Test
    public void testNewDirectoryStream() {
        final String root = UUID.randomUUID().toString();
        final String filename1 = "silly1.txt";
        final String filename2 = "silly2.txt";
        final String filename3 = "silly3.txt";
        final String filename4 = "silly4.txt";
        final String dotfilename = ".sillly.txt";

        File rootDir = new File( filesystemPath, root );
        File file1 = new File( rootDir, filename1 );
        File file2 = new File( rootDir, filename2 );
        File file3 = new File( rootDir, filename3 );
        File file4 = new File( rootDir, filename4 );
        File dotfile = new File( rootDir, dotfilename );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file1, expected, UTF8 );
            IOUtils.writeFile( file2, expected, UTF8 );
            IOUtils.writeFile( file3, expected, UTF8 );
            IOUtils.writeFile( file4, expected, UTF8 );
            IOUtils.writeFile( dotfile, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write files to {}: {}", rootDir, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write files to " + rootDir + ": " + e.getMessage() );
        }

        Path rootPath = FileSystems.getFileSystem( uri ).getPath( root );
        Set<String> expectedEntries = new HashSet<String>();
        expectedEntries.add( rootPath.resolve( filename1 ).toString() );
        expectedEntries.add( rootPath.resolve( filename2 ).toString() );
        expectedEntries.add( rootPath.resolve( filename3 ).toString() );
        expectedEntries.add( rootPath.resolve( filename4 ).toString() );
        expectedEntries.add( rootPath.resolve( dotfilename ).toString() );

        try {
            DirectoryStream<Path> directoryStream = null;
            try {
                directoryStream = rootPath.getFileSystem().provider().newDirectoryStream( rootPath, new Filter<Path>() {
                    @Override
                    public boolean accept( Path path ) throws IOException {
                        if ( path.getFileName().toString().equals( filename1 ) ) {
                            return false;
                        }
                        else {
                            return true;
                        }
                    }
                } );
                for ( Path directoryEntry : directoryStream ) {
                    assertTrue( expectedEntries.remove( directoryEntry.toString() ) );
                }
                assertTrue( expectedEntries.remove( rootPath.resolve( filename1 ).toString() ) );
                assertTrue( expectedEntries.isEmpty() );
            }
            finally {
                IOUtils.closeAndLogException( directoryStream );
            }
        }
        catch ( IOException e ) {
            logger.error( "could not obtain directory stream from {}: {}", rootPath, e );
            logger.debug( "could not obtain directory stream:", e );
            fail( "could not obtain directory stream from " + rootPath + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file1, file1, file3, file4, dotfile, rootDir );
        }
    }

    @Test
    public void testNewInputStream() {
        String root = UUID.randomUUID().toString();
        String filename = "outputstreamtest.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        Path filePath = FileSystems.getFileSystem( uri ).getPath( root ).resolve( filename );
        try {
            rootDir.mkdirs();

            IOUtils.writeFile( file, expected );

            InputStream inputStream = null;
            try {
                inputStream = filePath.getFileSystem().provider().newInputStream( filePath );
                assertEquals( expected, IOUtils.copyToString( inputStream ) );
            }
            finally {
                IOUtils.closeAndLogException( inputStream );
            }
        }
        catch ( IOException e ) {
            logger.error( "failed for {}: {}", filePath, e );
            logger.debug( "failed:", e );
            fail( "failed for " + filePath + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }

    @Test
    public void testNewOutputStream() {
        String root = UUID.randomUUID().toString();
        String filename = "outputstreamtest.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        Path path = FileSystems.getFileSystem( uri ).getPath( root ).resolve( filename );
        try {
            logger.debug( "making dir {}", rootDir );
            rootDir.mkdirs();

            OutputStream outputStream = null;
            try {
                logger.trace( "getting outputstream" );
                outputStream = path.getFileSystem().provider().newOutputStream( path );
                logger.trace( "writing to outputstream" );
                IOUtils.copyFromString( expected, outputStream );
                logger.trace( "writing complete" );
            }
            finally {
                IOUtils.closeAndLogException( outputStream );
            }

            logger.trace( "checking file contents" );
            assertEquals( expected, IOUtils.readFile( file, UTF8 ) );
            logger.trace( "file contents match" );
        }
        catch ( IOException e ) {
            logger.error( "failed for {}: {}", path, e );
            logger.debug( "failed:", e );
            fail( "failed for " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }

    @Test
    public void testPosixFileAttributes() {
        String root = UUID.randomUUID().toString();
        String filename = "silly.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write to {}: {}", file, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write to " + file + ": " + e.getMessage() );
        }

        Path path = FileSystems.getFileSystem( uri ).getPath( root ).resolve( filename );
        try {
            long now = new Date().getTime();
            PosixFileAttributes attributes = path.getFileSystem().provider().readAttributes( path, PosixFileAttributes.class );

            assertTrue( now > attributes.creationTime().toMillis() );
            assertTrue( now > attributes.lastAccessTime().toMillis() );
            assertTrue( now > attributes.lastModifiedTime().toMillis() );
            assertTrue( attributes.isRegularFile() );
            assertFalse( attributes.isDirectory() );
            assertFalse( attributes.isSymbolicLink() );
            assertFalse( attributes.isOther() );
            assertEquals( expected.length(), attributes.size() );
            assertNotNull( attributes.fileKey() );
        }
        catch ( IOException e ) {
            logger.error( "could not read attribues from {}: {}", path, e );
            logger.debug( "could not read attributes:", e );
            fail( "could not read attributes from " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }

    @Test
    public void testReadAttributes() {
        String root = UUID.randomUUID().toString();
        String filename = "silly.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write to {}: {}", file, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write to " + file + ": " + e.getMessage() );
        }

        Path path = FileSystems.getFileSystem( uri ).getPath( root ).resolve( filename );
        try {
            long now = new Date().getTime();
            Map<String, Object> map = path.getFileSystem().provider().readAttributes( path, "creationTime,size,fileKey" );

            assertTrue( now > ((FileTime) map.get( "creationTime" )).toMillis() );
            assertEquals( Long.valueOf( expected.length() ), (Long) map.get( "size" ) );
            assertNotNull( map.get( "fileKey" ) );
        }
        catch ( IOException e ) {
            logger.error( "could not read attribues from {}: {}", path, e );
            logger.debug( "could not read attributes:", e );
            fail( "could not read attributes from " + path + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        	try {
        		// should add wait to delete files to ensure delete is complete... but...
				Thread.sleep(1000);
			} catch (InterruptedException e) { }
        }

        try {
            // should throw exception as file no longer exists
            path.getFileSystem().provider().readAttributes( path, "creationTime,size,fileKey" );
            fail( "NoSuchFileException should have been thrown" );
        }
        catch ( NoSuchFileException e ) {
            logger.debug( "as expected, [{}] does not exist", e.getFile() );
        }
        catch ( IOException e ) {
            logger.error( "could not read attribues from {}: {}", path, e );
            logger.debug( "could not read attributes:", e );
            fail( "could not read attributes from " + path + ": " + e.getMessage() );
        }

        try {
            // should throw exception as dir no longer exists
            path.getFileSystem().provider().readAttributes(
                    FileSystems.getFileSystem( uri ).getPath( root ),
                    "creationTime,size,fileKey" );
            fail( "NoSuchFileException should have been thrown" );
        }
        catch ( NoSuchFileException e ) {
            logger.debug( "as expected, [{}] does not exist", e.getFile() );
        }
        catch ( IOException e ) {
            logger.error( "could not read attribues from {}: {}", path, e );
            logger.debug( "could not read attributes:", e );
            fail( "could not read attributes from " + path + ": " + e.getMessage() );
        }
    }

    @Test
    public void testRelativize() {
        FileSystem fileSystem = FileSystems.getFileSystem( uri );
        Path path = fileSystem.getPath( "this/is/a/test" );
        Path other = fileSystem.getPath( "this/is/a/test/of/the/emergency/broadcast/system" );
        Path crazy = fileSystem.getPath( "this/is/b/test/of/the" );
        Path expectedRelative = fileSystem.getPath( "of/the/emergency/broadcast/system" );
        Path expectedInverseRelative = fileSystem.getPath( "../../../../.." );
        Path expectedCrazyRelative = fileSystem.getPath( "../../b/test/of/the" );
        Path expectedOtherCrazyRelative = fileSystem.getPath( "../../../../../../../b/test/of/the" );
        assertEquals( expectedRelative, path.relativize( other ) );
        assertEquals( expectedInverseRelative, other.relativize( path ) );
        assertEquals( expectedCrazyRelative, path.relativize( crazy ) );
        assertEquals( expectedOtherCrazyRelative, other.relativize( crazy ) );
    }

    @Test
    public void testSeekableByteChannel() {
        String root = UUID.randomUUID().toString();
        String filename = "outputstreamtest.txt";

        File rootDir = new File( filesystemPath, root );
        File file = new File( rootDir, filename );
        Path filePath = FileSystems.getFileSystem( uri ).getPath( root ).resolve( filename );
        try {
            rootDir.mkdirs();

            IOUtils.writeFile( file, expected, UTF8 );

            try (SeekableByteChannel byteChannel = filePath.getFileSystem().provider().newByteChannel(
                    filePath,
                    EnumSet.of(
                            StandardOpenOption.READ,
                            StandardOpenOption.WRITE ),
                    PosixFilePermissions.asFileAttribute( EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE ) ) )) {

                byte[] bytes = new byte[4];
                ByteBuffer buffer = ByteBuffer.wrap( bytes );
                byteChannel.position( 3 ).read( buffer );

                String threeToSeven = expected.substring( 3, 7 );
                assertEquals( threeToSeven, new String( bytes, UTF8 ) );

                buffer.position( 0 );
                assertEquals( 4, byteChannel.position( 10 ).write( buffer ) );

                String newExpected = expected.substring( 0, 10 ) + threeToSeven + expected.substring( 14 );
                bytes = new byte[expected.getBytes( UTF8 ).length];
                buffer = ByteBuffer.wrap( bytes );
                byteChannel.position( 0 ).read( buffer );
                assertEquals( newExpected, new String( bytes, UTF8 ) );
            }
        }
        catch ( IOException e ) {
            logger.error( "failed for {}: {}", filePath, e );
            logger.debug( "failed:", e );
            fail( "failed for " + filePath + ": " + e.getMessage() );
        }
        finally {
            IOUtils.deleteFiles( file, rootDir );
        }
    }

    @Test
    public void testStatDirectory() {
        final String root = UUID.randomUUID().toString();
        final String filename1 = "silly1.txt";
        final String filename2 = "silly2.txt";
        final String filename3 = "silly3.txt";
        final String filename4 = "silly4.txt";
        final String dotfilename = ".silly.txt";

        File rootDir = new File( filesystemPath, root );
        File file1 = new File( rootDir, filename1 );
        File file2 = new File( rootDir, filename2 );
        File file3 = new File( rootDir, filename3 );
        File file4 = new File( rootDir, filename4 );
        File dotfile = new File( rootDir, dotfilename );
        try {
            rootDir.mkdirs();
            IOUtils.writeFile( file1, expected, UTF8 );
            IOUtils.writeFile( file2, expected, UTF8 );
            IOUtils.writeFile( file3, expected, UTF8 );
            IOUtils.writeFile( file4, expected, UTF8 );
            IOUtils.writeFile( dotfile, expected, UTF8 );
        }
        catch ( IOException e ) {
            logger.error( "could not write files to {}: {}", rootDir, e );
            logger.debug( "could not write to file:", e );
            fail( "could not write files to " + rootDir + ": " + e.getMessage() );
        }

        UnixSshPath rootPath = (UnixSshPath) FileSystems.getFileSystem( uri ).getPath( root );
        try {
            Map<UnixSshPath, PosixFileAttributes> map = rootPath.getFileSystem().provider().statDirectory( rootPath );
            assertEquals( 5, map.size() );
            assertTrue( map.containsKey( FileSystems.getFileSystem( uri ).getPath( filename1 ) ) );
            assertTrue( map.containsKey( FileSystems.getFileSystem( uri ).getPath( filename2 ) ) );
            assertTrue( map.containsKey( FileSystems.getFileSystem( uri ).getPath( filename3 ) ) );
            assertTrue( map.containsKey( FileSystems.getFileSystem( uri ).getPath( filename4 ) ) );
            assertTrue( map.containsKey( FileSystems.getFileSystem( uri ).getPath( dotfilename ) ) );
        }
        catch ( IOException e ) {
            logger.error( "could not stat directory {}: {}", rootDir, e );
            logger.debug( "could not stat directory:", e );
            fail( "could not stat directory " + rootDir + ": " + e.getMessage() );
        }
    }

    @Test
    public void testUri() {
        String filename = "silly.txt";
        Path tempPath = Paths.get( uri );
        UnixSshPath path = (UnixSshPath) tempPath.resolve( filename );
        assertEquals( username, path.getUsername() );
        assertEquals( hostname, path.getHostname() );
        assertEquals( port, path.getPort() );
        assertEquals( sshPath + PATH_SEPARATOR + filename, path.toString() );
    }
}
