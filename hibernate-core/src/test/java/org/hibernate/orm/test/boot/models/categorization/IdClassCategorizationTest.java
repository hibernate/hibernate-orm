/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorization;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * Tests that {@link DomainModelCategorizationCollector} correctly collects
 * {@link IdClass} references from entities.
 */
public class IdClassCategorizationTest {

	@Test
	void idClassIsCollected() {
		final ModelsContext modelsContext = createBuildingContext(
				MyIdClass.class,
				EntityWithIdClass.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getIdClasses() )
					.containsExactly( MyIdClass.class.getName() );
		}
	}

	@Test
	void entityWithoutIdClassHasEmptyIdClasses() {
		final ModelsContext modelsContext = createBuildingContext(
				SimpleEntity.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getIdClasses() ).isEmpty();
		}
	}

	public static class MyIdClass implements Serializable {
		private Long key1;
		private Long key2;
	}

	@Entity
	@IdClass(MyIdClass.class)
	public static class EntityWithIdClass {
		@Id
		private Long key1;
		@Id
		private Long key2;
	}

	@Entity
	public static class SimpleEntity {
		@Id
		private Long id;
	}
}
