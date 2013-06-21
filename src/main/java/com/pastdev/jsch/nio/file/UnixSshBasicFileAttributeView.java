package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;


public class UnixSshBasicFileAttributeView implements BasicFileAttributeView {
    private LinkOption[] options;
    private UnixSshPath path;

    UnixSshBasicFileAttributeView( UnixSshPath path, LinkOption... options ) {
        this.path = path;
    }
    
    LinkOption[] getOptions() {
        return options;
    }
    
    UnixSshPath getPath() {
        return path;
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return path.getFileSystem().provider().readAttributes( path, BasicFileAttributes.class, options );
    }

    @Override
    public void setTimes( FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime ) throws IOException {
        // TODO Auto-generated method stub

    }
}
