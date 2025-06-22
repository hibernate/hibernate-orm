/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.util.Date;

import org.hibernate.annotations.Immutable;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests applying {@link Immutable} to an {@link jakarta.persistence.AttributeConverter}.
 *
 * Here we just verify that has the same effect as {@link Immutable} directly on the attribute
 * in terms of configuring the boot model references.
 *
 * @see ImmutableConvertedBaselineTests
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel( annotatedClasses = ImmutableConverterTests.TestEntity.class )
@SessionFactory
public class ImmutableConverterTests {
	@Test
	void verifyMetamodel(DomainModelScope scope) {
		scope.withHierarchy( TestEntity.class, (entity) -> {
			{
				final Property property = entity.getProperty( "mutableDate" );
				assertThat( property ).isNotNull();
				assertThat( property.isUpdateable() ).isTrue();

				final BasicValue basicValue = (BasicValue) property.getValue();
				final BasicValue.Resolution<?> resolution = basicValue.resolve();
				assertThat( resolution.getMutabilityPlan().isMutable() ).isTrue();
			}

			{
				final Property property = entity.getProperty( "immutableDate" );
				assertThat( property ).isNotNull();
				assertThat( property.isUpdateable() ).isTrue();

				final BasicValue basicValue = (BasicValue) property.getValue();
				final BasicValue.Resolution<?> resolution = basicValue.resolve();
				assertThat( resolution.getMutabilityPlan().isMutable() ).isFalse();
			}
		} );
	}

	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Convert( converter = ImmutableDateConverter.class )
		private Date immutableDate;
		private Date mutableDate;

		private TestEntity() {
			// for use by Hibernate
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
