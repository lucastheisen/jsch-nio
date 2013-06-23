package com.pastdev.jsch.nio.file;


import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.Collections;
import java.util.List;


public class UnixSshPathWatchKey implements WatchKey {
    private boolean cancelled;
    private List<WatchEvent<?>> events;
    private Watchable watchable;
    private UnixSshFileSystemWatchService watchService;

    public UnixSshPathWatchKey( UnixSshFileSystemWatchService watchService, Watchable watchable ) {
        this.watchService = watchService;
        this.watchable = watchable;
        this.cancelled = false;
    }
    
    void addCreateEvent( UnixSshPath path ) {
        events.add( new UnixSshPathWatchEvent<UnixSshPath>( UnixSshPathWatchEvent.ENTRY_CREATE, path ) );
    }

    @Override
    public boolean isValid() {
        return (!cancelled) && (!watchService.closed());
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
        List<WatchEvent<?>> currentEvents = Collections.unmodifiableList( events );
        events.clear();
        return currentEvents;
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
    public void cancel() {
        watchService.unregister( this );
        cancelled = true;
    }

    @Override
    public Watchable watchable() {
        return watchable;
    }
}
