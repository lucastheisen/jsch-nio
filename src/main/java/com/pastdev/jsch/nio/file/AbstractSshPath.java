package com.pastdev.jsch.nio.file;


import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;


abstract public class AbstractSshPath implements Path {
    private AbstractSshFileSystem fileSystem;

    protected AbstractSshPath( AbstractSshFileSystem fileSystem ) {
        this.fileSystem = fileSystem;
    }

    @Override
    public AbstractSshFileSystem getFileSystem() {
        return fileSystem;
    }

    public String getHostname() {
        return getFileSystem().getUri().getHost();
    }

    @Override
    public Path getFileName() {
        return getName( getNameCount() - 1 );
    }

    public int getPort() {
        return getFileSystem().getUri().getPort();
    }

    public String getUsername() {
        return getFileSystem().getUri().getUserInfo();
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            int index = 0;
            int count = getNameCount();

            public boolean hasNext() {
                return index < count;
            }

            public Path next() {
                return getName( index++ );
            }

            public void remove() {
                // path is immutable... dont want to allow changes
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException( "path not from default provider" );
    }
}
