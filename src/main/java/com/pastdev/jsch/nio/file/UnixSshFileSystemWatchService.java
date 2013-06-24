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


public class UnixSshFileSystemWatchService implements WatchService {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemWatchService.class );
    private static final long DEFAULT_CHECK_INTERVAL = 10;
    private static final TimeUnit DEFAULT_CHECK_INTERVAL_TIME_UNIT = TimeUnit.MINUTES;

    private long checkInterval;
    private TimeUnit checkIntervalTimeUnit;
    private volatile boolean closed;
    private final LinkedBlockingDeque<WatchKey> pendingKeys;
    private ExecutorService executorService;

    public UnixSshFileSystemWatchService( Long checkInterval, TimeUnit checkIntervalTimeUnit ) {
        logger.debug( "creating new watch service polling every {} {}", checkInterval, checkIntervalTimeUnit );
        this.checkInterval = checkInterval == null ? DEFAULT_CHECK_INTERVAL : checkInterval;
        this.checkIntervalTimeUnit = checkIntervalTimeUnit == null 
                ? DEFAULT_CHECK_INTERVAL_TIME_UNIT : checkIntervalTimeUnit;
        this.pendingKeys = new LinkedBlockingDeque<WatchKey>();
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void close() throws IOException {
        if ( closed ) return;
        closed = true;
    }

    boolean closed() {
        return closed;
    }

    void ensureOpen() {
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
    
    void unregister( WatchKey watchKey ) {
        
    }

    @Override
    public WatchKey take() throws InterruptedException {
        ensureOpen();
        // TODO Auto-generated method stub
        return null;
    }
}
