package com.pastdev.jsch.nio.file;


import java.nio.file.WatchEvent;


public class UnixSshPathWatchEvent<T> implements WatchEvent<T> {
    public static final WatchEvent.Kind<UnixSshPath> ENTRY_CREATE =
            new UnixSshWatchEventKind<UnixSshPath>( "ENTRY_CREATE", UnixSshPath.class );
    public static final WatchEvent.Kind<Object> OVERFLOW =
            new UnixSshWatchEventKind<Object>( "OVERFLOW", Object.class );

    private final WatchEvent.Kind<T> kind;
    private final T context;
    private int count;

    UnixSshPathWatchEvent( WatchEvent.Kind<T> kind, T context ) {
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

    private static class UnixSshWatchEventKind<T> implements WatchEvent.Kind<T> {
        private String name;
        private Class<T> clazz;

        public UnixSshWatchEventKind( String name, Class<T> clazz ) {
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
