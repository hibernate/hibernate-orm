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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.maven.plugin.logging.Log;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Entity;

public class EnhanceMojoTest {

    @TempDir
    File tempDir;

    private List<String> logMessages = new ArrayList<String>();
 
    private Field classesDirectoryField;
    private Field sourceSetField;
    private Field enhancerField;

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
        enhancerField = EnhanceMojo.class.getDeclaredField("enhancer");
        enhancerField.setAccessible(true);
        enhanceMojo = new EnhanceMojo();     
        enhanceMojo.setLog(createLog());        
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

    @Test
    void testCreateEnhancer() throws Exception {
        Method createEnhancerMethod = EnhanceMojo.class.getDeclaredMethod("createEnhancer");
        createEnhancerMethod.setAccessible(true);
        Enhancer enhancer = (Enhancer)enhancerField.get(enhanceMojo);
        assertNull(enhancer);
        createEnhancerMethod.invoke(enhanceMojo);
        enhancer = (Enhancer)enhancerField.get(enhanceMojo);
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

    @Test
    void testDetermineClassName() throws Exception {
        Method determineClassNameMethod = EnhanceMojo.class.getDeclaredMethod(
            "determineClassName", 
            new Class[] { File.class });
        determineClassNameMethod.setAccessible(true);
        assertEquals("org.foo.Bar", determineClassNameMethod.invoke(enhanceMojo, barClassFile));
    }

    @Test
    void testDiscoverTypesForClass() throws Exception {
        final List<Boolean> hasRun = new ArrayList<Boolean>();
        Method discoverTypesForClassMethod = EnhanceMojo.class.getDeclaredMethod(
            "discoverTypesForClass", 
            new Class[] { File.class });
        discoverTypesForClassMethod.setAccessible(true);
        Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
            getClass().getClassLoader(), 
            new Class[] { Enhancer.class }, 
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("discoverTypes")) {
                        assertEquals("org.foo.Bar", args[0]);
                        hasRun.add(0, true);
                    }
                    return null;
                 }               
            });
        enhancerField.set(enhanceMojo, enhancer);
        assertFalse(hasRun.contains(true));
        discoverTypesForClassMethod.invoke(enhanceMojo, barClassFile);
        assertTrue(hasRun.contains(true));
    }

    @Test
    void testDiscoverTypes() throws Exception {
        final List<Boolean> hasRun = new ArrayList<Boolean>();
        Method discoverTypesMethod = EnhanceMojo.class.getDeclaredMethod(
            "discoverTypes", 
            new Class[] { });
        discoverTypesMethod.setAccessible(true);
        Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
            getClass().getClassLoader(), 
            new Class[] { Enhancer.class }, 
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("discoverTypes")) {
                        assertEquals("org.foo.Bar", args[0]);
                        hasRun.add(0, true);
                    }
                    return null;
                 }               
            });
        enhancerField.set(enhanceMojo, enhancer);
        assertFalse(hasRun.contains(true));
        discoverTypesMethod.invoke(enhanceMojo);
        assertFalse(hasRun.contains(true));
        List<File> sourceSet = new ArrayList<File>();
        sourceSet.add(barClassFile);
        sourceSetField.set(enhanceMojo, sourceSet);
        discoverTypesMethod.invoke(enhanceMojo);
        assertTrue(hasRun.contains(true));
    }

    @Test
    void testClearFile() throws Exception {
        Method clearFileMethod = EnhanceMojo.class.getDeclaredMethod(
            "clearFile", 
            new Class[] { File.class });
        clearFileMethod.setAccessible(true);
        Files.writeString(fooTxtFile.toPath(), "foobar");
        fooTxtFile.setLastModified(0);
        assertEquals("foobar", new String(Files.readAllBytes(fooTxtFile.toPath())));
        boolean result = (boolean)clearFileMethod.invoke(enhanceMojo, new File("foobar"));
        assertFalse(result);
        result = (boolean)clearFileMethod.invoke(enhanceMojo, fooTxtFile);
        long modified = fooTxtFile.lastModified();
        assertTrue(result);
        // File should be empty
        assertTrue(Files.readAllBytes(fooTxtFile.toPath()).length == 0);
        // last modification 'after' should be after 'before'
        assertNotEquals(0, modified);
        assertTrue(modified > 0);
    }

    @Test
    void testWriteByteCodeToFile() throws Exception {
        Method writeByteCodeToFileMethod = EnhanceMojo.class.getDeclaredMethod(
            "writeByteCodeToFile", 
            new Class[] { byte[].class, File.class});
        writeByteCodeToFileMethod.setAccessible(true);
        fooTxtFile.setLastModified(0);
        // File fooTxtFile is empty
        assertTrue(Files.readAllBytes(fooTxtFile.toPath()).length == 0);
        writeByteCodeToFileMethod.invoke(enhanceMojo, "foobar".getBytes(), fooTxtFile);
        long modified = fooTxtFile.lastModified();
        // last modification 'after' should be after 'before'
        assertNotEquals(0, modified);
        assertTrue(modified > 0);
        // File should be contain 'foobar'
        assertEquals(new String(Files.readAllBytes(fooTxtFile.toPath())), "foobar");
    }

    @Test
    void testEnhanceClass() throws Exception {
        final List<Integer> calls = new ArrayList<Integer>();
        calls.add(0, 0);
        Method enhanceClassMethod = EnhanceMojo.class.getDeclaredMethod(
            "enhanceClass", 
            new Class[] { File.class });
        enhanceClassMethod.setAccessible(true);
        Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
            getClass().getClassLoader(), 
            new Class[] { Enhancer.class }, 
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                	calls.set(0, calls.get(0) + 1);
                    if (method.getName().equals("enhance")) {
                        assertEquals("org.foo.Bar", args[0]);
                    }
                    if (calls.get(0) == 1) {
                    	return "foobar".getBytes();
                    } else if (calls.get(0) == 2) {
                    	return null;
                    } else {
                    	throw new EnhancementException("foobar");
                    }
                 }               
            });
        long beforeRuns = barClassFile.lastModified();
        enhancerField.set(enhanceMojo, enhancer);
        assertEquals(0, calls.get(0));
        enhanceClassMethod.invoke(enhanceMojo, barClassFile);
        long afterFirstRun = barClassFile.lastModified();
        assertEquals(1, calls.get(0));
        assertTrue(afterFirstRun > beforeRuns);
        assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
        enhanceClassMethod.invoke(enhanceMojo, barClassFile);
        long afterSecondRun = barClassFile.lastModified();
        assertEquals(2, calls.get(0));
        assertEquals(afterSecondRun, afterFirstRun);
        assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
        try {
        	enhanceClassMethod.invoke(enhanceMojo, barClassFile);
        	fail();
        } catch (Throwable e) {
        	System.out.println(e.getMessage());
	        long afterThirdRun = barClassFile.lastModified();
	        assertEquals(3, calls.get(0));
	        assertEquals(afterThirdRun, afterFirstRun);
	        assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
        }
    }

    @Test
    void testPerformEnhancement() throws Exception {
        final List<Boolean> hasRun = new ArrayList<Boolean>();
        Method performEnhancementMethod = EnhanceMojo.class.getDeclaredMethod(
            "performEnhancement", 
            new Class[] { });
            performEnhancementMethod.setAccessible(true);
        Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
            getClass().getClassLoader(), 
            new Class[] { Enhancer.class }, 
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("enhance")) {
                        assertEquals("org.foo.Bar", args[0]);
                        hasRun.add(0, true);
                    }
                    return "foobar".getBytes();
                 }               
            });
        enhancerField.set(enhanceMojo, enhancer);
        List<File> sourceSet = new ArrayList<File>();
        sourceSet.add(barClassFile);
        sourceSetField.set(enhanceMojo, sourceSet);
        long lastModified = barClassFile.lastModified();
        assertFalse(hasRun.contains(true));
        assertNotEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
        performEnhancementMethod.invoke(enhanceMojo);
        assertTrue(hasRun.contains(true));
        assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
        assertEquals(lastModified, barClassFile.lastModified());
    }

    @Test
    void testExecute() throws Exception {
    	Method executeMethod = EnhanceMojo.class.getDeclaredMethod("execute", new Class[] {});
    	executeMethod.setAccessible(true);
    	final String barSource = 
    		"package org.foo;" +
    	    "import jakarta.persistence.Entity;" + 
    	    "@Entity public class Bar { "+ 
    	    "    private String foo; " +
    	    "    String getFoo() {  return foo; } " +
    	    "    public void setFoo(String f) { foo = f; } " +
    	    "}";
        File barJavaFile = new File(fooFolder, "Bar.java");
        Files.writeString(barJavaFile.toPath(), barSource);
    	final String fooSource = 
    		"package org.foo;" +
        	"public class Foo { "+ 
        	"    private Bar bar; " +
        	"    Bar getBar() {  return bar; } " +
        	"    public void setBar(Bar b) { bar = b; } " +
        	"}";
        File fooJavaFile = new File(fooFolder, "Foo.java");
        Files.writeString(fooJavaFile.toPath(), fooSource);
        File fooClassFile = new File(fooFolder, "Foo.class");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        URL url = Entity.class.getProtectionDomain().getCodeSource().getLocation();
        String classpath = new File(url.toURI()).getAbsolutePath();
        String[] options = List.of(
        		"-cp",
        		classpath,
        		barJavaFile.getAbsolutePath(),
        		fooJavaFile.getAbsolutePath()).toArray(new String[] {});
        compiler.run(null, null, null, options);
        String barBytesString = new String(Files.readAllBytes(barClassFile.toPath()));
        String fooBytesString = new String(Files.readAllBytes(fooClassFile.toPath()));
        List<File> sourceSet = new ArrayList<File>();
        sourceSet.add(barClassFile);
        sourceSet.add(fooClassFile);
        sourceSetField.set(enhanceMojo, sourceSet);
        executeMethod.invoke(enhanceMojo);
        assertNotEquals(barBytesString, new String(Files.readAllBytes(barClassFile.toPath())));
        assertEquals(fooBytesString, new String(Files.readAllBytes(fooClassFile.toPath())));
        URLClassLoader classLoader = new URLClassLoader(
        		new URL[] {classesDirectory.toURI().toURL()}, 
        		getClass().getClassLoader());
        Class<?> barClass = classLoader.loadClass("org.foo.Bar");
        assertNotNull(barClass);
        Method m = barClass.getMethod("$$_hibernate_getEntityInstance", new Class[]{});
        assertNotNull(m);
        Class<?> fooClass = classLoader.loadClass("org.foo.Foo");
        try {
        	m = fooClass.getMethod("$$_hibernate_getEntityInstance", new Class[]{});
        	fail();
        } catch (NoSuchMethodException e) {
        	assertEquals("org.foo.Foo.$$_hibernate_getEntityInstance()", e.getMessage());
        }
        classLoader.close();
    } 

    @Test
    void testVerifyParameters() throws Exception {
        String lazyInitializationDeprecatedWarning = 
            "[WARNING] The 'enableLazyInitialization' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning";
        String dirtyTrackingDeprecatedWarning = 
            "[WARNING] The 'enableDirtyTracking' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning";
        Method verifyParametersMethod = EnhanceMojo.class.getDeclaredMethod(
            "verifyParameters", 
            new Class[] {});
        verifyParametersMethod.setAccessible(true);
        Field enableLazyInitializationField = EnhanceMojo.class.getDeclaredField("enableLazyInitialization");
        enableLazyInitializationField.setAccessible(true);
        Field enableDirtyTrackingField = EnhanceMojo.class.getDeclaredField("enableDirtyTracking");
        enableDirtyTrackingField.setAccessible(true);
        assertTrue(logMessages.isEmpty());
        verifyParametersMethod.invoke(enhanceMojo);
        assertEquals(2, logMessages.size());
        assertTrue(logMessages.contains(lazyInitializationDeprecatedWarning));
        assertTrue(logMessages.contains(dirtyTrackingDeprecatedWarning));
        logMessages.clear();
        assertTrue(logMessages.isEmpty());
        enableLazyInitializationField.set(enhanceMojo, Boolean.TRUE);
        enableDirtyTrackingField.set(enhanceMojo, Boolean.TRUE);
        verifyParametersMethod.invoke(enhanceMojo);
        assertTrue(logMessages.isEmpty());
    }

    private Log createLog() {
        return (Log)Proxy.newProxyInstance(
            getClass().getClassLoader(), 
            new Class[] { Log.class}, 
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("info".equals(method.getName())) {
                        logMessages.add("[INFO] " + args[0]);
                    } else if ("warn".equals(method.getName())) {
                        logMessages.add("[WARNING] " + args[0]);
                    } else if ("error".equals(method.getName())) {
                        logMessages.add("[ERROR] " +args[0]);
                    }
                    return null;
                }             
            });
    }

}
