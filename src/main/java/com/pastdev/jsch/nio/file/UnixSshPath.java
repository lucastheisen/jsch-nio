package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessMode;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class UnixSshPath extends AbstractSshPath {
    private boolean absolute;
    private String[] parts;

    UnixSshPath( UnixSshFileSystem unixSshFileSystem, String path ) {
        super( unixSshFileSystem );

        // normalize path string and discover separator indexes.
        // could probably optimize this at some point...
        this.absolute = false;
        if ( path == null || path.isEmpty() ) {
            this.parts = new String[0];
        }
        else {
            String[] parts = path.split( PATH_SEPARATOR + "+", 0 );
            if ( parts.length == 0 ) {
                this.absolute = true;
                this.parts = parts;
            }
            else if ( parts[0].isEmpty() ) {
                this.absolute = true;
                this.parts = Arrays.copyOfRange( parts, 1, parts.length );
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

    /** {@inheritDoc} */
    @Override
    public int compareTo( Path o ) {
        if ( !getFileSystem().provider().equals( o.getFileSystem().provider() ) ) {
            throw new ClassCastException( "cannot compare paths from 2 different provider instances" );
        }
        return toString().compareTo( ((UnixSshPath) o).toString() );
    }

    /** {@inheritDoc} */
    @Override
    public boolean endsWith( Path path ) {
        if ( !getFileSystem().equals( path.getFileSystem() ) ) {
            return false;
        }
        if ( path.isAbsolute() && !isAbsolute() ) {
            return false;
        }

        int count = getNameCount();
        int otherCount = path.getNameCount();
        if ( otherCount > count ) {
            return false;
        }

        for ( count--, otherCount--; otherCount >= 0; count--, otherCount-- ) {
            if ( !path.getName( otherCount ).toString().equals( getName( count ).toString() ) ) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean endsWith( String path ) {
        return endsWith( new UnixSshPath( getFileSystem(), path ) );
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals( Object other ) {
        if ( !(other instanceof UnixSshPath) ) {
            return false;
        }
        UnixSshPath otherPath = (UnixSshPath) other;
        if ( !otherPath.getFileSystem().equals( getFileSystem() ) ) {
            return false;
        }
        return toString().equals( otherPath.toString() );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath getFileName() {
        if ( parts.length == 0 ) return null;
        return new UnixSshPath( getFileSystem(), false, getFileNameString() );
    }

    String getFileNameString() {
        return parts[parts.length - 1];
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshFileSystem getFileSystem() {
        return (UnixSshFileSystem) super.getFileSystem();
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath getName( int index ) {
        if ( index < 0 ) {
            throw new IllegalArgumentException();
        }
        if ( index >= parts.length ) {
            throw new IllegalArgumentException();
        }

        return new UnixSshPath( getFileSystem(),
                false, parts[index] );
    }

    /** {@inheritDoc} */
    @Override
    public int getNameCount() {
        return parts.length;
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath getParent() {
        //if ( parts.length == 0 && !isAbsolute() ) {
        if ( parts.length == 0 || ( parts.length == 1 && !isAbsolute() ) ) {
            return null;
        }
        if ( parts.length <= 1 ) {
            return new UnixSshPath( getFileSystem(), isAbsolute() );
        }
        return new UnixSshPath( getFileSystem(), isAbsolute(),
                Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
    }

    /** {@inheritDoc} */
    @Override
    public Path getRoot() {
        if ( isAbsolute() ) {
            return new UnixSshPath( getFileSystem(), true );
        }
        else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath normalize() {
        List<String> partsList = new ArrayList<String>();
        for ( String part : parts ) {
            if ( part.equals( "." ) ) {
                continue;
            }
            else if ( part.equals( ".." ) ) {
                int size = partsList.size();
                if ( size > 0 ) {
                    partsList.remove( size - 1 );
                }
            }
            else {
                partsList.add( part );
            }
        }
        return new UnixSshPath( getFileSystem(), isAbsolute(),
                partsList.toArray( new String[partsList.size()] ) );
    }

    /** {@inheritDoc} */
    @Override
    public WatchKey register( WatchService watcher, Kind<?>... events ) throws IOException {
        return register( watcher, events, new WatchEvent.Modifier[0] );
    }

    /** {@inheritDoc} */
    @Override
    public WatchKey register( WatchService watcher, Kind<?>[] events, Modifier... modifiers ) throws IOException {
        if ( watcher == null ) {
            throw new NullPointerException();
        }
        if ( !(watcher instanceof UnixSshFileSystemWatchService) ) {
            throw new ProviderMismatchException();
        }
        if ( !getFileSystem().provider().readAttributes( this, BasicFileAttributes.class ).isDirectory() ) {
            throw new NotDirectoryException( this.toString() );
        }
        getFileSystem().provider().checkAccess( this, AccessMode.READ );
        return ((UnixSshFileSystemWatchService) watcher).register( this, events, modifiers );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath relativize( Path other ) {
        if ( other == null ) {
            throw new NullPointerException();
        }
        if ( !(other instanceof UnixSshPath) ) {
            throw new ProviderMismatchException();
        }

        UnixSshPath unixOther = (UnixSshPath) other;
        if ( isAbsolute() && !unixOther.isAbsolute() ) {
            throw new IllegalArgumentException( "this and other must have same isAbsolute" );
        }

        if ( getNameCount() == 0 ) {
            return unixOther;
        }

        Path relative = null;
        Path remainingOther = null;
        Iterator<Path> otherIterator = unixOther.iterator();
        for ( Path part : this ) {
            if ( relative != null ) {
                relative = relative.resolve( ".." );
                continue;
            }

            if ( otherIterator.hasNext() ) {
                Path otherPart = otherIterator.next();
                if ( !part.equals( otherPart ) ) {
                    remainingOther = otherPart;
                    while ( otherIterator.hasNext() ) {
                        remainingOther = remainingOther.resolve(
                                otherIterator.next() );
                    }
                    relative = new UnixSshPath( getFileSystem(), ".." );
                }
            }
            else {
                relative = new UnixSshPath( getFileSystem(), ".." );
            }
        }

        if ( relative == null ) {
            while ( otherIterator.hasNext() ) {
                if ( remainingOther == null ) {
                    remainingOther = new UnixSshPath( getFileSystem(), "" );
                }
                else {
                    remainingOther = remainingOther.resolve(
                            otherIterator.next() );
                }
            }
            return remainingOther == null
                    ? new UnixSshPath( getFileSystem(), "" )
                    : (UnixSshPath) remainingOther;
        }
        return remainingOther == null
                ? (UnixSshPath) relative
                : (UnixSshPath) relative.resolve( remainingOther );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath resolve( Path other ) {
        if ( other.isAbsolute() ) {
            if ( other instanceof UnixSshPath ) {
                return (UnixSshPath) other;
            }
            else {
                return new UnixSshPath( getFileSystem(), other.toString() );
            }
        }
        else if ( other.getNameCount() == 0 ) {
            return this;
        }

        int count = other.getNameCount();
        String[] combined = new String[parts.length + count];
        System.arraycopy( parts, 0, combined, 0, parts.length );
        int index = parts.length;
        for ( Path otherPart : other ) {
            combined[index++] = otherPart.toString();
        }
        return new UnixSshPath( getFileSystem(), isAbsolute(), combined );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath resolve( String other ) {
        return resolve( new UnixSshPath( getFileSystem(), other ) );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath resolveSibling( Path other ) {
        return getParent().resolve( other );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath resolveSibling( String other ) {
        return resolveSibling( new UnixSshPath( getFileSystem(), other ) );
    }

    /** {@inheritDoc} */
    @Override
    public boolean startsWith( Path other ) {
        if ( !getFileSystem().equals( other.getFileSystem() ) ) {
            return false;
        }
        if ( (other.isAbsolute() && !isAbsolute()) ||
                (isAbsolute() && !other.isAbsolute()) ) {
            return false;
        }

        int count = getNameCount();
        int otherCount = other.getNameCount();
        if ( otherCount > count ) {
            return false;
        }

        for ( int i = 0; i < otherCount; i++ ) {
            if ( !other.getName( i ).toString().equals( getName( i ).toString() ) ) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean startsWith( String other ) {
        return startsWith( new UnixSshPath( getFileSystem(), other ) );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath subpath( int start, int end ) {
        String[] parts = new String[end - start];
        for ( int i = start; i < end; i++ ) {
            parts[i] = getName( i ).toString();
        }
        return new UnixSshPath( getFileSystem(), false, parts );
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath toAbsolutePath() {
        if ( isAbsolute() ) {
            return this;
        }
        else {
            return getFileSystem().getDefaultDirectory().resolve( this );
        }
    }

    /** {@inheritDoc} */
    @Override
    public UnixSshPath toRealPath( LinkOption... linkOptions ) throws IOException {
        throw new UnsupportedOperationException( "not yet implemented" );
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if ( parts.length > 0 ) {
            for ( String part : parts ) {
                if ( builder.length() > 0 || isAbsolute() ) {
                    builder.append( PATH_SEPARATOR );
                }
                builder.append( part );
            }
        }
        else if ( isAbsolute() ) {
            builder.append( PATH_SEPARATOR );
        }

        return builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public URI toUri() {
        return getFileSystem().getUri().resolve( toAbsolutePath().toString() );
    }
}
