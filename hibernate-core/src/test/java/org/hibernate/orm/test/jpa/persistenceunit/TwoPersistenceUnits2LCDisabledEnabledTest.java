/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import java.util.List;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gail Badner
 */
public class TwoPersistenceUnits2LCDisabledEnabledTest {

	@Test
	@JiraKey( value = "HHH-11516" )
	@Jpa(
			annotatedClasses = {TwoPersistenceUnits2LCDisabledEnabledTest.AnEntity.class},
			integrationSettings = {
					@Setting(name = CacheSettings.JAKARTA_SHARED_CACHE_MODE, value = "ENABLE_SELECTIVE"),
					@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
			},
			settingProviders = {
					@SettingProvider(
							settingName = AvailableSettings.LOADED_CLASSES,
							provider = LoadedClassesSettingProvider.class
					)
			}
	)
	public void testEnabled(EntityManagerFactoryScope scope) {
		final EntityPersister persister =
				scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
						.getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		assertNotNull( persister.getCacheAccessStrategy() );
	}

	@Test
	@JiraKey( value = "HHH-11516" )
	@Jpa(
			annotatedClasses = {TwoPersistenceUnits2LCDisabledEnabledTest.AnEntity.class},
			integrationSettings = {
					@Setting(name = CacheSettings.JAKARTA_SHARED_CACHE_MODE, value = "ENABLE_SELECTIVE"),
					@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false")
			},
			settingProviders = {
					@SettingProvider(
							settingName = AvailableSettings.LOADED_CLASSES,
							provider = LoadedClassesSettingProvider.class
					)
			}
	)
	public void testDisabled(EntityManagerFactoryScope scope) {
		final EntityPersister persister =
				scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
						.getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		assertNull( persister.getCacheAccessStrategy() );
	}

	public static class LoadedClassesSettingProvider implements SettingProvider.Provider<List<Class<?>>> {
		@Override
		public List<Class<?>> getSetting() {
			return List.of(AnEntity.class);
		}
	}

	@Cacheable
	@Entity( name = "AnEntity" )
	public static class AnEntity {
		@Id
		private Long id;
	}
}
