package com.pastdev.jsch.nio.file.spi;


import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;


abstract public class AbstractSshFileSystemProvider extends FileSystemProvider {

    public static class ArrayEntryDirectoryStream implements DirectoryStream<Path> {
        private String[] entries;
        private Path parent;

        public ArrayEntryDirectoryStream( Path parent, String[] entries ) {
            this.parent = parent;
            this.entries = entries;
        }

        public void close() throws IOException {
            // nothing to do...
        }

        public Iterator<Path> iterator() {
            return new Iterator<Path>() {
                private int currentIndex = 0;

                public boolean hasNext() {
                    return currentIndex < entries.length;
                }

                public Path next() {
                    return parent.resolve( entries[currentIndex++] );
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
