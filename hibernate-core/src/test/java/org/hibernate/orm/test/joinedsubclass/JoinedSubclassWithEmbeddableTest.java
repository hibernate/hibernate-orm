/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.joinedsubclass;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				JoinedSubclassWithEmbeddableTest.BaseEntity.class,
				JoinedSubclassWithEmbeddableTest.ConcreteEntity.class
		}
)
@SessionFactory
public class JoinedSubclassWithEmbeddableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ConcreteEntity entity = new ConcreteEntity();
			entity.setId( 1L );
			entity.setField( "field_base" );
			EmbeddedValue embeddedValue = new EmbeddedValue();
			embeddedValue.setField( "field_embedded" );
			entity.setEmbedded( embeddedValue );

			session.persist( entity );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10920")
	public void testEmbeddedFieldIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ConcreteEntity entity = session.get( ConcreteEntity.class, 1L );
			assertThat( entity.getEmbedded().getField(), is( "field_embedded" ) );
			assertThat( entity.getField(), is( "field_base" ) );
			entity.getEmbedded().setField( "field_subclass" );
		} );

		scope.inTransaction( session -> {
			final ConcreteEntity entity = session.get( ConcreteEntity.class, 1L );
			assertThat( entity.getEmbedded().getField(), is( "field_subclass" ) );
			assertThat( entity.getField(), is( "field_base" ) );
		} );
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class BaseEntity {
		@Id
		private Long id;

		private String field;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	@Entity(name = "ConcreteEntity")
	public static class ConcreteEntity extends BaseEntity {
		private EmbeddedValue embeddedValue;

		public EmbeddedValue getEmbedded() {
			return embeddedValue;
		}

		public void setEmbedded(EmbeddedValue embeddedValue) {
			this.embeddedValue = embeddedValue;
		}
	}

	@Embeddable
	public static class EmbeddedValue {
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
