package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.spi.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Set;


import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.nio.file.spi.UnixSshFileSystemProvider;


public class UnixSshFileSystem extends AbstractSshFileSystem {
    private String defaultDirectory;

    public UnixSshFileSystem( UnixSshFileSystemProvider provider, URI uri, CommandRunner commandRunner ) {
        super( provider, uri, commandRunner );
        this.defaultDirectory = uri.getPath();

        if ( defaultDirectory.charAt( 0 ) != PATH_SEPARATOR ) {
            throw new RuntimeException( "default directory must be absolute" );
        }
    }

    String getDefaultDirectory() {
        return defaultDirectory;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path getPath( String first, String... more ) {
        if ( more == null || more.length == 0 ) return new UnixSshPath( this, first );

        StringBuilder builder = new StringBuilder( first );
        for ( String part : more ) {
            builder.append( PATH_SEPARATOR )
                    .append( part );
        }
        return new UnixSshPath( this, builder.toString() );
    }

    @Override
    public PathMatcher getPathMatcher( String arg0 ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSeparator() {
        return Character.toString( PATH_SEPARATOR );
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // TODO Auto-generated method stub
        return null;
    }
}
