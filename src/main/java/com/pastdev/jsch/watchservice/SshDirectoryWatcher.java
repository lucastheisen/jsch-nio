package com.pastdev.jsch.watchservice;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.scp.DestinationOs;


abstract public class SshDirectoryWatcher implements Runnable {
    private static Logger logger = LoggerFactory.getLogger( SshDirectoryWatcher.class );

    protected final String[] path;
    private SessionFactory sessionFactory;
    private long timer = 10;
    private TimeUnit timerUnit = TimeUnit.SECONDS;

    protected SshDirectoryWatcher( SessionFactory sessionFactory, String... path ) {
        this.sessionFactory = sessionFactory;
        this.path = path;
    }

    abstract protected String getCommand();

    public SshDirectoryWatcher newUnixInstance( SessionFactory sessionFactory, String... path ) {
        return new UnixSshDirectoryWatcher( sessionFactory, path );
    }

    abstract protected List<DirectoryEntry> parseCommandOutput( String output );

    @Override
    public void run() {
    }

    public static enum DirectoryEntryType {
        DIRECTORY, FILE, SYMBOLIC
    }

    protected class DirectoryEntry {
        private String mode;
        private String name;
        private long size;
        private long timestamp;
        private DirectoryEntryType type;

        public DirectoryEntry( String name, String mode, long size, long timestamp, DirectoryEntryType type ) {
            this.name = name;
            this.mode = mode;
            this.size = size;
            this.timestamp = timestamp;
            this.type = type;
        }

        public String getMode() {
            return mode;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public DirectoryEntryType getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return name + " " + mode + " " + size + " " + type + " " + timestamp;
        }
    }

    private static class UnixSshDirectoryWatcher extends SshDirectoryWatcher {
        private static final Pattern outputPattern = Pattern.compile( "^(\\d+) (\\d+) (\\d+) (\\w+) (.*)$" );
        private static final int TIMESTAMP_GROUP = 1;
        private static final int MODE_GROUP = 1;
        private static final int SIZE_GROUP = 1;
        private static final int TYPE_GROUP = 1;
        private static final int NAME_GROUP = 1;

        private String command;

        public UnixSshDirectoryWatcher( SessionFactory sessionFactory, String... path ) {
            super( sessionFactory, path );
            command = new StringBuilder( "find " )
                    .append( (path == null || path.length < 1)
                            ? "." : DestinationOs.UNIX.joinPath( path ) )
                    .append( " -maxdepth 1 -type f -exec stat -c '%Y %a %s %F %n' {} +" )
                    .toString();
        }

        @Override
        protected String getCommand() {
            return command;
        }

        @Override
        protected List<DirectoryEntry> parseCommandOutput( String output ) {
            List<DirectoryEntry> entries = new ArrayList<DirectoryEntry>();
            Matcher matcher = outputPattern.matcher( output );

            int matchIndex = 0;
            while ( matcher.find( matchIndex ) ) {
                String typeString = matcher.group( TYPE_GROUP ).toLowerCase();
                DirectoryEntryType type = null;
                if ( "file".equals( typeString ) ) {
                    type = DirectoryEntryType.FILE;
                }
                else if ( "directory".equals( typeString ) ) {
                    type = DirectoryEntryType.DIRECTORY;
                }
                else if ( "symbolic".equals( typeString ) ) {
                    type = DirectoryEntryType.SYMBOLIC;
                }
                else {
                    logger.warn( "unknown directory entry type '{}'", typeString );
                }

                if ( type != null ) {
                    entries.add( new DirectoryEntry(
                            matcher.group( NAME_GROUP ),
                            matcher.group( MODE_GROUP ),
                            Long.parseLong( matcher.group( SIZE_GROUP ) ),
                            Long.parseLong( matcher.group( TIMESTAMP_GROUP ) ),
                            type ) );
                }
                matchIndex = matcher.end();
            }
            return entries;
        }
    }
}