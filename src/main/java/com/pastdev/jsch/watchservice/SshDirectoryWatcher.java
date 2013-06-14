package com.pastdev.jsch.watchservice;


import java.nio.file.WatchKey;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;


import com.pastdev.jsch.scp.ScpFile;


class SshDirectoryWatcher implements Runnable {
    private LinkedBlockingDeque<WatchKey> queue;
    
    public SshDirectoryWatcher( LinkedBlockingDeque<WatchKey> queue ) {
        
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}