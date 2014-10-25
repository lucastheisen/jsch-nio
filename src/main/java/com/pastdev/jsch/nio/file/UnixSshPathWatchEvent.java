package com.pastdev.jsch.nio.file;


import java.nio.file.WatchEvent;


public class UnixSshPathWatchEvent<T> implements WatchEvent<T> {
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
}
