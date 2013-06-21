package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;


public class UnixSshPath extends AbstractSshPath {
    private boolean absolute;
    private String[] parts;

    UnixSshPath( UnixSshFileSystem unixSshFileSystem, String path ) {
        super( unixSshFileSystem );

        // normalize path string and discover separator indexes.
        // could probably optimize this at some point...
        if ( !path.isEmpty() ) {
            String[] parts = path.split( PATH_SEPARATOR + "+", 0 );
            if ( parts[0].isEmpty() ) {
                this.absolute = true;
                this.parts = Arrays.copyOfRange( parts, 1, parts.length - 1 );
                int newLength = parts.length - 1;
                this.parts = new String[newLength];
                System.arraycopy( parts, 1, this.parts, 0, newLength );
            }
            else {
                this.parts = parts;
            }
        }
    }

    private UnixSshPath( UnixSshFileSystem unixSshFileSystem, boolean isAbsolute, String... parts ) {
        super( unixSshFileSystem );
        this.absolute = isAbsolute;
        this.parts = parts == null ? new String[0] : parts;
    }

    @Override
    public int compareTo( Path o ) {
        if ( !getFileSystem().provider().equals( o.getFileSystem().provider() ) ) {
            throw new ClassCastException( "cannot compare paths from 2 different provider instances" );
        }
        return toString().compareTo( ((UnixSshPath)o).toString() );
    }

    @Override
    public boolean endsWith( Path arg0 ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean endsWith( String arg0 ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Path getFileName() {
        // TODO Auto-generated method stub
        return null;
    }

    String getFileNameString() {
        return parts[parts.length - 1];
    }

    @Override
    public UnixSshFileSystem getFileSystem() {
        return (UnixSshFileSystem)super.getFileSystem();
    }

    @Override
    public String getHostname() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().getHost();
    }

    @Override
    public Path getName( int index ) {
        if ( index < 0 ) {
            throw new IllegalArgumentException();
        }
        if ( index >= parts.length ) {
            throw new IllegalArgumentException();
        }

        return new UnixSshPath( (UnixSshFileSystem)getFileSystem(),
                isAbsolute(), Arrays.copyOfRange( parts, 0, index + 1 ) );
    }

    @Override
    public int getNameCount() {
        return parts.length;
    }

    @Override
    public Path getParent() {
        if ( parts.length == 0 && !isAbsolute() ) {
            return null;
        }
        if ( parts.length <= 1 ) {
            return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), isAbsolute() );
        }
        return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), isAbsolute(),
                Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
    }

    @Override
    public int getPort() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().getPort();
    }

    @Override
    public Path getRoot() {
        if ( isAbsolute() ) {
            return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), true );
        }
        else {
            return null;
        }
    }

    @Override
    public String getUsername() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().getUserInfo();
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            int index = 0;

            public boolean hasNext() {
                return index < parts.length;
            }

            public Path next() {
                if ( index++ == 0 ) {
                    return getFileSystem().getPath( parts[0] );
                }
                else {
                    return getFileSystem().getPath( parts[0], Arrays.copyOfRange( parts, 1, index ) );
                }
            }

            public void remove() {
                // path is immutable... dont want to allow changes
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Path normalize() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WatchKey register( WatchService arg0, Kind<?>... arg1 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WatchKey register( WatchService arg0, Kind<?>[] arg1, Modifier... arg2 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path relativize( Path arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path resolve( Path other ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path resolve( String other ) {
        String[] newPath = new String[parts.length + 1];
        System.arraycopy( parts, 0, newPath, 0, parts.length );
        newPath[parts.length] = other;
        return new UnixSshPath( (UnixSshFileSystem)getFileSystem(), isAbsolute(), newPath );
    }

    @Override
    public Path resolveSibling( Path arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path resolveSibling( String arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean startsWith( Path arg0 ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean startsWith( String arg0 ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Path subpath( int arg0, int arg1 ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path toAbsolutePath() {
        if ( isAbsolute() ) {
            return this;
        }
        else {
            UnixSshFileSystem fileSystem = (UnixSshFileSystem)getFileSystem();
            return fileSystem.getPath(
                    fileSystem.getDefaultDirectory() + PATH_SEPARATOR + toString() );
        }
    }

    @Override
    public File toFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path toRealPath( LinkOption... arg0 ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for ( String part : parts ) {
            if ( builder.length() > 0 || isAbsolute() ) {
                builder.append( PATH_SEPARATOR );
            }
            builder.append( part );
        }

        return builder.toString();
    }

    @Override
    public URI toUri() {
        return ((UnixSshFileSystem)getFileSystem()).getUri().resolve( toString() );
    }
}
