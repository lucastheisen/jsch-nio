package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;
import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR_STRING;


import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.Proxy;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionFactory.SessionFactoryBuilder;
import com.pastdev.jsch.sftp.SftpRunner;


public class UnixSshSftpHybridFileSystem extends UnixSshFileSystem {
    private static final Logger logger = LoggerFactory.getLogger( UnixSshSftpHybridFileSystem.class );

    private UnixSshPath defaultDirectory;
    private UnixSshPath rootDirectory;

    private SftpRunner sftpRunner;

    public UnixSshSftpHybridFileSystem( UnixSshFileSystemProvider provider, URI uri, Map<String, ?> environment ) throws IOException {
        super( provider, uri, environment );

        // Construct a new sessionFactory from the URI authority, path, and
        // optional environment proxy
        SessionFactory defaultSessionFactory = (SessionFactory) environment.get( "defaultSessionFactory" );
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
        Proxy proxy = (Proxy) environment.get( "proxy" );
        if ( proxy != null ) {
            builder.setProxy( proxy );
        }
        logger.debug( "Building SftpRunner" );
        this.sftpRunner = new SftpRunner( builder.build() );

        this.defaultDirectory = new UnixSshPath( this, uri.getPath() );
        if ( !defaultDirectory.isAbsolute() ) {
            throw new RuntimeException( "default directory must be absolute" );
        }

        rootDirectory = new UnixSshPath( this, PATH_SEPARATOR_STRING );
    }

    @Override
    public void close() throws IOException {
        getSftpRunner().close();
        super.close();
    }

    @Override
    public UnixSshPath getPath( String first, String... more ) {
        if ( more == null || more.length == 0 ) return new UnixSshPath( this, first );

        StringBuilder builder = new StringBuilder( first );
        for ( String part : more ) {
            builder.append( PATH_SEPARATOR )
                    .append( part );
        }
        return new UnixSshPath( this, builder.toString() );
    }

    public SftpRunner getSftpRunner() {
        return sftpRunner;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.unmodifiableList(
                Arrays.asList( new Path[] { rootDirectory } ) );
    }

    @Override
    public UnixSshSftpHybridFileSystemProvider provider() {
        return (UnixSshSftpHybridFileSystemProvider) super.provider();
    }
}
