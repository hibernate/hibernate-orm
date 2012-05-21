package org.hibernate.test.service;

import org.hibernate.service.classloading.internal.ClassLoaderServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.Entity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    private static class TestClassLoader extends ClassLoader {
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
