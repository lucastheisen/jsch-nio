package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.command.CommandRunner.ChannelExecWrapper;
import com.pastdev.jsch.command.CommandRunner.ExecuteResult;


public class UnixSshFileSystemProvider extends AbstractSshFileSystemProvider {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemProvider.class );
    private static final String ASCII_UNIT_SEPARATOR = Character.toString( (char)31 );
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
    public static final char PATH_SEPARATOR = '/';
    public static final String PATH_SEPARATOR_STRING = "/";
    private static final SupportedAttribute[] POSIX_ADDITIONAL_SUPPORTED_ATTRIBUTES = new SupportedAttribute[] {
            SupportedAttribute.permissions,
            SupportedAttribute.owner,
            SupportedAttribute.group };
    private static final SimpleDateFormat TOUCH_DATE_FORMAT = new SimpleDateFormat( "yyyyMMddHHmm.ss" );

    private Map<URI, UnixSshFileSystem> fileSystemMap;

    public UnixSshFileSystemProvider() {
        this.fileSystemMap = new HashMap<URI, UnixSshFileSystem>();
    }

    UnixSshPath checkPath( Path path ) {
        if ( path == null ) {
            throw new NullPointerException();
        }
        if ( !(path instanceof UnixSshPath) ) {
            throw new IllegalArgumentException( "path not an instanceof UnixSshPath" );
        }
        return (UnixSshPath)path;
    }

    @Override
    public void checkAccess( Path path, AccessMode... modes ) throws IOException {
        UnixSshPath unixPath = checkPath( path ).toAbsolutePath();
        String pathString = unixPath.toString();

        ExecuteResult result = execute( unixPath, "test -e " + pathString );
        if ( result.getExitCode() != 0 ) {
            throw new NoSuchFileException( pathString );
        }

        Set<AccessMode> modesSet = toSet( modes );
        if ( modesSet.contains( AccessMode.READ ) ) {
            result = execute( unixPath, "test -r " + pathString );
            if ( result.getExitCode() != 0 ) {
                throw new AccessDeniedException( pathString );
            }
        }
        if ( modesSet.contains( AccessMode.WRITE ) ) {
            result = execute( unixPath, "test -w " + pathString );
            if ( result.getExitCode() != 0 ) {
                throw new AccessDeniedException( pathString );
            }
        }
        if ( modesSet.contains( AccessMode.EXECUTE ) ) {
            result = execute( unixPath, "test -x " + pathString );
            if ( result.getExitCode() != 0 ) {
                throw new AccessDeniedException( pathString );
            }
        }
    }

    @Override
    public void copy( Path from, Path to, CopyOption... copyOptions ) throws IOException {
        copyOrMove( "cp", from, to, copyOptions );
    }

    public void copyOrMove( String cpOrMv, Path from, Path to, CopyOption... copyOptions ) throws IOException {
        UnixSshPath unixFrom = checkPath( from );
        UnixSshPath unixTo = checkPath( to );

        Set<CopyOption> options = toSet( copyOptions );
        if ( options.contains( StandardCopyOption.ATOMIC_MOVE ) ) {
            throw new AtomicMoveNotSupportedException( from.toString(), to.toString(),
                    "to complicated to think about right now, try again at a later release." );
        }

        BasicFileAttributesImpl fromAttributes = new BasicFileAttributesImpl( unixFrom );

        if ( exists( unixTo ) ) {
            PosixFileAttributesImpl toAttributes = new PosixFileAttributesImpl( unixTo );
            if ( fromAttributes.isSameFile( toAttributes ) ) return;
            if ( options.contains( StandardCopyOption.REPLACE_EXISTING ) ) {
                delete( unixTo, toAttributes );
            }
            else {
                throw new FileAlreadyExistsException( to.toString() );
            }
        }

        String command = cpOrMv + " " + unixFrom.toString() + " " + unixTo.toString();
        ExecuteResult result = execute( unixTo, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to " + command
                    + " (" + result.getExitCode() + "): " + result.getStderr() );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createDirectory( Path path, FileAttribute<?>... fileAttributes ) throws IOException {
        UnixSshPath unixPath = checkPath( path );
        Set<PosixFilePermission> permissions = null;
        for ( FileAttribute<?> fileAttribute : fileAttributes ) {
            if ( fileAttribute.name().equals( "posix:permissions" ) ) {
                permissions = (Set<PosixFilePermission>)fileAttribute.value();
            }
        }

        StringBuilder commandBuilder = new StringBuilder( "mkdir " );
        if ( permissions != null ) {
            commandBuilder.append( "-m " ).append( toMode( permissions ) );
        }
        commandBuilder.append( unixPath.toString() );
        ExecuteResult result = execute( unixPath, commandBuilder.toString() );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to " + commandBuilder.toString()
                    + " (" + result.getExitCode() + "): " + result.getStderr() );
        }
    }

    @SuppressWarnings("unchecked")
    PosixFileAttributes createFile( UnixSshPath path, FileAttribute<?>... fileAttributes ) throws IOException {
        Set<PosixFilePermission> permissions = null;
        UserPrincipal owner = null;
        GroupPrincipal group = null;
        for ( FileAttribute<?> fileAttribute : fileAttributes ) {
            String name = fileAttribute.name();
            if ( name.equals( "posix:permissions" ) ) {
                permissions = (Set<PosixFilePermission>)fileAttribute.value();
            }
            else if ( name.equals( "posix:owner" ) ) {
                owner = (UserPrincipal)fileAttribute.value();
            }
            else if ( name.equals( "posix:group" ) ) {
                group = (GroupPrincipal)fileAttribute.value();
            }
        }

        String command = "touch " + path.toString();
        ExecuteResult result = execute( path, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to " + command
                    + " (" + result.getExitCode() + "): " + result.getStderr() );
        }

        if ( permissions != null ) {
            setPermissions( path, permissions );
        }
        if ( owner != null ) {
            setOwner( path, owner );
        }
        if ( group != null ) {
            setGroup( path, group );
        }

        return readAttributes( path, PosixFileAttributes.class );
    }

    @Override
    public void delete( Path path ) throws IOException {
        delete( checkPath( path ), new BasicFileAttributesImpl( path ) );
    }

    private void delete( UnixSshPath path, BasicFileAttributes attributes ) throws IOException {
        if ( attributes.isDirectory() ) {
            ExecuteResult result = execute( path, "rmdir " + path.toString() );
            if ( result.getExitCode() != 0 ) {
                throw new DirectoryNotEmptyException( path.toString() );
            }
        }
        else {
            ExecuteResult result = execute( path, "unlink " + path.toString() );
            if ( result.getExitCode() != 0 ) {
                throw new IOException( "failed to remove existing destination file "
                        + result.getExitCode() + ": " + path.toString() );
            }
        }
    }

    private ExecuteResult execute( UnixSshPath path, String command ) throws IOException {
        CommandRunner commandRunner = path.getFileSystem().getCommandRunner();
        try {
            return commandRunner.execute( command );
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    private boolean exists( Path path ) throws IOException {
        try {
            checkAccess( path );
            return true;
        }
        catch ( NoSuchFileException e ) {
            return false;
        }
    }

    UnixSshBasicFileAttributeView getFileAttributeView( Path path, String viewName, LinkOption... linkOptions ) {
        if ( viewName.equals( "basic" ) ) {
            return new UnixSshBasicFileAttributeView( checkPath( path ), linkOptions );
        }
        else if ( viewName.equals( "posix" ) ) {
            return new UnixSshPosixFileAttributeView( checkPath( path ), linkOptions );
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView( Path path, Class<V> type, LinkOption... linkOptions ) {
        if ( type == BasicFileAttributeView.class ) {
            return (V)getFileAttributeView( path, "basic", linkOptions );
        }
        if ( type == PosixFileAttributeView.class ) {
            return (V)getFileAttributeView( path, "posix", linkOptions );
        }
        if ( type == null ) {
            throw new NullPointerException();
        }
        return (V)null;
    }

    @Override
    public FileStore getFileStore( Path arg0 ) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException( "no idea what a file store would mean in this context, so for now, you have to deal with this exception" );
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
    public String getScheme() {
        return "ssh.unix";
    }

    @Override
    public boolean isHidden( Path path ) throws IOException {
        return checkPath( path ).getFileNameString().startsWith( "." );
    }

    @Override
    public boolean isSameFile( Path path1, Path path2 ) throws IOException {
        if ( path1.equals( path2 ) ) {
            return true;
        }
        else if ( !isSameProvider( path1, path2 ) ) {
            return false;
        }
        return new BasicFileAttributesImpl( path1 ).isSameFile(
                new BasicFileAttributesImpl( path2 ) );
    }

    private boolean isSameProvider( Path path1, Path path2 ) {
        return path1.getFileSystem().provider().equals( path2.getFileSystem().provider() );
    }

    @Override
    public void move( Path from, Path to, CopyOption... copyOptions ) throws IOException {
        copyOrMove( "mv", from, to, copyOptions );
    }

    @Override
    public SeekableByteChannel newByteChannel( Path path, Set<? extends OpenOption> openOptions, FileAttribute<?>... fileAttributes ) throws IOException {
        return new UnixSshSeekableByteChannel( checkPath( path ), openOptions, fileAttributes );
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream( Path path, Filter<? super Path> filter ) throws IOException {
        UnixSshPath unixPath = checkPath( path );
        ExecuteResult result = execute( unixPath, "ls -1 " + unixPath.toAbsolutePath().toString() );
        if ( result.getExitCode() == 0 ) {
            return new StandardDirectoryStream(
                    path, result.getStdout().split( "\n" ), filter );
        }
        else {
            throw new IOException( "failed to list directory (" + result.getExitCode() + "): " +
                    "out='" + result.getStderr() + "', " +
                    "err='" + result.getStderr() + "'" );
        }
    }

    @Override
    public FileSystem newFileSystem( URI uri, Map<String, ?> environment ) throws IOException {
        URI baseUri = uri.resolve( PATH_SEPARATOR_STRING );
        UnixSshFileSystem existing = fileSystemMap.get( baseUri );
        if ( existing != null ) {
            throw new RuntimeException( "filesystem already exists for " + uri.toString() + " at " + existing.toString() );
        }

        UnixSshFileSystem fileSystem = new UnixSshFileSystem( this, uri, environment );
        fileSystemMap.put( baseUri, fileSystem );
        return fileSystem;
    }

    @Override
    public InputStream newInputStream( Path path, OpenOption... openOptions ) throws IOException {
        UnixSshPath unixPath = checkPath( path ).toAbsolutePath();
        try {
            final ChannelExecWrapper channel = unixPath.getFileSystem()
                    .getCommandRunner().open( "cat " + unixPath.toString() );
            return new InputStream() {
                private InputStream inputStream = channel.getInputStream();

                @Override
                public void close() throws IOException {
                    int exitCode = channel.close();
                    logger.debug( "cat exited with {}", exitCode );
                }

                @Override
                public int read() throws IOException {
                    return inputStream.read();
                }
            };
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    @Override
    public OutputStream newOutputStream( Path path, OpenOption... openOptions ) throws IOException {
        UnixSshPath unixPath = checkPath( path ).toAbsolutePath();
        Set<OpenOption> options = null;
        if ( openOptions == null || openOptions.length == 0 ) {
            options = new HashSet<OpenOption>( Arrays.asList( new StandardOpenOption[] {
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            } ) );
            logger.debug( "no open options specified, so using CREATE, TRUNCATE_EXISTING, and WRITE" );
        }
        else {
            options = new HashSet<OpenOption>( Arrays.asList( openOptions ) );
        }

        if ( options.contains( StandardOpenOption.READ ) ) {
            throw new IllegalArgumentException( "read not allowed on OutputStream, seriously..." );
        }
        if ( !options.contains( StandardOpenOption.WRITE ) ) {
            throw new IllegalArgumentException( "what good is an OutputStream that you cant write to?" );
        }
        if ( options.contains( StandardOpenOption.DELETE_ON_CLOSE ) ) {
            throw new UnsupportedOperationException( "not gonna implement" );
        }
        // dd has options for SYNC, DSYNC and SPARSE maybe...

        try {
            checkAccess( unixPath );
            if ( options.contains( StandardOpenOption.CREATE_NEW ) ) {
                throw new FileAlreadyExistsException( unixPath.toString() );
            }
        }
        catch ( NoSuchFileException e ) {
            if ( options.contains( StandardOpenOption.CREATE_NEW ) ) {
                // this is as close to atomic create as i can get...
                // TODO convert this to use `dd of=file conv=excl` and write 0
                // bytes which will make the check for exists and create atomic
                createFile( unixPath );
            }
            else if ( !options.contains( StandardOpenOption.CREATE ) ) {
                throw e;
            }
        }

        try {

            StringBuilder commandBuilder = new StringBuilder( "cat " );
            if ( options.contains( StandardOpenOption.APPEND )
                    && !options.contains( StandardOpenOption.TRUNCATE_EXISTING ) ) {
                commandBuilder.append( ">> " );
            }
            else {
                commandBuilder.append( "> " );
            }
            commandBuilder.append( unixPath.toString() );

            final ChannelExecWrapper channel = unixPath.getFileSystem()
                    .getCommandRunner().open( commandBuilder.toString() );
            return new OutputStream() {
                private OutputStream outputStream = channel.getOutputStream();

                @Override
                public void close() throws IOException {
                    int exitCode = channel.close();
                    logger.debug( "cat exited with {}", exitCode );
                }

                @Override
                public void write( int b ) throws IOException {
                    outputStream.write( b );
                }
            };
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    int read( UnixSshPath path, long startIndex, ByteBuffer bytes ) throws IOException {
        try {
            int read = 0;
            ChannelExecWrapper sshChannel = path.getFileSystem().getCommandRunner().open( "dd bs=1 skip=" + startIndex + " if=" + path.toString() );
            try (InputStream in = sshChannel.getInputStream()) {
                ReadableByteChannel inChannel = Channels.newChannel( in );
                read = inChannel.read( bytes );
            }
            return read;
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes( Path path, Class<A> type, LinkOption... linkOptions ) throws IOException {
        if ( type == BasicFileAttributes.class ) {
            return (A)new BasicFileAttributesImpl( path, linkOptions );
        }
        if ( type == PosixFileAttributes.class ) {
            return (A)new PosixFileAttributesImpl( path, linkOptions );
        }
        if ( type == null ) {
            throw new NullPointerException();
        }
        return (A)null;
    }

    @Override
    public Map<String, Object> readAttributes( Path path, String attributes, LinkOption... linkOptions ) throws IOException {
        List<SupportedAttribute> attributeList = new ArrayList<SupportedAttribute>();
        for ( String attributeName : attributes.split( "," ) ) {
            attributeName = attributeName.trim();
            if ( attributeName.equals( "*" ) ) {
                return readAttributes( path, SupportedAttribute.values() );
            }
            SupportedAttribute attribute = SupportedAttribute.fromString( attributeName );
            if ( attribute != null ) {
                attributeList.add( attribute );
            }
        }
        return readAttributes( path, attributeList.toArray( new SupportedAttribute[attributeList.size()] ), linkOptions );
    }

    private Map<String, Object> readAttributes( Path path, SupportedAttribute[] attributes, LinkOption... linkOptions ) throws IOException {
        UnixSshPath unixPath = checkPath( path ).toAbsolutePath();

        String command = statCommand( attributes ) + " " + unixPath.toString();
        ExecuteResult result = execute( unixPath, command );
        if ( result.getExitCode() == 0 ) {
            return statParse( result.getStdout(), attributes );
        }
        else {
            throw new IOException( "failed to list directory (" + result.getExitCode() + "): " +
                    "out='" + result.getStdout() + "', " +
                    "err='" + result.getStderr() + "'" );
        }
    }

    @Override
    public void setAttribute( Path path, String attribute, Object value, LinkOption... linkOptions ) throws IOException {
        String viewName = null;
        String attributeName = null;
        int colonIndex = attribute.indexOf( ':' );
        if ( colonIndex < 0 ) {
            viewName = "basic";
            attributeName = attribute;
        }
        else {
            viewName = attribute.substring( 0, colonIndex );
            attributeName = attribute.substring( colonIndex + 1 );
        }

        UnixSshBasicFileAttributeView view = getFileAttributeView( path, viewName, linkOptions );
        if ( view == null ) {
            throw new UnsupportedOperationException( "unsupported view " + viewName );
        }
        view.setAttribute( attributeName, value );
    }

    void setGroup( UnixSshPath path, GroupPrincipal group ) throws IOException {
        String command = "chgrp " + group.getName() + " " + path.toString();
        ExecuteResult result = execute( path, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to run " + command + " ("
                    + result.getExitCode() + "): " + result.getStderr() );
        }
    }

    void setOwner( UnixSshPath path, UserPrincipal owner ) throws IOException {
        String command = "chown " + owner.getName() + " " + path.toString();
        ExecuteResult result = execute( path, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to run " + command + " ("
                    + result.getExitCode() + "): " + result.getStderr() );
        }
    }

    void setPermissions( UnixSshPath path, Set<PosixFilePermission> permissions ) throws IOException {
        String command = "chmod " + toMode( permissions ) + " " + path.toString();
        ExecuteResult result = execute( path, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to run " + command + " ("
                    + result.getExitCode() + "): " + result.getStderr() );
        }
    }

    void setTimes( UnixSshPath path, FileTime lastModifiedTime, FileTime lastAccessTime ) throws IOException {
        if ( lastModifiedTime != null && lastModifiedTime.equals( lastAccessTime ) ) {
            String command = "touch -d " + toTouchTime( lastModifiedTime ) + " " + path.toString();
            ExecuteResult result = execute( path, command );
            if ( result.getExitCode() != 0 ) {
                throw new IOException( "failed to run " + command + " ("
                        + result.getExitCode() + "): " + result.getStderr() );
            }
            return;
        }

        if ( lastModifiedTime != null ) {
            String command = "touch -a -d " + toTouchTime( lastModifiedTime ) + " " + path.toString();
            ExecuteResult result = execute( path, command );
            if ( result.getExitCode() != 0 ) {
                throw new IOException( "failed to run " + command + " ("
                        + result.getExitCode() + "): " + result.getStderr() );
            }
        }
        if ( lastAccessTime != null ) {
            String command = "touch -a -d " + toTouchTime( lastModifiedTime ) + " " + path.toString();
            ExecuteResult result = execute( path, command );
            if ( result.getExitCode() != 0 ) {
                throw new IOException( "failed to run " + command + " ("
                        + result.getExitCode() + "): " + result.getStderr() );
            }
        }
    }

    private String statCommand( SupportedAttribute[] attributes ) {
        return statCommand( attributes, false );
    }

    private String statCommand( SupportedAttribute[] attributes, boolean newline ) {
        StringBuilder commandBuilder = new StringBuilder( "stat --printf \"" );
        for ( int i = 0; i < attributes.length; i++ ) {
            if ( i > 0 ) {
                commandBuilder.append( ASCII_UNIT_SEPARATOR );
            }
            commandBuilder.append( attributes[i].option() );
        }
        if ( newline ) {
            commandBuilder.append( "\\n" );
        }
        return commandBuilder.append( "\"" ).toString();
    }

    private Map<String, Object> statParse( String result, SupportedAttribute... attributes ) {
        String[] values = result.split( ASCII_UNIT_SEPARATOR );
        Map<String, Object> map = new HashMap<String, Object>();
        int index = 0;
        for ( SupportedAttribute attribute : attributes ) {
            map.put( attribute.name(), attribute.toObject( values[index++] ) );
        }
        return map;
    }

    Map<UnixSshPath, PosixFileAttributes> statDirectory( UnixSshPath directoryPath ) throws IOException {
        Map<UnixSshPath, PosixFileAttributes> map = new HashMap<>();
        SupportedAttribute[] allAttributes = SupportedAttribute.values();
        String command = directoryPath.getFileSystem().getCommand( "find" ) + " "
                + directoryPath.toAbsolutePath().toString()
                + " -maxdepth 1 -type f -exec " + statCommand( allAttributes, true ) + " {} +";

        ExecuteResult result = execute( directoryPath, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to run " + command + " ("
                    + result.getExitCode() + "): " + result.getStderr() );
        }

        for ( String file : result.getStdout().split( "\n" ) ) {
            Map<String, Object> fileAttributes = statParse( file, allAttributes );
            UnixSshPath filePath = directoryPath.toAbsolutePath().relativize( directoryPath.resolve(
                    (String)fileAttributes.get( SupportedAttribute.name.toString() ) ) );
            map.put( filePath, new PosixFileAttributesImpl( fileAttributes ) );
        }

        return map;
    }

    private String toMode( Set<PosixFilePermission> permissions ) {
        int[] values = new int[] { 4, 2, 1 };
        int[] sections = new int[3];

        String permissionsString = PosixFilePermissions.toString( permissions );
        for ( int i = 0; i < 9; i++ ) {
            if ( permissionsString.charAt( i ) != '-' ) {
                sections[i / 3] += values[i % 3];
            }
        }

        return "" + sections[0] + sections[1] + sections[2];
    }

    private String toTouchTime( FileTime fileTime ) {
        return TOUCH_DATE_FORMAT.format( new Date( fileTime.toMillis() ) );
    }

    private static <T> Set<T> toSet( T[] array ) {
        return new HashSet<T>( Arrays.asList( array ) );
    }

    void truncateFile( UnixSshPath path, long size ) throws IOException {
        String command = "truncate -s " + size + " " + path.toString();
        ExecuteResult result = execute( path, command );
        if ( result.getExitCode() != 0 ) {
            throw new IOException( "failed to run " + command + " ("
                    + result.getExitCode() + "): " + result.getStderr() );
        }
    }

    int write( UnixSshPath path, long startIndex, ByteBuffer bytes ) throws IOException {
        try {
            int bytesPosition = bytes.position();
            // TODO cache this buffer for reuse
            ByteBuffer temp = ByteBuffer.allocateDirect( bytes.limit() - bytesPosition );
            temp.put( bytes );
            bytes.position( bytesPosition );

            String command = "dd conv=notrunc bs=1 seek=" + startIndex + " of=" + path.toString();
            ChannelExecWrapper sshChannel = path.getFileSystem().getCommandRunner().open( command );
            int written = 0;
            try (OutputStream out = sshChannel.getOutputStream()) {
                WritableByteChannel outChannel = Channels.newChannel( out );
                temp.flip();
                written = outChannel.write( temp );
            }
            if ( written > 0 ) {
                bytes.position( bytesPosition + written );
            }
            return written;
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    private class BasicFileAttributesImpl implements BasicFileAttributes {
        protected Map<String, Object> map;

        private BasicFileAttributesImpl( Map<String, Object> attributesMap ) {
            this.map = attributesMap;
        }

        private BasicFileAttributesImpl( Path path, LinkOption... linkOptions ) throws IOException {
            this( path, null, linkOptions );
        }

        private BasicFileAttributesImpl( Path path, SupportedAttribute[] additionalAttributes, LinkOption... linkOptions ) throws IOException {
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

        public FileTime creationTime() {
            return (FileTime)map.get( SupportedAttribute.creationTime.toString() );
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

        private boolean isSameFile( BasicFileAttributes other ) {
            return fileKey().equals( other.fileKey() );
        }

        public boolean isSymbolicLink() {
            return (Boolean)map.get( SupportedAttribute.isSymbolicLink.toString() );
        }

        public FileTime lastAccessTime() {
            return (FileTime)map.get( SupportedAttribute.lastAccessTime.toString() );
        }

        public FileTime lastModifiedTime() {
            return (FileTime)map.get( SupportedAttribute.lastModifiedTime.toString() );
        }

        public long size() {
            return (Long)map.get( SupportedAttribute.size.toString() );
        }
    }

    private class PosixFileAttributesImpl extends BasicFileAttributesImpl implements PosixFileAttributes {
        private PosixFileAttributesImpl( Map<String, Object> attributeMap ) {
            super( attributeMap );
        }

        private PosixFileAttributesImpl( Path path, LinkOption... linkOptions ) throws IOException {
            super( path, POSIX_ADDITIONAL_SUPPORTED_ATTRIBUTES, linkOptions );
        }

        public GroupPrincipal group() {
            return (GroupPrincipal)map.get( SupportedAttribute.group.toString() );
        }

        public UserPrincipal owner() {
            return (UserPrincipal)map.get( SupportedAttribute.owner.toString() );
        }

        @SuppressWarnings("unchecked")
        public Set<PosixFilePermission> permissions() {
            return (Set<PosixFilePermission>)map.get( SupportedAttribute.permissions.toString() );
        }
    }

    private enum SupportedAttribute {
        creationTime("%W", FileTime.class),
        group("%G", GroupPrincipal.class),
        fileKey("%i", Long.TYPE),
        lastAccessTime("%X", FileTime.class),
        lastModifiedTime("%Y", FileTime.class),
        lastChangedTime("%Z", FileTime.class),
        name("%n", String.class),
        owner("%U", UserPrincipal.class),
        permissions("%A", Set.class),
        size("%s", Long.TYPE),
        isRegularFile("%F", Boolean.TYPE),
        isDirectory("%F", Boolean.TYPE),
        isSymbolicLink("%F", Boolean.TYPE),
        isOther("%F", Boolean.TYPE);

        private static Map<String, SupportedAttribute> lookup;
        private static final char[] allPermissions = new char[] { 'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x' };

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
            if ( this == owner ) {
                return new StandardUserPrincipal( value );
            }
            if ( this == group ) {
                return new StandardGroupPrincipal( value );
            }
            if ( this == permissions ) {
                // need to remove leading 'd' and replace possible 's'
                char[] permissions = value.substring( 1 ).toCharArray();
                for ( int i = 0; i < 9; i++ ) {
                    if ( permissions[i] != '-' ) {
                        permissions[i] = allPermissions[i];
                    }
                }
                return PosixFilePermissions.fromString( new String( permissions ) );
            }
            if ( valueClass == Long.TYPE ) {
                return Long.parseLong( value );
            }
            if ( valueClass == FileTime.class ) {
                return FileTime.fromMillis( Long.parseLong( value ) * 1000 );
            }

            return value;
        }
    }
}
