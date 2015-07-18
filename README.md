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

The important part here is that you _may_ need to use the `FileSystems.newFileSystem` overload that allows you to specify the `ClassLoader`.  You may also notice that I left out the `DefaultSessionFactory` environment configuration.  This configuration became optional as of release 0.1.7 because the default `DefaultSessionFactory` has some really sound _defaults_.  You can check the javadoc to see what they are...

