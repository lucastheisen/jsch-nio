package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;
import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR_STRING;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


import com.jcraft.jsch.JSchException;


public class UnixSshFileSystem extends AbstractSshFileSystem {
    private UnixSshPath defaultDirectory;
    private UnixSshPath rootDirectory;

    public UnixSshFileSystem( UnixSshFileSystemProvider provider, URI uri, Map<String, ?> environment ) throws IOException {
        super( provider, uri, environment );

        this.defaultDirectory = new UnixSshPath( this, uri.getPath() );
        if ( !defaultDirectory.isAbsolute() ) {
            throw new RuntimeException( "default directory must be absolute" );
        }

        rootDirectory = new UnixSshPath( this, PATH_SEPARATOR_STRING );
    }

    @Override
    public void close() throws IOException {
        getCommandRunner().close();
        provider().removeFileSystem( this );
    }

    UnixSshPath getDefaultDirectory() {
        return defaultDirectory;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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

    @Override
    public PathMatcher getPathMatcher( String syntaxAndPattern ) {
        int firstColon = syntaxAndPattern.indexOf( ':' );
        if ( firstColon == -1 ) {
            throw new IllegalArgumentException( "must be of the form 'syntax:pattern'" );
        }

        String syntax = syntaxAndPattern.substring( 0, firstColon );
        String patternString = syntaxAndPattern.substring( firstColon + 1 );

        if ( syntax.equalsIgnoreCase( "glob" ) ) {
            StringBuilder builder = new StringBuilder().append( '^' );
            char[] chars = patternString.toCharArray();
            for ( int i = 0; i < chars.length; i++ ) {
                char c = chars[i];
                if ( c == '*' ) {
                    if ( chars[i + 1] == '*' ) {
                        builder.append( ".*" );
                        i++;
                    }
                    else {
                        builder.append( "[^/]*" );
                    }
                }
                else if ( c == '?' ) {
                    builder.append( '.' );
                }
                else if ( c == '{' ) {
                    builder.append( "(?:" );
                    while ( true ) {
                        if ( ++i < chars.length ) {
                            c = chars[i];
                        }
                        else {
                            throw new IllegalArgumentException( "invalid glob '" + patternString + "'" );
                        }

                        if ( c == ',' ) {
                            builder.append( '|' );
                        }
                        else if ( c == '}' ) {

                            break;
                        }
                        else {
                            builder.append( c );
                        }
                    }
                    builder.append( ")" );
                }
                else if ( c == '[' ) {
                    builder.append( '[' );
                    boolean first = true;
                    boolean caratEncountered = false;
                    while ( true ) {
                        if ( ++i < chars.length ) {
                            c = chars[i];
                        }
                        else {
                            throw new IllegalArgumentException( "invalid glob '" + patternString + "', unclosed range" );
                        }

                        if ( first ) {
                            if ( c == '!' ) {
                                builder.append( "^" );
                            }
                            else if ( c == '^' ) {
                                caratEncountered = true;
                            }
                            else {
                                builder.append( c );
                            }
                            first = false;
                            continue;
                        }

                        if ( c == ']' ) {
                            break;
                        }
                        else if ( c == '\\' ) {
                            builder.append( "\\\\" );
                        }
                        else {
                            builder.append( c );
                        }
                    }
                    if ( caratEncountered ) builder.append( '^' );
                    builder.append( ']' );
                }
                else if ( c == '\\' ) {
                    if ( ++i < chars.length ) {
                        builder.append( chars[i] );
                    }
                    else {
                        throw new IllegalArgumentException( "invalid glob '" + patternString + "'" );
                    }
                }
                else if ( c == '.' ) {
                    builder.append( "\\." );
                }
                else if ( c == '+' ) {
                    builder.append( "\\+" );
                }
                else if ( c == '(' ) {
                    builder.append( "\\(" );
                }
                else {
                    builder.append( c );
                }
            }
            patternString = builder.append( '$' ).toString();
        }
        else if ( syntax.equalsIgnoreCase( "regex" ) ) {
            // do nothing, pattern string is already regex
        }
        else {
            throw new UnsupportedOperationException( "i dont have any clue what '" + syntax + "' is supposed to mean" );
        }

        final Pattern pattern = Pattern.compile( patternString );
        return new PathMatcher() {
            @Override
            public boolean matches( Path path ) {
                return pattern.matcher( path.toString() ).matches();
            }
        };
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.unmodifiableList(
                Arrays.asList( new Path[] { rootDirectory } ) );
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR_STRING;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return new UserPrincipalLookupService() {
            @Override
            public UserPrincipal lookupPrincipalByName( String user ) throws IOException {
                try {
                    if ( getCommandRunner().execute( "id " + user ).getExitCode() == 0 ) {
                        return new StandardUserPrincipal( user );
                    }
                    else {
                        throw new UserPrincipalNotFoundException( user + " does not exist" );
                    }
                }
                catch ( JSchException e ) {
                    throw new IOException( e );
                }
            }

            @Override
            public GroupPrincipal lookupPrincipalByGroupName( String group ) throws IOException {
                try {
                    // I don't like this, but don't have a better way right
                    // now...
                    // Should be pretty safe in most instances
                    if ( getCommandRunner().execute( "egrep -i \"^" + group + "\" /etc/group" ).getExitCode() == 0 ) {
                        return new StandardGroupPrincipal( group );
                    }
                    else {
                        throw new UserPrincipalNotFoundException( group + " does not exist" );
                    }
                }
                catch ( JSchException e ) {
                    throw new IOException( e );
                }
            }
        };
    }

    @Override
    public boolean isOpen() {
        // command runner will just open a new session if the current session is
        // closed, so it is effectively always open
        return true;
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        if ( getBooleanFromEnvironment( "watchservice.inotify" ) ) {
            return UnixSshFileSystemWatchService.inotifyWatchService();
        }
        else {
            // TODO make sure these values are set in environment, or get good
            // defaults
            return UnixSshFileSystemWatchService.pollingWatchService(
                    getLongFromEnvironment( "watchservice.polling.interval" ),
                    getTimeUnitFromEnvironment( "watchservice.polling.timeunit" ) );
        }
    }

    @Override
    public UnixSshFileSystemProvider provider() {
        return (UnixSshFileSystemProvider)super.provider();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        Set<String> supportedFileAttributeViews = new HashSet<String>();
        supportedFileAttributeViews.addAll( Arrays.asList( new String[] { "basic", "posix" } ) );
        return Collections.unmodifiableSet( supportedFileAttributeViews );
    }
}
