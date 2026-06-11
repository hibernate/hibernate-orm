/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.util.LinkedHashMap;

import org.hibernate.boot.settings.ResolvedBootstrapSettings;

import org.junit.jupiter.api.Test;

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
				true
		) ).isInstanceOf( NullPointerException.class );
	}

	@Test
	void collectionsAreDefensivelyExposed() {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( "hibernate.example", "original" );

		final ResolvedBootstrapSettings settings = new ResolvedBootstrapSettings(
				configurationValues,
				true
		);

		configurationValues.put( "hibernate.example", "changed" );

		assertThat( settings.configurationValues() ).containsEntry( "hibernate.example", "original" );
		assertThatThrownBy( () -> settings.configurationValues().put( "another", "value" ) )
				.isInstanceOf( UnsupportedOperationException.class );
	}
}
