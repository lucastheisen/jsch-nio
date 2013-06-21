package com.pastdev.jsch.nio.file.spi;


import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionFactory.SessionFactoryBuilder;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.command.CommandRunner.ExecuteResult;
import com.pastdev.jsch.file.SshPath;
import com.pastdev.jsch.file.attribute.PosixFileAttributes;
import com.pastdev.jsch.nio.file.UnixSshFileSystem;
import com.pastdev.jsch.nio.file.UnixSshPath;


public class UnixSshFileSystemProvider extends AbstractSshFileSystemProvider {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemProvider.class );
    private static final String ASCII_UNIT_SEPARATOR = Character.toString( (char)31 );
    public static final char PATH_SEPARATOR = '/';
    public static final String PATH_SEPARATOR_STRING = "/";

    private Map<URI, UnixSshFileSystem> fileSystemMap;

    public UnixSshFileSystemProvider() {
        this.fileSystemMap = new HashMap<URI, UnixSshFileSystem>();
    }

    public UnixSshPath checkPath( Path path ) {
        if ( path == null ) {
            throw new NullPointerException();
        }
        if ( !(path instanceof UnixSshPath) ) {
            throw new IllegalArgumentException( "path not an instanceof UnixSshPath" );
        }
        return (UnixSshPath)path;
    }

    @Override
    public void checkAccess( Path arg0, AccessMode... arg1 ) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy( Path arg0, Path arg1, CopyOption... arg2 ) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void createDirectory( Path arg0, FileAttribute<?>... arg1 ) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete( Path arg0 ) throws IOException {
        // TODO Auto-generated method stub

    }

    private boolean exists( Path path ) {
        // TODO implement me
        return false;
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView( Path arg0, Class<V> arg1, LinkOption... arg2 ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileStore getFileStore( Path arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileSystem getFileSystem( URI uri ) {
        UnixSshFileSystem fileSystem = fileSystemMap.get( uri.resolve( PATH_SEPARATOR_STRING ) );
        if ( fileSystem == null ) {
            throw new RuntimeException( "no filesystem defined for " + uri.toString() );
        }
        return fileSystem;
    }

    @Override
    public Path getPath( URI uri ) {
        return getFileSystem( uri ).getPath( uri.getPath() );
    }

    @Override
    public String getScheme() {
        return "ssh.unix";
    }

    @Override
    public boolean isHidden( Path arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSameFile( Path arg0, Path arg1 ) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void move( Path arg0, Path arg1, CopyOption... arg2 ) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public SeekableByteChannel newByteChannel( Path arg0, Set<? extends OpenOption> arg1, FileAttribute<?>... arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( Path path, Filter<? super Path> filter ) throws IOException {
        UnixSshPath unixPath = checkPath( path );
        CommandRunner commandRunner = ((UnixSshFileSystem)unixPath.getFileSystem()).getCommandRunner();
        try {
            ExecuteResult result = commandRunner.execute( "ls -1 " + path.toString() );
            if ( result.getExitCode() == 0 ) {
                return new ArrayEntryDirectoryStream(
                        path, result.getStdout().split( "\n" ) );
            }
            else {
                throw new IOException( "failed to list directory (" + result.getExitCode() + "): " +
                        "out='" + result.getStderr() + "', " +
                        "err='" + result.getStderr() + "'" );
            }
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    @Override
    public FileSystem newFileSystem( URI uri, Map<String, ?> environment ) throws IOException {
        URI baseUri = uri.resolve( PATH_SEPARATOR_STRING );
        UnixSshFileSystem existing = fileSystemMap.get( baseUri );
        if ( existing != null ) {
            throw new RuntimeException( "filesystem already exists for " + uri.toString() + " at " + existing.toString() );
        }
        try {
            // Construct a new sessionFactory from the URI authority, path, and
            // optional environment proxy
            SessionFactory defaultSessionFactory = (SessionFactory)environment.get( "defaultSessionFactory" );
            if ( defaultSessionFactory == null ) {
                throw new IllegalArgumentException( "defaultSessionFactory environment parameter is required" );
            }
            SessionFactoryBuilder builder = defaultSessionFactory.newSessionFactoryBuilder();
            String username = uri.getUserInfo();
            if ( username != null ) {
                builder.setUsername( username );
            }
            String hostname = uri.getHost();
            if ( hostname != null ) {
                builder.setHostname( hostname );
            }
            int port = uri.getPort();
            if ( port != -1 ) {
                builder.setPort( port );
            }
            Proxy proxy = (Proxy)environment.get( "proxy" );
            if ( proxy != null ) {
                builder.setProxy( proxy );
            }

            UnixSshFileSystem fileSystem = new UnixSshFileSystem(
                    this, uri, new CommandRunner( builder.build() ) );
            fileSystemMap.put( baseUri, fileSystem );
            return fileSystem;
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes( Path arg0, Class<A> arg1, LinkOption... arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> readAttributes( Path arg0, String arg1, LinkOption... arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttribute( Path arg0, String arg1, Object arg2, LinkOption... arg3 ) throws IOException {
        // TODO Auto-generated method stub

    }

    private enum SupportedAttribute {
        creationTime("%W", Date.class),
        group("%G", String.class),
        fileKey("%i", Long.TYPE),
        lastAccessTime("%X", Date.class),
        lastModifiedTime("%Y", Date.class),
        lastChangedTime("%Z", Date.class),
        name("%n", String.class),
        owner("%U", String.class),
        permissions("%a", String.class),
        size("%s", Long.TYPE),
        isRegularFile("%F", Boolean.TYPE),
        isDirectory("%F", Boolean.TYPE),
        isSymbolicLink("%F", Boolean.TYPE),
        isOther("%F", Boolean.TYPE);

        private static Map<String, SupportedAttribute> lookup;

        static {
            lookup = new HashMap<String, SupportedAttribute>();
            for ( SupportedAttribute attribute : values() ) {
                lookup.put( attribute.name(), attribute );
            }
        }

        private String option;
        private Class<?> valueClass;

        private SupportedAttribute( String option, Class<?> valueClass ) {
            this.option = option;
            this.valueClass = valueClass;
        }

        public static SupportedAttribute fromString( String attribute ) {
            return lookup.get( attribute );
        }

        public String option() {
            return option;
        }

        public Object toObject( String value ) {
            if ( this == isRegularFile ) {
                return "regular file".equals( value.toLowerCase() );
            }
            if ( this == isDirectory ) {
                return "directory".equals( value.toLowerCase() );
            }
            if ( this == isSymbolicLink ) {
                return "symbolic link".equals( value.toLowerCase() );
            }
            if ( this == isOther ) {
                return "other".equals( value.toLowerCase() );
            }
            if ( valueClass == Long.TYPE ) {
                return Long.parseLong( value );
            }
            if ( valueClass == Date.class ) {
                return new Date( Long.parseLong( value ) * 1000 );
            }
            else {
                return value;
            }
        }
    }

    private static final SupportedAttribute[] BASIC_SUPPORTED_ATTRIBUTES = new SupportedAttribute[] {
            SupportedAttribute.creationTime,
            SupportedAttribute.fileKey,
            SupportedAttribute.isDirectory,
            SupportedAttribute.isRegularFile,
            SupportedAttribute.isSymbolicLink,
            SupportedAttribute.isOther,
            SupportedAttribute.lastAccessTime,
            SupportedAttribute.lastModifiedTime,
            SupportedAttribute.size };
    private static final SupportedAttribute[] POSIX_ADDITIONAL_SUPPORTED_ATTRIBUTES = new SupportedAttribute[] {
            SupportedAttribute.permissions,
            SupportedAttribute.owner,
            SupportedAttribute.group };

    private class BasicFileAttributesImpl implements BasicFileAttributes {
        protected Map<String, Object> map;

        public BasicFileAttributesImpl( Path path ) throws IOException {
            this( path, null );
        }

        protected BasicFileAttributesImpl( Path path, SupportedAttribute[] additionalAttributes ) throws IOException {
            SupportedAttribute[] supportedAttributes = null;
            if ( additionalAttributes == null ) {
                supportedAttributes = BASIC_SUPPORTED_ATTRIBUTES;
            }
            else {
                supportedAttributes = new SupportedAttribute[BASIC_SUPPORTED_ATTRIBUTES.length + additionalAttributes.length];
                System.arraycopy( BASIC_SUPPORTED_ATTRIBUTES, 0, supportedAttributes, 0, BASIC_SUPPORTED_ATTRIBUTES.length );
                System.arraycopy( additionalAttributes, 0, supportedAttributes, BASIC_SUPPORTED_ATTRIBUTES.length, additionalAttributes.length );
            }
            map = readAttributes( path, supportedAttributes );
        }

        public Date creationTime() {
            return (Date)map.get( SupportedAttribute.creationTime.toString() );
        }

        public Object fileKey() {
            return map.get( SupportedAttribute.fileKey.toString() );
        }

        public boolean isDirectory() {
            return (Boolean)map.get( SupportedAttribute.isDirectory.toString() );
        }

        public boolean isOther() {
            return (Boolean)map.get( SupportedAttribute.isOther.toString() );
        }

        public boolean isRegularFile() {
            return (Boolean)map.get( SupportedAttribute.isRegularFile.toString() );
        }

        public boolean isSymbolicLink() {
            return (Boolean)map.get( SupportedAttribute.isSymbolicLink.toString() );
        }

        public Date lastAccessTime() {
            return (Date)map.get( SupportedAttribute.lastAccessTime.toString() );
        }

        public Date lastModifiedTime() {
            return (Date)map.get( SupportedAttribute.lastModifiedTime.toString() );
        }

        public long size() {
            return (Long)map.get( SupportedAttribute.size.toString() );
        }
    }

    private class PosixFileAttributesImpl extends BasicFileAttributesImpl implements PosixFileAttributes {
        public PosixFileAttributesImpl( SshPath path ) throws IOException {
            super( path, POSIX_ADDITIONAL_SUPPORTED_ATTRIBUTES );
        }

        public String group() {
            return (String)map.get( SupportedAttribute.group.toString() );
        }

        public String owner() {
            return (String)map.get( SupportedAttribute.owner.toString() );
        }

        public String permissions() {
            return (String)map.get( SupportedAttribute.permissions.toString() );
        }
    }
}
