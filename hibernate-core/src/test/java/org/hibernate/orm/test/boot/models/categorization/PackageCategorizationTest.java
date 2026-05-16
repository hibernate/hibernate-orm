/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorization;

import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * Tests that {@link DomainModelCategorizationCollector} correctly collects
 * package names from {@code package-info} classes.
 */
public class PackageCategorizationTest {

	@Test
	void packageInfoClassIsCollected() {
		// The package-info class in pkgannotated has @FilterDef
		final Class<?> packageInfoClass;
		try {
			packageInfoClass = Class.forName(
					"org.hibernate.orm.test.boot.models.categorization.pkgannotated.package-info" );
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException( e );
		}

		final ModelsContext modelsContext = createBuildingContext( packageInfoClass );

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getPackageNames() )
					.contains( "org.hibernate.orm.test.boot.models.categorization.pkgannotated" );
		}
	}

	@Test
	void regularClassDoesNotProducePackageName() {
		final ModelsContext modelsContext = createBuildingContext( SimpleEntity.class );

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getPackageNames() ).isEmpty();
		}
	}

	@Entity
	public static class SimpleEntity {
		@Id
		private Long id;
	}
}
