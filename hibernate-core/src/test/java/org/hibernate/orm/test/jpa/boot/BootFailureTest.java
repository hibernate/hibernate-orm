/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.boot;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that a dump configuration error results in an exception being
 * thrown even when booting via the standard JPA boostrap API.
 *
 * @author Andrea Boriero
 * @author Sanne Grinovero
 */
public class BootFailureTest {

	@Test
	public void exceptionOnIllegalPUTest() {
		Assertions.assertThrows( ServiceException.class, () ->
				bootstrapPersistenceUnit( "IntentionallyBroken" ) );
	}

	@Test
	public void exceptionOnIllegalPUWithoutProviderTest() {
		Assertions.assertThrows( ServiceException.class, () ->
				bootstrapPersistenceUnit( "IntentionallyBrokenWihoutExplicitProvider" ) );
	}

	private void bootstrapPersistenceUnit(final String puName) {
		final Map<String, Object> properties = new HashMap<>();
		properties.put( AvailableSettings.CLASSLOADERS, Arrays.asList( new TestClassLoader() ) );
		EntityManagerFactory broken = Persistence.createEntityManagerFactory(
				puName,
				properties
		);
		if ( broken != null ) {
			broken.close();
		}
	}

	private static class TestClassLoader extends ClassLoader {
		static final List<URL> urls = Arrays.asList( ClassLoaderServiceTestingImpl.INSTANCE.locateResource(
				"org/hibernate/orm/test/jpa/boot/META-INF/persistence.xml" ) );

		@Override
		protected Enumeration<URL> findResources(String name) {
			return name.equals( "META-INF/persistence.xml" ) ?
					Collections.enumeration( urls ) :
					Collections.emptyEnumeration();
		}
	}
}

