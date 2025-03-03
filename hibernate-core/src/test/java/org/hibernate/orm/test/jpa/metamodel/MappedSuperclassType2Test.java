/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Specifically see if we can access a MappedSuperclass via Metamodel that is not part of a entity hierarchy
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
public class MappedSuperclassType2Test {

	@Test
	@JiraKey( value = "HHH-8534" )
	@FailureExpected( jiraKey = "HHH-8534" )
	public void testMappedSuperclassAccessNoEntity() {
		// stupid? yes.  tck does it? yes.

		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				// pass in a MappedSuperclass that is not used in any entity hierarchy
				return Arrays.asList( SomeMappedSuperclass.class.getName() );
			}
		};

		final Map settings = ServiceRegistryUtil.createBaseSettings();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		try {
			ManagedType<SomeMappedSuperclass> type = emf.getMetamodel().managedType( SomeMappedSuperclass.class );
			// the issue was in regards to throwing an exception, but also check for nullness
			assertNotNull( type );
		}
		finally {
			emf.close();
		}
	}
}
