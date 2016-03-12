package com.pastdev.jsch.nio.file;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchEvent.Kind;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.command.CommandRunner;
import com.pastdev.jsch.command.CommandRunner.ChannelExecWrapper;


public class UnixSshPathINotifyWatchKey extends UnixSshPathWatchKey {
    private static Logger logger = LoggerFactory.getLogger( UnixSshPathINotifyWatchKey.class );
    private static final String SEPARATOR = " ";

    private CommandRunner commandRunner = null;

    public UnixSshPathINotifyWatchKey( UnixSshFileSystemWatchService watchService, UnixSshPath dir, Kind<?>[] kinds ) {
        super( watchService, dir, kinds, -1, null );
    }
    
    private void closeCommandRunner() {
        if ( commandRunner != null ) {
            try {
                commandRunner.close();
            }
            catch ( IOException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cancel() {
        closeCommandRunner();
        super.cancel();
    }

    private void event( String line ) {
        String[] actionAndFile = line.split( SEPARATOR );
        UnixSshPath filePath = dir.toAbsolutePath().relativize( dir.resolve( actionAndFile[1] ).toAbsolutePath() );
        if ( "CREATE".equals( actionAndFile[0] ) ) {
            addCreateEvent( filePath );
        }
        else if ( "MODIFY".equals( actionAndFile[0] ) ) {
            addModifyEvent( filePath );
        }
        else if ( "DELETE".equals( actionAndFile[0] ) ) {
            addDeleteEvent( filePath );
        }
    }

    @Override
    public void run() {
        StringBuilder commandBuilder = new StringBuilder( dir.getFileSystem().getCommand( "inotifywait" ) )
                .append( " -m -e create -e modify -e delete  --format '%:e" ).append( SEPARATOR ).append( "%f' " )
                .append( dir.toAbsolutePath().quotedString() );

        try {
            // duplicate the command runner so that we can close it causing the
            // underlying socket to exit to _interrupt_ the buffered reader.
            // http://stackoverflow.com/a/3596072/516433
            commandRunner = dir.getFileSystem().getCommandRunner().duplicate();

            ChannelExecWrapper channel = commandRunner.open( commandBuilder.toString() );
            try (BufferedReader reader = new BufferedReader( new InputStreamReader( channel.getInputStream() ) )) {
                String line = null;
                while ( (line = reader.readLine()) != null ) {
                    event( line );
                }
            }
        }
        catch ( JSchException e ) {
            logger.debug( "watch service failed: ", e.getMessage() );
        }
        catch ( IOException e ) {
            logger.debug( "watch service failed: ", e.getMessage() );
        }
        catch ( ClosedWatchServiceException e ) {
            logger.debug( "watch service was closed, so exit" );
        }
        finally {
            closeCommandRunner();
        }
        logger.info( "poller stopped for {}", dir );
    }
}
