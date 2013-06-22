package com.pastdev.jsch.nio.file;


import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;


abstract public class AbstractSshPath implements Path {
    private AbstractSshFileSystem fileSystem;

    protected AbstractSshPath( AbstractSshFileSystem fileSystem ) {
        this.fileSystem = fileSystem;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    abstract public String getHostname();

    @Override
    public Path getFileName() {
        return getName( getNameCount() - 1 );
    }

    abstract public int getPort();

    abstract public String getUsername();

    @Override
    public File toFile() {
        throw new UnsupportedOperationException( "path not from default provider" );
    }
}
