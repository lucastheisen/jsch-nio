jsch-nio 
========
_Note that this project depends on the [jsch-extension](https://github.com/lucastheisen/jsch-extension) project_

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

# New in Version 0.1.7
The requirement to specify the `defaultSessionFactory` in the environment has been removed.  You can now grab your `FileSystem` like this:

    try (FileSystem sshfs = FileSystems.newFileSystem( 
        new URI( "ssh.unix://" + hostname + "/home/joe", Collections.EMPTY_MAP )) {
        Path path = sshfs.getPath( "afile" ); // refers to /home/joe/afile
        try (InputStream inputStream = path.getFileSystem().provider().newInputStream( path )) {
            // do something with inputStream...
        }
    }
    
This is due to the fact that `DefaultSessionFactory` has some fine defaults.  Specifically, the username, port, known hosts, and identity values.  For the details on how they are set, see the javadoc.  Furthermore, the username, hostname, and port can all be overridden by the `URI` used in the `FileSystems.newFileSystem` method.

# Groovy Example
For all of you who want to use this library in a groovy app, the `GroovyClassLoader` may make things _slightly_ more difficult.  To that end, here is a fully working example:

    @Grab(group='com.pastdev', module='jsch-nio', version='0.1.7')

    import java.nio.file.FileSystems

    def username = System.getProperty( 'user.name' )
    def hostname = 'localhost'

    def fileContents = new StringBuilder()
    def uri = new URI( "ssh.unix://${username}@${hostname}/home/${username}" )
    FileSystems.newFileSystem( uri, [:], getClass().getClassLoader() )
            .withCloseable { sshfs ->
                def path = sshfs.getPath( "afile" )
                new InputStreamReader( path.getFileSystem().provider().newInputStream( path ) )
                        .withCloseable { reader ->
                            def read = null
                            while ( (read = reader.readLine()) != null ) {
                                fileContents.append( read )
                            }
                        }
            }

    println( "File contains:\n*********************\n${fileContents}\n*********************" )

The important part here is that you _may_ need to use the `FileSystems.newFileSystem` overload that allows you to specify the `ClassLoader`.  You may also notice that I left out the `DefaultSessionFactory` environment configuration per the new behavior as of [version 0.1.7](#new-in-version-017)

# New in Version 1.0.2

The constructor for `UnixSshFileSystemWatchService` was removed in favor of factory methods that offer more flexibility to the `UnixSshFileSystem`.  This required a major version change due to the minor backwards incompatibility that _should_ not have been used anyway (a watch service should be obtained from the `FileSystem`, not constructed).  This was done to add support for an [inotify](https://en.wikipedia.org/wiki/Inotify) enabled watch service.  With this new feature, events are fired by the remote operating system rather than polling for changes.  This should _drastically_ improve performance in some cases.  Specifically when the folder being watched has a large number of files/folders in it.  In order to use this new feature, the remote system will have to have the `inotifywait` command available (part of the `inotify-tools` package).  To enable it, you supply the `watchservice.inotify` property with a value of `true` to the registration:

    Map<String, Object> environment = new HashMap<String, Object>();
    environment.put( "defaultSessionFactory", defaultSessionFactory );
    environment.put( "watchservice.inotify", true );
    uri = new URI( scheme + "://" + username + "@" + hostname + ":" + port + sshPath );
    FileSystem fileSystem = FileSystems.newFileSystem( uri, environment );

