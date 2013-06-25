package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UnixSshPathWatchKey implements WatchKey, Runnable {
    private static Logger logger = LoggerFactory.getLogger( UnixSshPathWatchKey.class );
    private boolean cancelled;
    private UnixSshPath dir;
    private Map<UnixSshPath, PosixFileAttributes> entries;
    private List<WatchEvent<?>> events;
    private long pollingTimeout;
    private State state;
    private UnixSshFileSystemWatchService watchService;

    public UnixSshPathWatchKey( UnixSshFileSystemWatchService watchService, UnixSshPath dir, Kind<?>[] events, long pollingTimeout ) {
        this.watchService = watchService;
        this.dir = dir;
        this.pollingTimeout = pollingTimeout;
        this.cancelled = false;
        this.events = new ArrayList<WatchEvent<?>>();
        this.state = State.READY;
    }

    void addCreateEvent( UnixSshPath path ) {
        synchronized ( events ) {
            events.add( new UnixSshPathWatchEvent<Path>( StandardWatchEventKinds.ENTRY_CREATE, path ) );
            signal();
        }
    }

    void addDeleteEvent( UnixSshPath path ) {
        synchronized ( events ) {
            events.add( new UnixSshPathWatchEvent<Path>( StandardWatchEventKinds.ENTRY_DELETE, path ) );
            signal();
        }
    }

    void addModifyEvent( UnixSshPath path ) {
        synchronized ( events ) {
            events.add( new UnixSshPathWatchEvent<Path>( StandardWatchEventKinds.ENTRY_MODIFY, path ) );
            signal();
        }
    }

    @Override
    public void cancel() {
        watchService.unregister( this );
        cancelled = true;
    }

    @Override
    public boolean isValid() {
        return (!cancelled) && (!watchService.closed());
    }

    private boolean modified( PosixFileAttributes attributes, PosixFileAttributes otherAttributes ) {
        if ( attributes.size() != otherAttributes.size() ) {
            return true;
        }
        if ( attributes.lastModifiedTime() != otherAttributes.lastModifiedTime() ) {
            return true;
        }
        return false;
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
        synchronized ( events ) {
            List<WatchEvent<?>> currentEvents = Collections.unmodifiableList( events );
            events.clear();
            return currentEvents;
        }
    }

    @Override
    public boolean reset() {
        if ( !isValid() ) {
            return false;
        }
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void run() {
        try {
            while ( true ) {
                if ( cancelled ) {
                    break;
                }
                try {
                    Map<UnixSshPath, PosixFileAttributes> entries =
                            dir.getFileSystem().provider().statDirectory( dir );
                    for ( UnixSshPath entryPath : entries.keySet() ) {
                        if ( this.entries.containsKey( entryPath ) ) {
                            if ( modified( entries.get( entryPath ), this.entries.remove( entryPath ) ) ) {
                                addModifyEvent( entryPath );
                            }
                        }
                        else {
                            addCreateEvent( entryPath );
                        }
                    }
                    for ( UnixSshPath entryPath : this.entries.keySet() ) {
                        addDeleteEvent( entryPath );
                    }
                    this.entries = entries;
                }
                catch ( IOException e ) {
                    logger.error( "checking {} failed: {}", dir, e );
                    logger.debug( "checking directory failed: ", e );
                }
                Thread.sleep( pollingTimeout );
            }
        }
        catch ( InterruptedException e ) {
            // time to close out
        }
    }
    
    private void signal() {
        if ( state != State.SIGNALLED ) {
            state = State.SIGNALLED;
            watchService.enqueue( this );
        }
    }

    @Override
    public UnixSshPath watchable() {
        return dir;
    }
    
    private enum State {
        READY, SIGNALLED
    }
}
