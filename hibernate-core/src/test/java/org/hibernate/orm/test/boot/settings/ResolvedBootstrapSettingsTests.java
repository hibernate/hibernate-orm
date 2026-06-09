/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedMappingSettings;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
public class ResolvedBootstrapSettingsTests {
	@Test
	void requiredValuesAreEnforced() {
		assertThatThrownBy( () -> new ResolvedBootstrapSettings(
				null,
				true,
				new ResolvedMappingSettings( true, false, FetchType.EAGER, null )
		) ).isInstanceOf( NullPointerException.class );
		assertThatThrownBy( () -> new ResolvedBootstrapSettings(
				Map.of(),
				true,
				null
		) ).isInstanceOf( NullPointerException.class );
	}

	@Test
	void collectionsAreDefensivelyExposed() {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( "hibernate.example", "original" );

		final var cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		cacheRegionDefinitions.add( new CacheRegionDefinition(
				CacheRegionType.ENTITY,
				"AnEntity",
				"read-write",
				"entities",
				false
		) );

		final ResolvedBootstrapSettings settings = new ResolvedBootstrapSettings(
				configurationValues,
				true,
				new ResolvedMappingSettings( true, false, FetchType.LAZY, cacheRegionDefinitions )
		);

		configurationValues.put( "hibernate.example", "changed" );
		cacheRegionDefinitions.clear();

		assertThat( settings.configurationValues() ).containsEntry( "hibernate.example", "original" );
		assertThat( settings.mappingSettings().cacheRegionDefinitions() ).hasSize( 1 );
		assertThatThrownBy( () -> settings.configurationValues().put( "another", "value" ) )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> settings.mappingSettings().cacheRegionDefinitions().clear() )
				.isInstanceOf( UnsupportedOperationException.class );
	}
}
