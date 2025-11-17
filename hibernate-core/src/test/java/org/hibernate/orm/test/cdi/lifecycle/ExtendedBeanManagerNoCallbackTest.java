/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.lifecycle;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * We pass an ExtendedBeanManager but never "initialize" it.  This might happen when the
 * environment provides an ExtendedBeanManager but there is no CDI needed for the app
 */
public class ExtendedBeanManagerNoCallbackTest {
	@Test
	public void tryIt() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, new ExtendedBeanManagerImpl() )
				.build();

		// this will trigger trying to locate IdentifierGeneratorFactory as a managed-bean
		try (SessionFactory sf = new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity.class )
				.buildMetadata()
				.buildSessionFactory()) {
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TheEntity")
	@Table(name = "TheEntity")
	public static class TheEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
	}

	public static class ExtendedBeanManagerImpl implements ExtendedBeanManager {
		private LifecycleListener lifecycleListener;

		@Override
		public void registerLifecycleListener(LifecycleListener lifecycleListener) {
			assert this.lifecycleListener == null;
			this.lifecycleListener = lifecycleListener;
		}

		public void notify(BeanManager ready) {
			lifecycleListener.beanManagerInitialized( ready );
		}
	}
}
