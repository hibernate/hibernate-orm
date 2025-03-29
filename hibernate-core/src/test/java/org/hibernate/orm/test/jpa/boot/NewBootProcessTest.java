/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import jakarta.persistence.EntityManagerFactory;
import java.net.URL;
import java.util.Map;

import org.hibernate.orm.test.jpa.xml.versions.JpaXsdVersionsTest;
import org.hibernate.jpa.HibernatePersistenceProvider;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class NewBootProcessTest {
	@Test
	public void basicNewBootProcessTest() {
		Map settings = ServiceRegistryUtil.createBaseSettings();

		HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();
		final EntityManagerFactory emf = persistenceProvider.createContainerEntityManagerFactory(
				new JpaXsdVersionsTest.PersistenceUnitInfoImpl( "my-test" ) {
					@Override
					public URL getPersistenceUnitRootUrl() {
						// just get any known url...
						return HibernatePersistenceProvider.class.getResource( "/org/hibernate/jpa/persistence_1_0.xsd" );
					}
				},
				settings
		);
		emf.close();
	}
}
