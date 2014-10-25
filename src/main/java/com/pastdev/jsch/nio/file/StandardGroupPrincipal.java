package com.pastdev.jsch.nio.file;

import java.nio.file.attribute.GroupPrincipal;

class StandardGroupPrincipal extends StandardUserPrincipal implements GroupPrincipal {
    StandardGroupPrincipal( String name ) {
        super( name );
    }
}