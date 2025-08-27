/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.util.Date;

import org.hibernate.annotations.Mutability;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests applying {@link Mutability} to an {@link jakarta.persistence.AttributeConverter}.
 *
 * Here we just verify that has the same effect as {@link Mutability} directly on the attribute
 * in terms of configuring the boot model references.
 *
 * @see ImmutableConvertedBaselineTests
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel( annotatedClasses = ImmutabilityConverterTests.TestEntity.class )
public class ImmutabilityConverterTests {
	@Test
	void verifyMetamodel(DomainModelScope scope) {
		scope.withHierarchy( TestEntity.class, (entity) -> {
			final Property theDateProperty = entity.getProperty( "theDate" );
			assertThat( theDateProperty ).isNotNull();
			assertThat( theDateProperty.isUpdatable() ).isTrue();

			final BasicValue basicValue = (BasicValue) theDateProperty.getValue();
			final BasicValue.Resolution<?> resolution = basicValue.resolve();
			assertThat( resolution.getMutabilityPlan().isMutable() ).isFalse();
		} );
	}

	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Convert( converter = ImmutabilityDateConverter.class )
		private Date theDate;

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
