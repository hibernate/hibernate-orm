/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import java.util.List;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { NestedEmbeddableDefaultAccessTests.MyEntity.class } )
@SessionFactory( exportSchema = false )
@JiraKey( "HHH-14703" )
public class NestedEmbeddableDefaultAccessTests {
	@Test
	public void verifyEmbeddedMapping(DomainModelScope scope) {
		scope.withHierarchy( MyEntity.class, (descriptor) -> {
			final Property outerEmbedded = descriptor.getProperty( "outerEmbeddable" );
			verifyMapping( (Component) outerEmbedded.getValue() );
		} );
	}

	@Test
	@JiraKey( "HHH-14063" )
	public void verifyElementCollectionMapping(DomainModelScope scope) {
		scope.withHierarchy( MyEntity.class, (descriptor) -> {
			final Property outerEmbeddedList = descriptor.getProperty( "outerEmbeddableList" );
			verifyMapping( (Component) ( (Collection) outerEmbeddedList.getValue() ).getElement() );
		} );
	}

	private void verifyMapping(Component outerEmbeddable) {
		final Property outerData = outerEmbeddable.getProperty( "outerData" );
		final BasicValue outerDataMapping = (BasicValue) outerData.getValue();
		final Property nestedEmbedded = outerEmbeddable.getProperty( "nestedEmbeddable" );
		final Component nestedEmbeddable = (Component) nestedEmbedded.getValue();
		final Property nestedData = nestedEmbeddable.getProperty( "nestedData" );
		final BasicValue nestedDataMapping = (BasicValue) nestedData.getValue();
		final Property nestedEnum = nestedEmbeddable.getProperty( "nestedEnum" );
		final BasicValue nestedEnumMapping = (BasicValue) nestedEnum.getValue();

		assertThat( outerDataMapping.getColumn().getText() ).isEqualTo( "outer_data" );
		assertThat( outerDataMapping.getJpaAttributeConverterDescriptor() ).isNotNull();

		assertThat( nestedDataMapping.getColumn().getText() ).isEqualTo( "nested_data" );

		// Check for HHH-14703
		assertThat( nestedEnumMapping.getEnumeratedType() ).isEqualTo( EnumType.STRING );
	}

	@Entity( name = "MyEntity" )
	@Table( name = "MyEntity" )
	public static class MyEntity {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Embedded
		private OuterEmbeddable outerEmbeddable;
		@ElementCollection
		private List<OuterEmbeddable> outerEmbeddableList;

		private MyEntity() {
			// for use by Hibernate
		}

		public MyEntity(Integer id, String name) {
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

		public OuterEmbeddable getOuterEmbeddable() {
			return outerEmbeddable;
		}

		public void setOuterEmbeddable(OuterEmbeddable outerEmbeddable) {
			this.outerEmbeddable = outerEmbeddable;
		}
	}

	@Embeddable
	public static class OuterEmbeddable {
		@Convert( converter = SillyConverter.class )
		@Column( name = "outer_data" )
		private String outerData;

		@Embedded
		private NestedEmbeddable nestedEmbeddable;
	}

	@Embeddable
	public static class NestedEmbeddable {
		@Convert( converter = SillyConverter.class )
		@Column( name = "nested_data" )
		private String nestedData;
		@Enumerated(EnumType.STRING)
		@Column( name = "nested_enum" )
		private MyEnum nestedEnum;
	}

	public enum MyEnum {
		A,
		B
	}
}
