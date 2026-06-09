/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.util.ArrayList;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
public class ResolvedMappingSettingsTests {
	@Test
	void collectionsAreDefensivelyExposed() {
		final var cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		cacheRegionDefinitions.add( new CacheRegionDefinition(
				CacheRegionType.ENTITY,
				"AnEntity",
				"read-write",
				"entities",
				false
		) );

		final ResolvedMappingSettings settings = new ResolvedMappingSettings(
				true,
				false,
				FetchType.LAZY,
				cacheRegionDefinitions
		);

		cacheRegionDefinitions.clear();

		assertThat( settings.cacheRegionDefinitions() ).hasSize( 1 );
		assertThatThrownBy( () -> settings.cacheRegionDefinitions().clear() )
				.isInstanceOf( UnsupportedOperationException.class );
	}
}
