/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		NestedEmbeddableInheritanceTest.Container.class,
		NestedEmbeddableInheritanceTest.NestedValue.class,
		NestedEmbeddableInheritanceTest.NestedSubtype.class,
		NestedEmbeddableInheritanceTest.Owner.class
})
@SessionFactory
public class NestedEmbeddableInheritanceTest {
	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void persistsAndLoadsNestedSubtype(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.persist( new Owner( 1, new Container( new NestedSubtype( "base", "subtype" ) ) ) )
		);

		scope.inTransaction( session -> {
			final Owner owner = session.find( Owner.class, 1 );
			assertThat( owner.container.nested ).isInstanceOf( NestedSubtype.class );
			assertThat( owner.container.nested.baseValue ).isEqualTo( "base" );
			assertThat( ( (NestedSubtype) owner.container.nested ).subtypeValue ).isEqualTo( "subtype" );
		} );
	}

	@Entity(name = "NestedEmbeddableInheritanceOwner")
	static class Owner {
		@Id
		private Integer id;

		@Embedded
		private Container container;

		Owner() {
		}

		Owner(Integer id, Container container) {
			this.id = id;
			this.container = container;
		}
	}

	@Embeddable
	static class Container {
		@Embedded
		private NestedValue nested;

		Container() {
		}

		Container(NestedValue nested) {
			this.nested = nested;
		}
	}

	@Embeddable
	@DiscriminatorColumn(name = "nested_value_type")
	@DiscriminatorValue("base")
	static class NestedValue {
		private String baseValue;

		NestedValue() {
		}

		NestedValue(String baseValue) {
			this.baseValue = baseValue;
		}
	}

	@Embeddable
	@DiscriminatorValue("subtype")
	static class NestedSubtype extends NestedValue {
		private String subtypeValue;

		NestedSubtype() {
		}

		NestedSubtype(String baseValue, String subtypeValue) {
			super( baseValue );
			this.subtypeValue = subtypeValue;
		}
	}
}
