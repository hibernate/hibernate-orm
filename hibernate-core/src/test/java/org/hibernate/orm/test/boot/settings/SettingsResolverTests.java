/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.util.Map;

import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SettingsResolverTests {
	@Test
	void facadeResolvesPhaseSettingsBuckets(ServiceRegistryScope registryScope) {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				Map.of(
						MappingSettings.XML_MAPPING_ENABLED, false,
						PersistenceSettings.SESSION_FACTORY_NAME, "settings-facade"
				),
				true
		);
		final var mappingSettings = SettingsResolver.resolveMappingSettings( bootstrapSettings, FetchType.LAZY );
		final var sessionFactorySettings = SettingsResolver.resolveSessionFactorySettings(
				bootstrapSettings,
				registryScope.getRegistry()
		);

		assertThat( bootstrapSettings.jpaBootstrap() ).isTrue();
		assertThat( mappingSettings.xmlMappingEnabled() ).isFalse();
		assertThat( mappingSettings.defaultToOneFetchType() ).isEqualTo( FetchType.LAZY );
		assertThat( sessionFactorySettings.sessionFactoryName() ).isEqualTo( "settings-facade" );
		assertThat( sessionFactorySettings.serviceRegistry() ).isSameAs( registryScope.getRegistry() );
	}
}
