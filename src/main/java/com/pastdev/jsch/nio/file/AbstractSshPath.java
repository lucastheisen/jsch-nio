package com.pastdev.jsch.nio.file;


import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;


abstract public class AbstractSshPath implements Path {
    public static final String QUOTED_SPECIAL = "\"$";

    static public String protect(String string, String special) {
        if (string.equals(""))
            return "\"\"";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (special.indexOf(c) != -1)
                sb.append("\\");
            sb.append(c);
        }
        return sb.toString();
    }

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

    public String quotedString() {
        return '"' + protect(toString(), QUOTED_SPECIAL) + '"';
    }
}
