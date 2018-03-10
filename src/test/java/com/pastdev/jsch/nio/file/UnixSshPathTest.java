package com.pastdev.jsch.nio.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnixSshPathTest extends FileSystemTestUtils {
	private static final Logger logger = LoggerFactory.getLogger(UnixSshPathTest.class);

    @AfterClass
    public static void afterClass() {
        closeFileSystem();
    }

    @BeforeClass
    public static void beforeClass() {
        initializeFileSystem( UnixSshFileSystemProvider.SCHEME_SSH_UNIX );
    }


	@Test
	public void TestGetParent() {
		assertNull(getPath("/").getParent());
		assertEquals(getPath("/"), getPath("/foo").getParent());
		// https://github.com/lucastheisen/jsch-nio/pull/22
		assertNull(getPath("foo").getParent());
		assertEquals(getPath("foo"), getPath("foo/bar").getParent());
	}

	private Path getPath(String path) {
		Path pathObj = FileSystems.getFileSystem( uri ).getPath(path);
		//Path pathObj = FileSystems.getDefault().getPath(path);
		logger.trace("getPath({}) -> [{}], {}", path, pathObj, pathObj.getClass().getName());
		return pathObj;
	}
}
