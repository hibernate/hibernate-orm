/*
 * Copyright 20024 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
