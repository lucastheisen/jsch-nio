package com.pastdev.jsch.nio.file;


import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;


public class UnixSshPosixFileAttributeView extends UnixSshBasicFileAttributeView implements PosixFileAttributeView {

    UnixSshPosixFileAttributeView( UnixSshPath path, LinkOption[] options ) {
        super( path, options );
    }

    @Override
    public UserPrincipal getOwner() throws IOException {
        return readAttributes().owner();
    }

    @Override
    public String name() {
        return "posix";
    }

    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        return getPath().getFileSystem().provider().readAttributes(
                getPath(), PosixFileAttributes.class, getOptions() );
    }

    @SuppressWarnings("unchecked")
    void setAttribute( String attributeName, Object value ) throws IOException {
        if ( attributeName.equals( "group" ) ) {
            setGroup( (GroupPrincipal)value );
        }
        else if ( attributeName.equals( "owner" ) ) {
            setOwner( (UserPrincipal)value );
        }
        else if ( attributeName.equals( "permissions" ) ) {
            setPermissions( (Set<PosixFilePermission>)value );
        }
        else {
            super.setAttribute( attributeName, value );
        }
    }

    @Override
    public void setGroup( GroupPrincipal group ) throws IOException {
        getPath().getFileSystem().provider().setGroup( getPath(), group );
    }

    @Override
    public void setOwner( UserPrincipal owner ) throws IOException {
        getPath().getFileSystem().provider().setOwner( getPath(), owner );
    }

    @Override
    public void setPermissions( Set<PosixFilePermission> permissions ) throws IOException {
        getPath().getFileSystem().provider().setPermissions( getPath(), permissions );
    }
}
