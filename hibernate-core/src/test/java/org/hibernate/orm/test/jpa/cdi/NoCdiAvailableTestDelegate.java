/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cdi;

import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.util.ServiceRegistryUtil;

/**
 * @author Steve Ebersole
 */
public class NoCdiAvailableTestDelegate {
	public static EntityManagerFactory passingNoBeanManager() {
		return new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				ServiceRegistryUtil.createBaseSettings()
		);
	}

	public static void passingBeanManager() {
		Map<String, Object> settings = ServiceRegistryUtil.createBaseSettings();
		settings.put( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, new Object() );
		new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				settings
		).close();
	}
}
