/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.classloading.internal;

import java.net.URL;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Cedric Tabin
 */
public class ClassLoaderServiceImplTest {
    @Test
    public void testNullTCCL() {
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl();
	Thread.currentThread().setContextClassLoader(null);
	
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.BEFORE);
	Class<ClassLoaderServiceImplTest> clazz1 = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz1);
	
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.AFTER);
	Class<ClassLoaderServiceImplTest> clazz2 = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz2);
	
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.NEVER);
	Class<ClassLoaderServiceImplTest> clazz3 = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz3);
	
	csi.stop();
	try { csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.BEFORE); assertTrue(false); } catch (Exception e) { }
	try { csi.getTTCLLookupBehavior(); assertTrue(false); } catch (Exception e) { }
    }
    
    @Test
    public void testLookupBefore() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl();
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.BEFORE);
	Class<ClassLoaderServiceImplTest> clazz = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz);
	assertEquals(1, icl.accessCount);
    }
    
    @Test
    public void testLookupAfterAvoided() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl();
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.AFTER);
	Class<ClassLoaderServiceImplTest> clazz = csi.classForName(ClassLoaderServiceImplTest.class.getName());
	assertEquals(ClassLoaderServiceImplTest.class, clazz);
	assertEquals(0, icl.accessCount);
    }
    
    @Test
    public void testLookupAfter() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl();
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.AFTER);
	try { csi.classForName("test.class.name"); assertTrue(false); }
	catch (Exception e) {}
	assertEquals(1, icl.accessCount);
    }
    
    @Test
    public void testLookupAfterNotFound() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl();
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.AFTER);
	try { csi.classForName("test.class.not.found"); assertTrue(false); }
	catch (Exception e) { }
	assertEquals(1, icl.accessCount);
    }
    
    @Test
    public void testLookupNever() {
	InternalClassLoader icl = new InternalClassLoader();
	Thread.currentThread().setContextClassLoader(icl);
	
	ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl();
	csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.NEVER);
	try { csi.classForName("test.class.name"); assertTrue(false); }
	catch (Exception e) { }
	assertEquals(0, icl.accessCount);
	
	csi.stop();
	try { csi.setTCCLLookupBehavior(ClassLoaderService.TCCLLookupBehavior.BEFORE); assertTrue(false); } catch (Exception e) { }
	try { csi.getTTCLLookupBehavior(); assertTrue(false); } catch (Exception e) { }
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
