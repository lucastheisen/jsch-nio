package com.pastdev.jsch.nio.file;

import java.nio.file.attribute.UserPrincipal;

class StandardUserPrincipal implements UserPrincipal {
    private String name;

    StandardUserPrincipal( String name ) {
        this.name = name;
    }

    @Override
    public boolean equals( Object o ) {
        return name.equals( o );
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}