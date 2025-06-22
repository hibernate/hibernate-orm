/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.lifecycle;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerExtendedAccessImpl;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.CDI_BEAN_MANAGER;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_CDI_BEAN_MANAGER;
import static org.hibernate.jpa.boot.spi.Bootstrap.getEntityManagerFactoryBuilder;

/**
 * @author Steve Ebersole
 */
public class ExtendedBeanManagerSmokeTests {

	@Test
	public void testIntegrationSetting() {
		verifyIntegrationSetting( JAKARTA_CDI_BEAN_MANAGER );
		verifyIntegrationSetting( CDI_BEAN_MANAGER );
	}

	private static void verifyIntegrationSetting(String settingName) {
		final ExtendedBeanManagerImpl ref = new ExtendedBeanManagerImpl();
		assertThat( ref.lifecycleListener ).isNull();

		final EntityManagerFactoryBuilder emfb = getEntityManagerFactoryBuilder(
				new PersistenceUnitInfoAdapter(),
				integrationSettings( settingName, ref )
		);

		assertApplied( ref, emfb.build() );
	}

	@Test
	public void testUserSetting() {
		verifyUserSettingWorks( JAKARTA_CDI_BEAN_MANAGER );
		verifyUserSettingWorks( CDI_BEAN_MANAGER );
	}

	private static void verifyUserSettingWorks(String settingName) {
		final ExtendedBeanManagerImpl ref = new ExtendedBeanManagerImpl();
		assertThat( ref.lifecycleListener ).isNull();

		final EntityManagerFactoryBuilder emfb = getEntityManagerFactoryBuilder(
				new PersistenceUnitInfoAdapter(),
				integrationSettings( settingName, ref )
		);

		assertApplied( ref, emfb.build() );
	}

	private static Map<String, Object> integrationSettings(String settingName, Object value) {
		final Map<String, Object> settings = ServiceRegistryUtil.createBaseSettings();
		settings.put( settingName, value );
		return settings;
	}

	private static void assertApplied(ExtendedBeanManagerImpl ref, EntityManagerFactory emf) {
		final SessionFactoryImplementor sfi = emf.unwrap( SessionFactoryImplementor.class );
		final ManagedBeanRegistry beanRegistry = sfi.getManagedBeanRegistry();
		assertThat( beanRegistry.getBeanContainer() ).isInstanceOf( CdiBeanContainerExtendedAccessImpl.class );

		final CdiBeanContainerExtendedAccessImpl extensionWrapper = (CdiBeanContainerExtendedAccessImpl) beanRegistry.getBeanContainer();
		assertThat( extensionWrapper.getBeanManager() ).isNull();

		ref.notify( null );
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
