jsch-nio
========
jsch-nio is an attempt to leverage JSch ssh implementation to implement an nio FileSystem and all that goes along with it.  So far there is a fully functional unix/linux FileSystemProvider that allows you to work with Path objects pointing to remote files and directories.  It leverages standard unix tools including cat, dd, touch, mkdir, rmdir, and more.

Here is a quick and dirty example:

First, create your SessionFactory (a layer on top of JSch Session that contains a fully configured JSch and user/host/port info):

    DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory( "joe", "remotehost", 22 );
    try {
        defaultSessionFactory.setKnownHosts( "/home/joe/.ssh/known_hosts" );
        defaultSessionFactory.setIdentityFromPrivateKey( "/home/joe/.ssh/id_dsa" );
    }
    catch ( JSchException e ) {
        Assume.assumeNoException( e );
    }

Then register and use the new FileSystem:

    Map<String, Object> environment = new HashMap<String, Object>();
    environment.put( "defaultSessionFactory", defaultSessionFactory );
    URI uri = new URI( "ssh.unix://" + username + "@" + hostname + ":" + port + "/home/joe" );
    try (FileSystem sshfs = FileSystems.newFileSystem( uri, environment )) {
        Path path = sshfs.getPath( "afile" ); // refers to /home/joe/afile
        try (InputStream inputStream = path.getFileSystem().provider().newInputStream( path )) {
            String fileContents = IOUtils.copyToString( inputStream );
        }
    }

There is a lot more it can do, so take a look at the unit tests for more examples.
