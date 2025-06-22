/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import jakarta.persistence.Entity;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem V. Navrotskiy
 * @author Emmanuel Bernard
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
	@JiraKey(value = "HHH-8363")
	public void testStoppableClassLoaderService() {
		final BootstrapServiceRegistryBuilder bootstrapBuilder = new BootstrapServiceRegistryBuilder();
		bootstrapBuilder.applyClassLoader( new TestClassLoader() );
		final ServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrapBuilder.build() ).build();

		final TypeContributor contributor1 = getTypeContributorServices( serviceRegistry );
		assertThat( contributor1 ).isNotNull();

		final TypeContributor contributor2 = getTypeContributorServices( serviceRegistry );
		assertThat( contributor2 ).isNotNull();

		assertThat( contributor1 ).isSameAs( contributor2 );

		StandardServiceRegistryBuilder.destroy( serviceRegistry );

		try {
			getTypeContributorServices( serviceRegistry );
			Assert.fail("Should have thrown an HibernateException -- the ClassLoaderService instance was closed.");
		}
		catch (HibernateException e) {
			Assert.assertEquals( "The ClassLoaderService cannot be reused (this instance was stopped already)",
					e.getMessage() );
		}
	}

	private TypeContributor getTypeContributorServices(ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		final Collection<TypeContributor> typeContributors = classLoaderService.loadJavaServices( TypeContributor.class );
		assertThat( typeContributors ).hasSize( 1 );
		return typeContributors.iterator().next();
	}

	private static class TestClassLoader extends ClassLoader {

		/**
		 * testStoppableClassLoaderService() needs a custom JDK service implementation.  Rather than using a real one
		 * on the test classpath, force it in here.
		 */
		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			if (name.equals( "META-INF/services/org.hibernate.boot.model.TypeContributor" )) {
				final URL serviceUrl = ConfigHelper.findAsResource(
						"org/hibernate/orm/test/service/org.hibernate.boot.model.TypeContributor" );
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
				return java.util.Collections.enumeration( java.util.Collections.<URL>emptyList() );
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
