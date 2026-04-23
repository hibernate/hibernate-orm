/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorization;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
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
 * enum types from entity fields and java.* types from class hierarchies.
 */
public class EnumAndJavaTypeCategorizationTest {

	enum MyStatus {
		ACTIVE,
		INACTIVE
	}

	@Entity
	public static class EntityWithEnum {
		@Id
		private Long id;
		private MyStatus status;
	}

	@Entity
	public static class SerializableEntity implements Serializable {
		@Id
		private Long id;
	}

	@Entity
	public static class SimpleEntity {
		@Id
		private Long id;
	}

	@MappedSuperclass
	public static class MappedSuperclassWithEnum {
		@Id
		private Long id;
		private MyStatus status;
	}

	@Entity
	public static class ChildEntity extends MappedSuperclassWithEnum {
		private String name;
	}

	@Embeddable
	public static class EmbeddableWithEnum {
		private MyStatus status;
	}

	@Entity
	public static class EntityWithEmbeddedEnum {
		@Id
		private Long id;
		@Embedded
		private EmbeddableWithEnum embedded;
	}

	@Test
	void enumFieldOnEntityIsCollected() {
		final ModelsContext modelsContext = createBuildingContext(
				MyStatus.class,
				EntityWithEnum.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getEnumTypes() )
					.contains( MyStatus.class.getName() );
		}
	}

	@Test
	void enumFieldOnMappedSuperclassIsCollected() {
		final ModelsContext modelsContext = createBuildingContext(
				MyStatus.class,
				MappedSuperclassWithEnum.class,
				ChildEntity.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getEnumTypes() )
					.contains( MyStatus.class.getName() );
		}
	}

	@Test
	void enumFieldOnEmbeddableIsCollected() {
		final ModelsContext modelsContext = createBuildingContext(
				MyStatus.class,
				EmbeddableWithEnum.class,
				EntityWithEmbeddedEnum.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getEnumTypes() )
					.contains( MyStatus.class.getName() );
		}
	}

	@Test
	void entityWithoutEnumHasEmptyEnumTypes() {
		final ModelsContext modelsContext = createBuildingContext(
				SimpleEntity.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getEnumTypes() ).isEmpty();
		}
	}

	@Test
	void javaInterfaceOnEntityIsCollected() {
		final ModelsContext modelsContext = createBuildingContext(
				SerializableEntity.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getJavaTypes() )
					.contains( Serializable.class.getName() );
		}
	}

	@Test
	void simpleEntityHasEmptyJavaTypes() {
		final ModelsContext modelsContext = createBuildingContext(
				SimpleEntity.class
		);

		try (BootstrapContextImpl bootstrapContext = new BootstrapContextImpl()) {
			final GlobalRegistrationsImpl globalRegistrations =
					new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
			final DomainModelCategorizationCollector collector =
					new DomainModelCategorizationCollector( globalRegistrations, modelsContext );

			modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );

			assertThat( collector.getJavaTypes() ).isEmpty();
		}
	}
}
