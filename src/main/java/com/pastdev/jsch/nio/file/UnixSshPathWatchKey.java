package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.Collections;
import java.util.List;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.command.CommandRunner.ExecuteResult;


public class UnixSshPathWatchKey implements WatchKey, Runnable {
    private boolean cancelled;
    private UnixSshPath dir;
    private List<WatchEvent<?>> events;
    private long pollingTimeout;
    private UnixSshFileSystemWatchService watchService;

    public UnixSshPathWatchKey( UnixSshFileSystemWatchService watchService, UnixSshPath dir, long pollingTimeout ) {
        this.watchService = watchService;
        this.dir = dir;
        this.pollingTimeout = pollingTimeout;
        this.cancelled = false;
    }

    synchronized void addCreateEvent( UnixSshPath path ) {
        events.add( new UnixSshPathWatchEvent<UnixSshPath>( UnixSshPathWatchEvent.ENTRY_CREATE, path ) );
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
    public void run() {
        try {
            CommandRunner commandRunner = dir.getFileSystem().getCommandRunner();
            String command = "find " + dir.toAbsolutePath().toString() + " -maxdepth 1 -type f -exec stat -c '%Y %n' {} +";
            while ( true ) {
                try {
                    ExecuteResult result = commandRunner.execute( command );
                    String[] files = result.getStdout().split( "\n" );
                }
                catch ( JSchException | IOException e ) {
                }
                Thread.sleep( pollingTimeout );
            }
        }
        catch ( InterruptedException e ) {
            // time to close out
        }
    }

    @Override
    public UnixSshPath watchable() {
        return dir;
    }
}
