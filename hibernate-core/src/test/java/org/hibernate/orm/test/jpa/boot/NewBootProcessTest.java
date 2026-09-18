/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
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
	public void basicNewBootProcessTest(@TempDir Path directory) throws Exception {
		final var root = directory.toUri().toURL();
		Map settings = ServiceRegistryUtil.createBaseSettings();

		HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();
		final EntityManagerFactory emf = persistenceProvider.createContainerEntityManagerFactory(
				new JpaXsdVersionsTest.PersistenceUnitInfoImpl( "my-test" ) {
					@Override
					public URL getPersistenceUnitRootUrl() {
						return root;
					}
				},
				settings
		);
		emf.close();
	}
}
