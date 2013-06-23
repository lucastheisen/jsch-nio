package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// use this command: find . -maxdepth 1 -type f -exec stat -c '%Y %n' {} +

public class UnixSshWatchService implements WatchService {
    private static Logger logger = LoggerFactory.getLogger( UnixSshWatchService.class );

    private long checkInterval;
    private TimeUnit checkIntervalTimeUnit;
    private volatile boolean closed;
    private final LinkedBlockingDeque<WatchKey> pendingKeys;
    private ExecutorService executorService;

    public UnixSshWatchService( long checkInterval, TimeUnit checkIntervalTimeUnit ) {
        this.checkInterval = checkInterval;
        this.checkIntervalTimeUnit = checkIntervalTimeUnit;
        this.pendingKeys = new LinkedBlockingDeque<WatchKey>();
        executorService = Executors.newFixedThreadPool( 1 );
        // executorService.execute( new SshDirectoryWatcher() );
    }

    @Override
    public void close() throws IOException {
        if ( closed ) return;
        closed = true;
    }

    private void ensureOpen() {
        if ( closed ) throw new ClosedWatchServiceException();
    }

    @Override
    public WatchKey poll() {
        ensureOpen();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WatchKey poll( long timeout, TimeUnit unit ) throws InterruptedException {
        ensureOpen();
        // TODO Auto-generated method stub
        return null;
    }

    WatchKey register( UnixSshPath path, Kind<?>[] events, Modifier... modifiers ) {
        // TODO implement register
        return null;
    }

    @Override
    public WatchKey take() throws InterruptedException {
        ensureOpen();
        // TODO Auto-generated method stub
        return null;
    }
}
