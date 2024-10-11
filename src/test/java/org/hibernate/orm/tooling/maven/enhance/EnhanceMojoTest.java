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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EnhanceMojoTest {

    @TempDir
    File tempDir;

    private Field classesDirectoryField;
    private Field sourceSetField;

    private File classesDirectory;  // folder '${tempDir}/classes'
    private File fooFolder;         // folder '${classesDirectory}/org/foo'
    private File barFolder;         // folder '${classesDirectory}/bar'
    private File barClassFile;      // file '${fooFolder}/Bar.class'
    private File fooTxtFile;        // file '${barFolder}/Foo.txt'

    private EnhanceMojo enhanceMojo;

    @BeforeEach
    void beforeEach() throws Exception {
        classesDirectoryField = EnhanceMojo.class.getDeclaredField("classesDirectory");
        classesDirectoryField.setAccessible(true);
        sourceSetField = EnhanceMojo.class.getDeclaredField("sourceSet");
        sourceSetField.setAccessible(true);  
        enhanceMojo = new EnhanceMojo();             
        classesDirectory = new File(tempDir, "classes");
        classesDirectory.mkdirs();
        classesDirectoryField.set(enhanceMojo, classesDirectory);
        fooFolder = new File(classesDirectory, "org/foo");
        fooFolder.mkdirs();
        barFolder = new File(classesDirectory, "bar");
        barFolder.mkdirs();
        barClassFile = new File(fooFolder, "Bar.class");
        barClassFile.createNewFile();
        fooTxtFile = new File (barFolder, "Foo.txt");
        fooTxtFile.createNewFile();
    }
 	
    @Test
    void testAssembleSourceSet() throws Exception {
        Method assembleSourceSetMethod = EnhanceMojo.class.getDeclaredMethod("assembleSourceSet");
        assembleSourceSetMethod.setAccessible(true);
        List<?> sourceSet = (List<?>)sourceSetField.get(enhanceMojo);
        assertTrue(sourceSet.isEmpty());
        assembleSourceSetMethod.invoke(enhanceMojo);
        assertFalse(sourceSet.isEmpty());
        assertTrue(sourceSet.contains(barClassFile));
        assertFalse(sourceSet.contains(fooTxtFile));
        assertEquals(1, sourceSet.size());
    }

    @Test
    void testCreateClassLoader() throws Exception {
        Method createClassLoaderMethod = EnhanceMojo.class.getDeclaredMethod("createClassLoader");
        createClassLoaderMethod.setAccessible(true);
        ClassLoader classLoader = (ClassLoader)createClassLoaderMethod.invoke(enhanceMojo);
        assertNotNull(classLoader);
        URL fooResource = classLoader.getResource("bar/Foo.txt");
        assertNotNull(fooResource);
        assertEquals(fooTxtFile.toURI().toURL(), fooResource);
    }

    @Test
    void testCreateEnhancementContext() throws Exception {
        Method createEnhancementContextMethod = EnhanceMojo.class.getDeclaredMethod("createEnhancementContext");
        createEnhancementContextMethod.setAccessible(true);
        EnhancementContext enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
        URLClassLoader classLoader = (URLClassLoader)enhancementContext.getLoadingClassLoader();
        assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
        assertFalse(enhancementContext.doBiDirectionalAssociationManagement(null));
        assertFalse(enhancementContext.doDirtyCheckingInline(null));
        assertFalse(enhancementContext.hasLazyLoadableAttributes(null));
        assertFalse(enhancementContext.isLazyLoadable(null));
        assertFalse(enhancementContext.doExtendedEnhancement(null));
        Field enableAssociationManagementField = EnhanceMojo.class.getDeclaredField("enableAssociationManagement");
        enableAssociationManagementField.setAccessible(true);
        enableAssociationManagementField.set(enhanceMojo, Boolean.TRUE);
        enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
        assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
        assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
        assertFalse(enhancementContext.doDirtyCheckingInline(null));
        assertFalse(enhancementContext.hasLazyLoadableAttributes(null));
        assertFalse(enhancementContext.isLazyLoadable(null));
        assertFalse(enhancementContext.doExtendedEnhancement(null));
        Field enableDirtyTrackingField = EnhanceMojo.class.getDeclaredField("enableDirtyTracking");
        enableDirtyTrackingField.setAccessible(true);
        enableDirtyTrackingField.set(enhanceMojo, Boolean.TRUE);
        enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
        assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
        assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
        assertTrue(enhancementContext.doDirtyCheckingInline(null));
        assertFalse(enhancementContext.hasLazyLoadableAttributes(null));
        assertFalse(enhancementContext.isLazyLoadable(null));
        assertFalse(enhancementContext.doExtendedEnhancement(null));
        Field enableLazyInitializationField = EnhanceMojo.class.getDeclaredField("enableLazyInitialization");
        enableLazyInitializationField.setAccessible(true);
        enableLazyInitializationField.set(enhanceMojo, Boolean.TRUE);
        enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
        assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
        assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
        assertTrue(enhancementContext.doDirtyCheckingInline(null));
        assertTrue(enhancementContext.hasLazyLoadableAttributes(null));
        assertTrue(enhancementContext.isLazyLoadable(null));
        assertFalse(enhancementContext.doExtendedEnhancement(null));
        Field enableExtendedEnhancementField = EnhanceMojo.class.getDeclaredField("enableExtendedEnhancement");
        enableExtendedEnhancementField.setAccessible(true);
        enableExtendedEnhancementField.set(enhanceMojo, Boolean.TRUE);
        enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
        assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
        assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
        assertTrue(enhancementContext.doDirtyCheckingInline(null));
        assertTrue(enhancementContext.hasLazyLoadableAttributes(null));
        assertTrue(enhancementContext.isLazyLoadable(null));
        assertTrue(enhancementContext.doExtendedEnhancement(null));
    }

    public void testCreateEnhancer() throws Exception {
        Method createEnhancerMethod = EnhanceMojo.class.getDeclaredMethod("createEnhancer");
        createEnhancerMethod.setAccessible(true);
        Enhancer enhancer = (Enhancer)createEnhancerMethod.invoke(enhanceMojo);
        assertNotNull(enhancer);
        Field byteByddyEnhancementContextField = EnhancerImpl.class.getDeclaredField("enhancementContext");
        byteByddyEnhancementContextField.setAccessible(true);
        Object byteByddyEnhancementContext = byteByddyEnhancementContextField.get(enhancer);
        assertNotNull(byteByddyEnhancementContext);
        Field enhancementContextField = byteByddyEnhancementContext.getClass().getDeclaredField("enhancementContext");
        enhancementContextField.setAccessible(true);
        EnhancementContext enhancementContext = (EnhancementContext)enhancementContextField.get(byteByddyEnhancementContext);
        assertNotNull(enhancementContext);
        ClassLoader classLoader = enhancementContext.getLoadingClassLoader();
        assertNotNull(classLoader);
        assertNotNull(classLoader);
        URL fooResource = classLoader.getResource("bar/Foo.txt");
        assertNotNull(fooResource);
        assertEquals(fooTxtFile.toURI().toURL(), fooResource);
    }

}
