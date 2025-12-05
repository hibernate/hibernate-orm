/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.query.internal.QueryInterpretationCacheDisabledImpl;
import org.hibernate.query.internal.QueryInterpretationCacheStandardImpl;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.internal.QueryEngineImpl;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for configuration validation in QueryEngineImpl.
 * Tests the consistency checks between QUERY_PLAN_CACHE_ENABLED and QUERY_PLAN_CACHE_MAX_SIZE.
 */
@Jira("HHH-19646")
public class QueryEngineImplConfigValidationTest {

	@Test
	public void testCacheEnabledWithValidMaxSize() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( QuerySettings.QUERY_PLAN_CACHE_ENABLED, true );
		settings.put( QuerySettings.QUERY_PLAN_CACHE_MAX_SIZE, 100 );
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			QueryInterpretationCache interpretationCache = assertDoesNotThrow( () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			testCacheEnabled( interpretationCache );
		}
	}

	@Test
	public void testCacheEnabledWithDefaultMaxSize() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( QuerySettings.QUERY_PLAN_CACHE_ENABLED, true );
		// No explicit max size - should use default
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			QueryInterpretationCache interpretationCache = assertDoesNotThrow( () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			testCacheEnabled( interpretationCache );
		}
	}

	@Test
	public void testCacheDisabledWithNoMaxSize() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( QuerySettings.QUERY_PLAN_CACHE_ENABLED, false );
		// No explicit max size - should work fine
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			QueryInterpretationCache interpretationCache = assertDoesNotThrow( () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			testCacheDisabled( interpretationCache );
		}
	}

	@Test
	public void testCacheDisabledWithPositiveMaxSize() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( QuerySettings.QUERY_PLAN_CACHE_ENABLED, false );
		settings.put( QuerySettings.QUERY_PLAN_CACHE_MAX_SIZE, 100 );
		//Explicit max size, with cache explicitly disabled is an inconsistency we want to flag
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			ConfigurationException exception = assertThrows( ConfigurationException.class, () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			assertTrue( exception.getMessage().matches(
					"Inconsistent configuration: '" + QuerySettings.QUERY_PLAN_CACHE_MAX_SIZE + "' can only be set to a value greater than zero when '" + QuerySettings.QUERY_PLAN_CACHE_ENABLED + "' is enabled" ) );
		}
	}

	@Test
	public void testCacheDisabledWithZeroMaxSize() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( QuerySettings.QUERY_PLAN_CACHE_ENABLED, false );
		settings.put( QuerySettings.QUERY_PLAN_CACHE_MAX_SIZE, 0 );
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			QueryInterpretationCache interpretationCache = assertDoesNotThrow( () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			testCacheDisabled( interpretationCache );
		}
	}

	@Test
	public void testNegativeMaxSize() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( QuerySettings.QUERY_PLAN_CACHE_ENABLED, true );
		settings.put( QuerySettings.QUERY_PLAN_CACHE_MAX_SIZE, -1 );
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			ConfigurationException exception = assertThrows( ConfigurationException.class, () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			assertTrue( exception.getMessage().contains( "can't be set to a negative value" ) );
		}
	}

	@Test
	public void testDefaultConfigurationWorks() {
		Map<String, Object> settings = new HashMap<>();
		// No explicit settings - should use defaults and work fine
		try (ServiceRegistry serviceRegistry = newRegistry()) {
			QueryInterpretationCache interpretationCache = assertDoesNotThrow( () ->
					QueryEngineImpl.buildInterpretationCache( serviceRegistry, settings )
			);
			testCacheEnabled( interpretationCache );
		}
	}

	private static StandardServiceRegistry newRegistry() {
		return new StandardServiceRegistryBuilder().build();
	}

	private void testCacheEnabled(QueryInterpretationCache interpretationCache) {
		assertInstanceOf( QueryInterpretationCacheStandardImpl.class, interpretationCache,
				"Default interpretation cache should be used" );
	}

	private void testCacheDisabled(QueryInterpretationCache interpretationCache) {
		assertInstanceOf( QueryInterpretationCacheDisabledImpl.class, interpretationCache,
				"Cache should have been disabled" );
	}
}
