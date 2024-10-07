package org.hibernate.orm.tooling.maven.enhance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EnhanceMojoTest {

    @TempDir
    File tempDir;
	
    @Test
    void testExecute() throws Exception {
    	File buildDir = new File(tempDir, "build");
        assertFalse(buildDir.exists());
    	File touchFile = new File(buildDir, "touch.txt");
        assertFalse(touchFile.exists());
        Field f = EnhanceMojo.class.getDeclaredField("outputDirectory");
        f.setAccessible(true);
        EnhanceMojo mojo = new EnhanceMojo();
        assertNull(f.get(mojo));
        f.set(mojo, buildDir);
        mojo.execute();
        assertTrue(buildDir.exists());
        assertTrue(touchFile.exists());
    }

}
