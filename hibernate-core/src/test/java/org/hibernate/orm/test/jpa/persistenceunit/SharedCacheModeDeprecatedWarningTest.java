/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import java.util.Collections;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@JiraKey(value = "HHH-15768")
public class SharedCacheModeDeprecatedWarningTest {

	private TriggerOnPrefixLogListener trigger;

	@BeforeEach
	public void setUp() {
		trigger = new TriggerOnPrefixLogListener( "HHH90000021" );
		LogInspectionHelper.registerListener( trigger, DeprecationLogger.DEPRECATION_LOGGER );
	}

	@AfterEach
	public void tearDown() {
		trigger.reset();
	}

	@AfterAll
	public static void reset() {
		LogInspectionHelper.clearAllListeners( DeprecationLogger.DEPRECATION_LOGGER );
	}

	@Test
	public void testPersistenceUnitSharedCacheSetting() {
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public SharedCacheMode getSharedCacheMode() {
				return SharedCacheMode.UNSPECIFIED;
			}
		};
		EntityManagerFactoryBuilder builder = Bootstrap.getEntityManagerFactoryBuilder( adapter, null );
		builder.cancel();
		assertFalse( trigger.wasTriggered(), "Log message was triggered" );
	}

	@Test
	public void testPersistenceUnitValidationSetting() {
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public ValidationMode getValidationMode() {
				return ValidationMode.NONE;
			}
		};
		EntityManagerFactoryBuilder builder = Bootstrap.getEntityManagerFactoryBuilder( adapter, null );
		builder.cancel();
		assertFalse( trigger.wasTriggered(), "Log message was triggered" );
	}

	@Test
	public void testJakartaSharedCacheSetting() {
		verifyCacheSetting( AvailableSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.UNSPECIFIED );
		assertFalse( trigger.wasTriggered(), "Log message was triggered" );
	}

	@Test
	public void testJakartaValidationSetting() {
		verifyCacheSetting( AvailableSettings.JAKARTA_VALIDATION_MODE, ValidationMode.NONE );
		assertFalse( trigger.wasTriggered(), "Log message was triggered" );
	}

	@Test
	public void testJpaSharedCacheSetting() {
		verifyCacheSetting( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.UNSPECIFIED );
		assertTrue( trigger.wasTriggered(), "Log message was not triggered" );
	}

	@Test
	public void testJpaValidationSetting() {
		verifyCacheSetting( AvailableSettings.JPA_VALIDATION_MODE, ValidationMode.NONE );
		assertTrue( trigger.wasTriggered(), "Log message was not triggered" );
	}

	private void verifyCacheSetting(String settingName, Object value) {
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		EntityManagerFactoryBuilderImpl builder;
		builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
				empty,
				Collections.singletonMap( settingName, value )
		);
		builder.cancel();
	}
}
