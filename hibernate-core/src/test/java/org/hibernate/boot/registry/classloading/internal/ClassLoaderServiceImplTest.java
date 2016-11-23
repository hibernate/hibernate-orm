/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.classloading.internal;

import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Cédric Tabin
 */
public class ClassLoaderServiceImplTest {
    @Test
    public void testNullTCCL() {
	Thread.currentThread().setContextClassLoader(null);
	
	ClassLoaderServiceImpl csi1 = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.BEFORE);
	Class<ClassLoaderServiceImplTest> clazz1 = csi1.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz1);
	csi1.stop();
	
	ClassLoaderServiceImpl csi2 = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.AFTER);
	Class<ClassLoaderServiceImplTest> clazz2 = csi2.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz2);
	csi2.stop();
	
	ClassLoaderServiceImpl csi3 = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.NEVER);
	Class<ClassLoaderServiceImplTest> clazz3 = csi3.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz3);
	csi3.stop();
    }
    
    @Test
    public void testLookupBefore() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.BEFORE);
	Class<ClassLoaderServiceImplTest> clazz = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz);
	assertEquals(1, icl.accessCount);
	csi.stop();
    }
    
    @Test
    public void testLookupAfterAvoided() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.AFTER);
	Class<ClassLoaderServiceImplTest> clazz = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz);
	assertEquals(0, icl.accessCount);
	csi.stop();
    }
    
    @Test
    public void testLookupAfter() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.AFTER);
	try { csi.classForName("test.class.name"); assertTrue(false); }
	catch (Exception e) {}
	assertEquals(1, icl.accessCount);
	csi.stop();
    }
    
    @Test
    public void testLookupAfterNotFound() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.BEFORE);
	try { csi.classForName("test.class.not.found"); assertTrue(false); }
	catch (Exception e) { }
	assertEquals(1, icl.accessCount);
	csi.stop();
    }
    
    @Test
    public void testLookupNever() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl(null,TcclLookupPrecedence.NEVER);
	try { csi.classForName("test.class.name"); assertTrue(false); }
	catch (Exception e) { }
	assertEquals(0, icl.accessCount);
	csi.stop();
    }
    
    private static class InternalClassLoader extends ClassLoader {
	private int accessCount = 0;
	
	public InternalClassLoader() { super(null); }

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
	    ++accessCount;
	    return super.loadClass(name);
	}

	@Override
	protected URL findResource(String name) {
	    ++accessCount;
	    return super.findResource(name);
	}
    }
}
