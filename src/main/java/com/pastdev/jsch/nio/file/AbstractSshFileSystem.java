package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;


import com.pastdev.jsch.command.CommandRunner;


public abstract class AbstractSshFileSystem extends FileSystem {
    private static final Set<String> supportedFileAttributeViews;

    static {
        supportedFileAttributeViews = new HashSet<String>();
        supportedFileAttributeViews.add( "basic" );
    }

    private AbstractSshFileSystemProvider provider;
    private CommandRunner commandRunner;
    private URI uri;

    public AbstractSshFileSystem( AbstractSshFileSystemProvider provider, URI uri, CommandRunner commandRunner ) {
        this.provider = provider;
        this.uri = uri;
        this.commandRunner = commandRunner;
    }

    @Override
    public void close() throws IOException {
        commandRunner.close();
    }

    public CommandRunner getCommandRunner() {
        return commandRunner;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return supportedFileAttributeViews;
    }
}
