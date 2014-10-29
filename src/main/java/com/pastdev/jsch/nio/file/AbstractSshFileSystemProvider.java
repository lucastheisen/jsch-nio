package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


abstract public class AbstractSshFileSystemProvider extends FileSystemProvider {
    @Override
    public Path getPath( URI uri ) {
        return getFileSystem( uri ).getPath( uri.getPath() );
    }

    public static class StandardDirectoryStream implements DirectoryStream<Path> {
        private List<Path> accepted;

        public StandardDirectoryStream( Path parent, String[] entries, Filter<? super Path> filter ) throws IOException {
            if ( entries == null ) {
                accepted = Collections.emptyList();
            }
            else {
                accepted = new ArrayList<Path>();
                for ( String entry : entries ) {
                    Path path = parent.resolve( entry );
                    if ( filter == null || filter.accept( path ) ) {
                        accepted.add( path );
                    }
                }
            }
        }

        public void close() throws IOException {
            // nothing to do...
        }

        public Iterator<Path> iterator() {
            return Collections.unmodifiableList( accepted ).iterator();
        }
    }
}
