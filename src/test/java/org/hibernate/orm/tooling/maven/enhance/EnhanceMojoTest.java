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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EnhanceMojoTest {

    @TempDir
    File tempDir;
	
    @Test
    void testAssembleSourceSet() throws Exception {
        Method assembleSourceSetMethod = EnhanceMojo.class.getDeclaredMethod("assembleSourceSet");
        assembleSourceSetMethod.setAccessible(true);
        Field classesDirectoryField = EnhanceMojo.class.getDeclaredField("classesDirectory");
        classesDirectoryField.setAccessible(true);
        Field sourceSetField = EnhanceMojo.class.getDeclaredField("sourceSet");
        sourceSetField.setAccessible(true);
        EnhanceMojo enhanceMojo = new EnhanceMojo();
        File classesDirectory = new File(tempDir, "classes");
        classesDirectory.mkdirs();
        classesDirectoryField.set(enhanceMojo, classesDirectory);
        File fooFolder = new File(classesDirectory, "org/foo");
        fooFolder.mkdirs();
        File barClassFile = new File(fooFolder, "Bar.class");
        barClassFile.createNewFile();
        File barFolder = new File(classesDirectory, "bar");
        barFolder.mkdirs();
        File fooTxtFile = new File (barFolder, "Foo.txt");
        fooTxtFile.createNewFile();
        List<?> sourceSet = (List<?>)sourceSetField.get(enhanceMojo);
        assertTrue(sourceSet.isEmpty());
        assembleSourceSetMethod.invoke(enhanceMojo);
        assertFalse(sourceSet.isEmpty());
        assertTrue(sourceSet.contains(barClassFile));
        assertFalse(sourceSet.contains(fooTxtFile));
        assertEquals(1, sourceSet.size());
    }

}
