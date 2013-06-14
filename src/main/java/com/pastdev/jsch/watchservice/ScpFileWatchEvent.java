package com.pastdev.jsch.watchservice;


import java.nio.file.WatchEvent;


import com.pastdev.jsch.scp.ScpFile;


public class ScpFileWatchEvent<T> implements WatchEvent<T> {
    public static final WatchEvent.Kind<ScpFile> ENTRY_CREATE =
            new ScpFileWatchEventKind<ScpFile>( "ENTRY_CREATE", ScpFile.class );
    public static final WatchEvent.Kind<Object> OVERFLOW =
            new ScpFileWatchEventKind<Object>( "OVERFLOW", Object.class );

    private final WatchEvent.Kind<T> kind;
    private final T context;
    private int count;
    
    public ScpFileWatchEvent( WatchEvent.Kind<T> kind, T context ) {
        this.kind = kind;
        this.context = context;
        this.count = 1;
    }

    @Override
    public WatchEvent.Kind<T> kind() {
        return kind;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public T context() {
        return context;
    }
    
    void increment() {
        count++;
    }

    private static class ScpFileWatchEventKind<T> implements WatchEvent.Kind<T> {
        private String name;
        private Class<T> clazz;

        public ScpFileWatchEventKind( String name, Class<T> clazz ) {
            this.name = name;
            this.clazz = clazz;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<T> type() {
            return clazz;
        }
    }
}
