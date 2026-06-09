/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source;

import java.util.Map;

import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class BootstrapSourceContributionsTests {
	@Test
	void adaptsHibernatePersistenceConfigurationSources(ServiceRegistryScope registryScope) {
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.managedClass( SimpleEntity.class );
		persistenceConfiguration.mappingFile( "mappings/complete/simple-complete.xml" );
		final var bootstrapSettings = BootstrapSettingsResolver.resolve(
				persistenceConfiguration,
				Map.of()
		);

		final var sourceContributions = BootstrapSourceContributions.from(
				persistenceConfiguration,
				bootstrapSettings,
				registryScope.getRegistry().requireService( ClassLoaderService.class )
		);

		assertThat( sourceContributions.managedClasses() ).containsExactly( SimpleEntity.class );
		assertThat( sourceContributions.managedClassNames() ).isEmpty();
		assertThat( sourceContributions.packageNames() ).isEmpty();
		assertThat( sourceContributions.mappingFiles() ).containsExactly( "mappings/complete/simple-complete.xml" );
		assertThat( sourceContributions.mappingFileUris() ).isEmpty();
	}
}
