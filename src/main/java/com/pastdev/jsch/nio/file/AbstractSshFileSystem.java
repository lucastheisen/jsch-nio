package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionFactory.SessionFactoryBuilder;
import com.pastdev.jsch.command.CommandRunner;


public abstract class AbstractSshFileSystem extends FileSystem {
    private static final Set<String> supportedFileAttributeViews;

    static {
        supportedFileAttributeViews = new HashSet<String>();
        supportedFileAttributeViews.add( "basic" );
    }

    private String binDir = null;
    private CommandRunner commandRunner;
    private Map<String, ?> environment;
    private AbstractSshFileSystemProvider provider;
    private URI uri;

    public AbstractSshFileSystem( AbstractSshFileSystemProvider provider, URI uri, Map<String, ?> environment ) throws IOException {
        this.provider = provider;
        this.uri = uri;
        this.environment = environment;

        String binDirKey = "dir.bin";
        if ( environment.containsKey( binDirKey ) ) {
            binDir = (String) environment.get( binDirKey );
        }

        // Construct a new sessionFactory from the URI authority, path, and
        // optional environment proxy
        SessionFactory defaultSessionFactory = (SessionFactory) environment.get( "defaultSessionFactory" );
        if ( defaultSessionFactory == null ) {
            defaultSessionFactory = new DefaultSessionFactory();
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
        this.commandRunner = new CommandRunner( builder.build() );
    }

    String getCommand( String command ) {
        String commandKey = "command." + command;
        if ( environment.containsKey( commandKey ) ) {
            return (String) environment.get( commandKey );
        }
        else if ( binDir != null ) {
            return binDir + PATH_SEPARATOR + command;
        }
        else {
            return command;
        }
    }

    Variant defaultVariant;

    public Variant getVariant( String command ) {
        String variantKey = "variant." + command;
        if ( environment.containsKey( variantKey ) ) {
            return (Variant) environment.get( variantKey );
        }

        // Get the host type
        if ( defaultVariant == null ) {
            final CommandRunner.ExecuteResult execute;
            try {
                execute = commandRunner.execute( "uname -s" );
            }
            catch ( JSchException | IOException e ) {
                return Variant.GNU;
            }

            if ( execute.getExitCode() != 0 ) {
                return Variant.GNU;
            }

            switch ( execute.getStdout().trim().toLowerCase() ) {
                case "darwin":
                case "freebsd":
                    defaultVariant = Variant.BSD;
                    break;
                default:
                    // TODO
                    defaultVariant = Variant.GNU;
            }

        }

        return defaultVariant;
    }

    public void setCommandRunner(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    public CommandRunner getCommandRunner() {
        return commandRunner;
    }

    public boolean getBooleanFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return false;
        }
        if ( value instanceof Boolean ) {
            return (boolean) value;
        }
        return Boolean.parseBoolean( value.toString() );
    }

    public Object getFromEnvironment( String key ) {
        return environment.get( key );
    }

    public Long getLongFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return null;
        }
        if ( value instanceof Long ) {
            return (Long) value;
        }
        return Long.parseLong( value.toString() );
    }

    public String getStringFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return null;
        }
        if ( value instanceof String ) {
            return (String) value;
        }
        return value.toString();
    }

    public TimeUnit getTimeUnitFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return null;
        }
        if ( value instanceof TimeUnit ) {
            return (TimeUnit) value;
        }
        return TimeUnit.valueOf( value.toString().toUpperCase() );
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return supportedFileAttributeViews;
    }

}
