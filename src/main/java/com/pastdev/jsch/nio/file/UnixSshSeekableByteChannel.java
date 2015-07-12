package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Set;


public class UnixSshSeekableByteChannel implements SeekableByteChannel {
    private boolean append;
    private boolean open;
    private UnixSshPath path;
    private long position = 0;
    private UnixSshFileSystemProvider provider;
    private boolean readable;
    private long size;
    private boolean writeable;

    public UnixSshSeekableByteChannel( UnixSshPath path, Set<? extends OpenOption> openOptions, FileAttribute<?>... createFileAttributes ) throws IOException {
        this.path = path.toAbsolutePath();
        this.append = openOptions.contains( StandardOpenOption.APPEND );
        this.readable = openOptions.isEmpty() || openOptions.contains( StandardOpenOption.READ );
        this.writeable = openOptions.contains( StandardOpenOption.WRITE );

        this.provider = path.getFileSystem().provider();

        PosixFileAttributes attributes = null;
        try {
            provider.checkAccess( path );
            attributes = provider.readAttributes( path, PosixFileAttributes.class );
        }
        catch ( NoSuchFileException e ) {
        }

        boolean create = false;
        if ( openOptions.contains( StandardOpenOption.CREATE_NEW ) ) {
            if ( attributes != null ) {
                throw new FileAlreadyExistsException( path.toString() );
            }
            create = true;
        }
        else if ( openOptions.contains( StandardOpenOption.CREATE ) ) {
            if ( attributes == null ) {
                create = true;
            }
        }
        else if ( attributes == null ) {
            throw new NoSuchFileException( "file not found and no CREATE/CREATE_NEW specified for "
                    + path.toString() );
        }

        if ( create ) {
            attributes = provider.createFile( path, createFileAttributes );
        }

        size = attributes.size();

        open = true;

        // maybe wanna lock file a la 'flock'
    }

    @Override
    public void close() throws IOException {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public UnixSshSeekableByteChannel position( long position ) throws IOException {
        this.position = position;
        return this;
    }

    @Override
    public int read( ByteBuffer bytes ) throws IOException {
        if ( !readable ) {
            throw new NonReadableChannelException();
        }
        if ( position >= size ) {
            return -1;
        }

        int read = provider.read( path, position, bytes );
        position += read;
        if ( position > size ) {
            // sucks, means somebody else is also writing this file, bad things
            // are gonna happen here...
            size = position;
        }

        return read;
    }

    @Override
    public long size() throws IOException {
        return size;
    }

    @Override
    public UnixSshSeekableByteChannel truncate( long size ) throws IOException {
        if ( !writeable ) {
            throw new NonWritableChannelException();
        }
        if ( size < 0 ) {
            throw new IllegalArgumentException( "size must be positive" );
        }
        if ( size >= this.size ) {
            return this;
        }

        provider.truncateFile( path, position );
        if ( position > size ) {
            position = size;
        }
        this.size = size;
        return this;
    }

    @Override
    public int write( ByteBuffer bytes ) throws IOException {
        if ( !writeable ) {
            throw new NonWritableChannelException();
        }
        if ( append ) {
            position = size();
        }

        int written = provider.write( path, position, bytes );
        position += written;
        if ( position > size ) {
            size = position;
        }

        return written;
    }
}
