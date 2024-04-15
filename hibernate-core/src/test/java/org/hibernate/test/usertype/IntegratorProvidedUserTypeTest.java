/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.usertype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christian Beikov
 */
public class IntegratorProvidedUserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				StringWrapperTestEntity.class
		};
	}

	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		builder.applyClassLoader( new TestClassLoader() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-14408" )
	public void test() {
		Type type = sessionFactory().getMetamodel().entityPersister( StringWrapperTestEntity.class )
				.getPropertyType( "stringWrapper" );
		Assert.assertTrue( "Type was initialized too early i.e. before integrators were run", type instanceof CustomType );
	}

	@Entity
	public static class StringWrapperTestEntity implements Serializable {
		@Id
		private Integer id;
		private StringWrapper stringWrapper;
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
			InputStream inputStream = originalClass.getResourceAsStream( originalPath);
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
