package org.hibernate.test.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;

import javax.persistence.Entity;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Artem V. Navrotskiy
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ClassLoaderServiceImplTest {
    /**
     * Test for bug: HHH-7084
     */
    @Test
    public void testSystemClassLoaderNotOverriding() throws IOException, ClassNotFoundException {
        Class<?> testClass = Entity.class;

        // Check that class is accessible by SystemClassLoader.
        ClassLoader.getSystemClassLoader().loadClass(testClass.getName());

        // Create ClassLoader with overridden class.
        TestClassLoader anotherLoader = new TestClassLoader();
        anotherLoader.overrideClass(testClass);
        Class<?> anotherClass = anotherLoader.loadClass(testClass.getName());
        Assert.assertNotSame( testClass, anotherClass );

        // Check ClassLoaderServiceImpl().classForName() returns correct class (not from current ClassLoader).
        ClassLoaderServiceImpl loaderService = new ClassLoaderServiceImpl(anotherLoader);
        Class<Object> objectClass = loaderService.classForName(testClass.getName());
        Assert.assertSame("Should not return class loaded from the parent classloader of ClassLoaderServiceImpl",
				objectClass, anotherClass);
    }
    
    /**
     * HHH-8363 discovered multiple leaks within CLS.  Most notably, it wasn't getting GC'd due to holding
     * references to ServiceLoaders.  Ensure that the addition of Stoppable functionality cleans up properly.
     * 
     * TODO: Is there a way to test that the ServiceLoader was actually reset?
     */
    @Test
    @TestForIssue(jiraKey = "HHH-8363")
    public void testStoppableClassLoaderService() {
    	final BootstrapServiceRegistryBuilder bootstrapBuilder = new BootstrapServiceRegistryBuilder();
    	bootstrapBuilder.with( new TestClassLoader() );
    	final ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder( bootstrapBuilder.build() ).build();
    	final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
    	
    	TestIntegrator testIntegrator1 = findTestIntegrator( classLoaderService );
    	assertNotNull( testIntegrator1 );
    	
    	TestIntegrator testIntegrator2 = findTestIntegrator( classLoaderService );
    	assertNotNull( testIntegrator2 );
    	
    	assertSame( testIntegrator1, testIntegrator2 );
    	
    	StandardServiceRegistryBuilder.destroy( serviceRegistry );
    	
    	// Should return null -- aggregratedClassLoader blown away.
    	testIntegrator2 = findTestIntegrator( classLoaderService );
    	assertNull( testIntegrator2 );
    }
    
    private TestIntegrator findTestIntegrator(ClassLoaderService classLoaderService) {
    	final LinkedHashSet<Integrator> integrators = classLoaderService.loadJavaServices( Integrator.class );
    	for (Integrator integrator : integrators) {
    		if (integrator instanceof TestIntegrator) {
    			return (TestIntegrator) integrator;
    		}
    	}
    	return null;
    }

    private static class TestClassLoader extends ClassLoader {
    	
    	/**
    	 * testStoppableClassLoaderService() needs a custom JDK service implementation.  Rather than using a real one
    	 * on the test classpath, force it in here.
    	 */
    	@Override
        protected Enumeration<URL> findResources(String name) throws IOException {
    		if (name.equals( "META-INF/services/org.hibernate.integrator.spi.Integrator" )) {
    			final URL serviceUrl = ConfigHelper.findAsResource(
    					"org/hibernate/test/service/org.hibernate.integrator.spi.Integrator" );
    			return new Enumeration<URL>() {
        			boolean hasMore = true;
        			
    				@Override
    				public boolean hasMoreElements() {
    					return hasMore;
    				}

    				@Override
    				public URL nextElement() {
    					hasMore = false;
    					return serviceUrl;
    				}
    			};
    		}
    		else {
    			return java.util.Collections.emptyEnumeration();
    		}
        }
    	
        /**
         * Reloading class from binary file.
         *
         * @param originalClass Original class.
         * @throws IOException .
         */
        public void overrideClass(final Class<?> originalClass) throws IOException {
            String originalPath = "/" + originalClass.getName().replaceAll("\\.", "/") + ".class";
            InputStream inputStream = originalClass.getResourceAsStream(originalPath);
            Assert.assertNotNull(inputStream);
            try {
                byte[] data = toByteArray( inputStream );
                defineClass(originalClass.getName(), data, 0, data.length);
            } finally {
                inputStream.close();
            }
        }

		private byte[] toByteArray(InputStream inputStream) throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int read;
			byte[] slice = new byte[2000];
			while ( (read = inputStream.read(slice, 0, slice.length) ) != -1) {
			  out.write( slice, 0, read );
			}
			out.flush();
			return out.toByteArray();
		}
    }
}
