package com.pastdev.jsch.watchservice;


import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.List;


public class ScpFileWatchKey implements WatchKey {

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean reset() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void cancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Watchable watchable() {
        // TODO Auto-generated method stub
        return null;
    }
}
