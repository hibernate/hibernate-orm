/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
		assertThrows( ServiceException.class, () ->
				bootstrapPersistenceUnit( "IntentionallyBroken" ) );
	}

	@Test
	public void exceptionOnIllegalPUWithoutProviderTest() {
		assertThrows( ServiceException.class, () ->
				bootstrapPersistenceUnit( "IntentionallyBrokenWihoutExplicitProvider" ) );
	}

	@Test
	public void missingClassPUTest() {
		assertThrows( HibernateException.class, () ->
			bootstrapPersistenceUnit( "IntentionallyMissingClass" ),
			"A HibernateException due to a missing class in the persistence.xml should have been thrown"
			);
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
