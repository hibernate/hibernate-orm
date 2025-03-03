/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		MappedSuperclassGenericEmbeddableQueryParamTest.FirstEntity.class,
		MappedSuperclassGenericEmbeddableQueryParamTest.SecondEntity.class
})
@SessionFactory
@JiraKey("HHH-16195")
public class MappedSuperclassGenericEmbeddableQueryParamTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final FirstEntity firstEntity = new FirstEntity();
			firstEntity.setSomeAttribute( new FirstEntityAttribute( "first" ) );
			final SecondEntity secondEntity = new SecondEntity();
			secondEntity.setSomeAttribute( new SecondEntityAttribute( "second" ) );
			session.persist( firstEntity );
			session.persist( secondEntity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from FirstEntity" ).executeUpdate();
			session.createMutationQuery( "delete from SecondEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testFirstEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final FirstEntity firstEntity = session.createQuery(
							"from FirstEntity where someAttribute = :param",
							FirstEntity.class
					)
					.setParameter( "param", new FirstEntityAttribute( "first" ) )
					.getSingleResult();
			assertThat( firstEntity.getSomeAttribute().getAttributeValue() ).isEqualTo( "first" );
		} );
	}

	@Test
	public void testSecondEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SecondEntity secondEntity = session.createQuery(
							"from SecondEntity where someAttribute = :param",
							SecondEntity.class
					)
					.setParameter( "param", new SecondEntityAttribute( "second" ) )
					.getSingleResult();
			assertThat( secondEntity.getSomeAttribute().getAttributeValue() ).isEqualTo( "second" );
		} );
	}

	@Embeddable
	public interface GenericValue {
		String getAttributeValue();

		void setAttributeValue(String value);
	}

	@MappedSuperclass
	public static class BaseEntity<T extends GenericValue> {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bigserial")
		private Long id;

		@Embedded
		private T someAttribute;

		public T getSomeAttribute() {
			return someAttribute;
		}

		public void setSomeAttribute(T someAttribute) {
			this.someAttribute = someAttribute;
		}
	}

	@Embeddable
	public static class FirstEntityAttribute implements GenericValue {
		private String attributeValue;

		public FirstEntityAttribute() {
		}

		public FirstEntityAttribute(String attributeValue) {
			this.attributeValue = attributeValue;
		}

		@Override
		public String getAttributeValue() {
			return attributeValue;
		}

		@Override
		public void setAttributeValue(String attributeValue) {
			this.attributeValue = attributeValue;
		}
	}

	@Entity(name = "FirstEntity")
	public static class FirstEntity extends BaseEntity<FirstEntityAttribute> {
	}

	@Embeddable
	public static class SecondEntityAttribute implements GenericValue {
		private String attributeValue;

		public SecondEntityAttribute() {
		}

		public SecondEntityAttribute(String attributeValue) {
			this.attributeValue = attributeValue;
		}

		@Override
		public String getAttributeValue() {
			return attributeValue;
		}

		@Override
		public void setAttributeValue(String attributeValue) {
			this.attributeValue = attributeValue;
		}
	}

	@Entity(name = "SecondEntity")
	public static class SecondEntity extends BaseEntity<SecondEntityAttribute> {
	}
}
