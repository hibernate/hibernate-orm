/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToOneBatchLoadErrorTest.Parent.class,
		ManyToOneBatchLoadErrorTest.Child.class
} )
@SessionFactory
public class ManyToOneBatchLoadErrorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Child child1 = new Child( "TYPE_ONE" );
			session.persist( child1 );
			session.persist( new Parent( 1L, child1 ) );
			final Child child2 = new Child( "TYPE_TWO" );
			session.persist( child2 );
			session.persist( new Parent( 2L, child2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
			session.createMutationQuery( "delete from Child" ).executeUpdate();
		} );
	}

	@Test
	public void testRuntimeException(SessionFactoryScope scope) {
		scope.inSession( session -> {
			try {
				session.find( Parent.class, 1L );
				fail( "Expected RuntimeException to be thrown in AttributeConverter" );
			}
			catch (Exception e) {
				assertThat( e )
						.isInstanceOf( PersistenceException.class )
						.hasMessageContaining( "Error attempting to apply AttributeConverter" );
			}
		} );
	}

	@Test
	public void testWorking(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Parent parent = session.find( Parent.class, 2L );
			assertThat( parent.getChild() ).isNotNull();
			assertThat( parent.getChild().getType() ).isEqualTo( "TYPE_TWO" );
		} );
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@Id
		private Long id;

		@ManyToOne
		@Fetch( value = FetchMode.SELECT )
		private Child child;

		public Parent() {
		}

		public Parent(Long id, Child child) {
			this.id = id;
			this.child = child;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity( name = "Child" )
	@BatchSize( size = 5 )
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		@Convert( converter = ChildTypeConverter.class )
		private String type;

		public Child() {
		}

		public Child(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}

	public static class ChildTypeConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			if ( dbData.equals( "TYPE_ONE" ) ) {
				throw new RuntimeException();
			}
			return dbData;
		}
	}
}
