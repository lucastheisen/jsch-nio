package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UnixSshFileSystemWatchService implements WatchService {
    private static Logger logger = LoggerFactory.getLogger( UnixSshFileSystemWatchService.class );
    private static final long DEFAULT_POLLING_INTERVAL = 10;
    private static final TimeUnit DEFAULT_POLLING_INTERVAL_TIME_UNIT = TimeUnit.MINUTES;

    private long pollingInterval;
    private TimeUnit pollingIntervalTimeUnit;
    private volatile boolean closed;
    private final ExecutorService executorService;
    private final LinkedBlockingDeque<WatchKey> pendingKeys;
    private final Map<UnixSshPath, Future<?>> watchKeyFutures;
    private final Map<UnixSshPath, UnixSshPathWatchKey> watchKeys;
    private final Lock watchKeysLock;

    public UnixSshFileSystemWatchService( Long pollingInterval, TimeUnit pollingIntervalTimeUnit ) {
        logger.debug( "creating new watch service polling every {} {}", pollingInterval, pollingIntervalTimeUnit );
        this.pollingInterval = pollingInterval == null ? DEFAULT_POLLING_INTERVAL : pollingInterval;
        this.pollingIntervalTimeUnit = pollingIntervalTimeUnit == null
                ? DEFAULT_POLLING_INTERVAL_TIME_UNIT : pollingIntervalTimeUnit;
        this.pendingKeys = new LinkedBlockingDeque<>();
        this.executorService = Executors.newCachedThreadPool();
        this.watchKeys = new HashMap<>();
        this.watchKeyFutures = new HashMap<>();
        this.watchKeysLock = new ReentrantLock();
    }

    @Override
    public void close() throws IOException {
        if ( closed ) return;
        closed = true;
    }

    boolean closed() {
        return closed;
    }

    void enqueue( WatchKey watchKey ) {
        ensureOpen();
        pendingKeys.add( watchKey );
    }

    void ensureOpen() {
        if ( closed ) throw new ClosedWatchServiceException();
    }

    @Override
    public WatchKey poll() {
        ensureOpen();
        return pendingKeys.poll();
    }

    @Override
    public WatchKey poll( long timeout, TimeUnit unit ) throws InterruptedException {
        ensureOpen();
        return pendingKeys.poll( timeout, unit );
    }

    UnixSshPathWatchKey register( UnixSshPath path, Kind<?>[] events, Modifier... modifiers ) {
        try {
            watchKeysLock.lock();
            if ( watchKeys.containsKey( path ) ) {
                return watchKeys.get( path );
            }

            UnixSshPathWatchKey watchKey = new UnixSshPathWatchKey( this, path, events, pollingInterval, pollingIntervalTimeUnit );
            watchKeys.put( path, watchKey );
            watchKeyFutures.put( path, executorService.submit( watchKey ) );
            return watchKey;
        }
        finally {
            watchKeysLock.unlock();
        }
    }

    void unregister( UnixSshPathWatchKey watchKey ) {
        try {
            watchKeysLock.lock();
            UnixSshPath path = watchKey.watchable();
            if ( !watchKeys.containsKey( path ) ) {
                return;
            }

            watchKeyFutures.remove( path ).cancel( true );
            watchKeys.remove( path );
        }
        finally {
            watchKeysLock.unlock();
        }
    }

    @Override
    public WatchKey take() throws InterruptedException {
        ensureOpen();
        return pendingKeys.take();
    }
}
