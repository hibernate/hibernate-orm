/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorization;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * Tests that {@link DomainModelCategorizationCollector} correctly categorizes
 * entity inheritance hierarchies, including non-root entity subclasses.
 */
public class EntityInheritanceCategorizationTest {

	private BootstrapContextImpl bootstrapContext;

	@AfterEach
	void tearDown() {
		if ( bootstrapContext != null ) {
			bootstrapContext.close();
		}
	}

	private DomainModelCategorizationCollector collectFrom(Class<?>... classes) {
		final ModelsContext modelsContext = createBuildingContext( classes );
		bootstrapContext = new BootstrapContextImpl();
		final GlobalRegistrationsImpl globalRegistrations =
				new GlobalRegistrationsImpl( modelsContext, bootstrapContext );
		final DomainModelCategorizationCollector collector =
				new DomainModelCategorizationCollector( globalRegistrations, modelsContext );
		modelsContext.getClassDetailsRegistry().forEachClassDetails( collector::apply );
		return collector;
	}

	@Entity
	public static class ParentEntity {
		@Id
		private Long id;
	}

	@Entity
	public static class ChildEntity extends ParentEntity {
		private String name;
	}

	@Test
	void parentIsRootChildIsSubEntity() {
		final DomainModelCategorizationCollector collector = collectFrom(
				ParentEntity.class,
				ChildEntity.class
		);

		assertThat( collector.getRootEntities() )
				.extracting( ClassDetails::getClassName )
				.containsExactly( ParentEntity.class.getName() );

		assertThat( collector.getEntitySubclasses() )
				.extracting( ClassDetails::getClassName )
				.containsExactly( ChildEntity.class.getName() );

		assertThat( collector.getAllEntities() )
				.extracting( ClassDetails::getClassName )
				.containsExactlyInAnyOrder(
						ParentEntity.class.getName(),
						ChildEntity.class.getName()
				);
	}

	@MappedSuperclass
	public static class BaseMappedSuperclass {
		@Id
		private Long id;
	}

	@Entity
	public static class RootEntityWithSuperclass extends BaseMappedSuperclass {
		private String name;
	}

	@Test
	void mappedSuperclassIsNotRootEntityAndNoSubEntities() {
		final DomainModelCategorizationCollector collector = collectFrom(
				BaseMappedSuperclass.class,
				RootEntityWithSuperclass.class
		);

		assertThat( collector.getRootEntities() )
				.extracting( ClassDetails::getClassName )
				.containsExactly( RootEntityWithSuperclass.class.getName() );

		assertThat( collector.getEntitySubclasses() ).isEmpty();

		assertThat( collector.getMappedSuperclasses() )
				.containsKey( BaseMappedSuperclass.class.getName() );
	}
}
