package com.pastdev.jsch.watchservice;

import java.io.IOException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;

public class SshDirectory implements Watchable {

    @Override
    public WatchKey register( WatchService watcher, Kind<?>[] events, Modifier... modifiers ) throws IOException {
        /
    }

    @Override
    public WatchKey register( WatchService watcher, Kind<?>... events ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
