package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;












import org.slf4j.Logger;
import org.slf4j.LoggerFactory;












import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.pastdev.jsch.sftp.SftpRunner.Sftp;


public class UnixSshSftpHybridFileSystemProvider extends UnixSshFileSystemProvider {
    private static final Logger logger = LoggerFactory.getLogger( UnixSshSftpHybridFileSystemProvider.class );
    public static final String SCHEME_SSH_SFTP_HYBRID_UNIX = "sshSftpHybrid.unix";

    private Map<URI, UnixSshSftpHybridFileSystem> fileSystemMap;

    public UnixSshSftpHybridFileSystemProvider() {
        this.fileSystemMap = new HashMap<URI, UnixSshSftpHybridFileSystem>();
    }

    UnixSshPath checkPath( Path path ) {
        if ( path == null ) {
            throw new NullPointerException();
        }
        if ( !(path instanceof UnixSshPath) ) {
            throw new IllegalArgumentException( "path not an instanceof UnixSshSftpHybridPath" );
        }
        return (UnixSshPath)path;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createDirectory( Path path, FileAttribute<?>... fileAttributes ) throws IOException {
        final UnixSshPath unixPath = checkPath( path );
        Set<PosixFilePermission> permissions = null;
        for ( FileAttribute<?> fileAttribute : fileAttributes ) {
            if ( fileAttribute.name().equals( "posix:permissions" ) ) {
                permissions = (Set<PosixFilePermission>)fileAttribute.value();
            }
        }
        final int permissionsAsInt = permissions == null ? -1 : toInt( permissions );

        try {
            logger.debug( "Getting sftpRunner to execute sftp createDirectory" );
            ((UnixSshSftpHybridFileSystem)unixPath.getFileSystem()).getSftpRunner().execute( new Sftp() {
                @Override
                public void run( ChannelSftp sftp ) throws IOException {
                    final String abspath = unixPath.toAbsolutePath().toString();

                    SftpATTRS stat = null;
                    try {
                        stat = sftp.lstat( abspath );
                    }
                    catch ( SftpException e ) {
                    }

                    if ( stat != null && stat.isDir() ) {
                        throw new FileAlreadyExistsException( "Directory " + unixPath + " already exists" );
                    }

                    if ( stat == null || !stat.isDir() ) {
                        try {
                            sftp.mkdir( abspath );
                        }
                        catch ( SftpException e ) {
                            throw new IOException( "Could not create directory", e );
                        }
                    }

                    if ( permissionsAsInt >= 0 ) {
                        try {
                            sftp.chmod( permissionsAsInt, abspath );
                        }
                        catch ( SftpException e ) {
                            throw new IOException( "Could change permission on created directory", e );
                        }
                    }

                }
            } );
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    @Override
    public FileSystem getFileSystem( URI uri ) {
        UnixSshFileSystem fileSystem = fileSystemMap.get( uri.resolve( PATH_SEPARATOR_STRING ) );
        if ( fileSystem == null ) {
            throw new FileSystemNotFoundException( "no filesystem defined for " + uri.toString() );
        }
        return fileSystem;
    }

    @Override
    public String getScheme() {
        return SCHEME_SSH_SFTP_HYBRID_UNIX;
    }

    @Override
    public FileSystem newFileSystem( URI uri, Map<String, ?> environment ) throws IOException {
        URI baseUri = uri.resolve( PATH_SEPARATOR_STRING );
        UnixSshSftpHybridFileSystem existing = fileSystemMap.get( baseUri );
        if ( existing != null ) {
            throw new RuntimeException( "filesystem already exists for " + uri.toString() + " at " + existing.toString() );
        }

        UnixSshSftpHybridFileSystem fileSystem = new UnixSshSftpHybridFileSystem( this, uri, environment );
        fileSystemMap.put( baseUri, fileSystem );
        return fileSystem;
    }

    void removeFileSystem( UnixSshFileSystem fileSystem ) {
        fileSystemMap.remove( fileSystem.getUri().resolve( PATH_SEPARATOR_STRING ) );
    }

    private static int toInt( Set<PosixFilePermission> permissions ) {
        int value = 0;
        for ( PosixFilePermission permission : permissions ) {
            switch ( permission ) {
                case OWNER_READ:
                    value |= 00400;
                    break;
                case OWNER_WRITE:
                    value |= 00200;
                    break;
                case OWNER_EXECUTE:
                    value |= 00100;
                    break;
                case GROUP_READ:
                    value |= 00040;
                    break;
                case GROUP_WRITE:
                    value |= 00020;
                    break;
                case GROUP_EXECUTE:
                    value |= 00010;
                    break;
                case OTHERS_READ:
                    value |= 00004;
                    break;
                case OTHERS_WRITE:
                    value |= 00002;
                    break;
                case OTHERS_EXECUTE:
                    value |= 00001;
                    break;
            }
        }

        return value;
    }
}
