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

    void setAttribute( String attributeName, Object value ) throws IOException {
        if ( attributeName.equals( "lastModifiedTime" ) ) {
            setTimes( (FileTime)value, null, null );
        }
        else if ( attributeName.equals( "lastAccessTime" ) ) {
            setTimes( null, (FileTime)value, null );
        }
        else if ( attributeName.equals( "createTime" ) ) {
            setTimes( null, null, (FileTime)value );
        }
        else {
            throw new IllegalArgumentException( "unsupported attribute name " + attributeName );
        }
    }

    @Override
    public void setTimes( FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime ) throws IOException {
        path.getFileSystem().provider().setTimes( path, lastModifiedTime, lastAccessTime );
    }
}
