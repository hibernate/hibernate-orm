/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.cdi.lifecycle;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

import org.junit.jupiter.api.Test;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * We pass an ExtendedBeanManager but never "initialize" it.  This might happen when the
 * environment provides an ExtendedBeanManager but there is no CDI needed for the app
 */
public class ExtendedBeanManagerNoCallbackTest {
	@Test
	public void tryIt() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, new ExtendedBeanManagerImpl() )
				.build();

		try {
			// this will trigger trying to locate IdentifierGeneratorFactory as a managed-bean
			new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata()
					.buildSessionFactory();
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
