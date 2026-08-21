/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A literal created for an enum constant which declares a class body must be typed as the
 * enum itself. The runtime class of such a constant is an anonymous subclass of the enum,
 * for which {@link Class#isEnum()} returns {@code false}.
 */
@JiraKey("HHH-20776")
@DomainModel(annotatedClasses = EnumConstantBodyLiteralTest.Task.class)
@SessionFactory
public class EnumConstantBodyLiteralTest {

	@BeforeAll
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Task( 1L, Status.OPEN ) );
			session.persist( new Task( 2L, Status.CLOSED ) );
			session.persist( new Task( 3L, Status.SUSPENDED ) );
		} );
	}

	@AfterAll
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInListOfEnumLiterals(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var task = query.from( Task.class );
			query.select( task.get( "id" ) )
					.where( task.get( "status" )
							.in( builder.literal( Status.OPEN ), builder.literal( Status.SUSPENDED ) ) )
					.orderBy( builder.asc( task.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.containsExactly( 1L, 3L );
		} );
	}

	@Test
	public void testEqualityWithEnumLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var task = query.from( Task.class );
			query.select( task.get( "id" ) )
					.where( builder.equal( task.get( "status" ), builder.literal( Status.CLOSED ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.containsExactly( 2L );
		} );
	}

	@Test
	public void testEnumLiteralAsLeftOperand(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var task = query.from( Task.class );
			query.select( task.get( "id" ) )
					.where( builder.notEqual( builder.literal( Status.CLOSED ), task.get( "status" ) ) )
					.orderBy( builder.asc( task.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.containsExactly( 1L, 3L );
		} );
	}

	public enum Status {
		OPEN {
			@Override
			public boolean isTerminal() {
				return false;
			}
		},
		SUSPENDED {
			@Override
			public boolean isTerminal() {
				return false;
			}
		},
		CLOSED {
			@Override
			public boolean isTerminal() {
				return true;
			}
		};

		public abstract boolean isTerminal();
	}

	@Entity(name = "Task")
	public static class Task {
		@Id
		private Long id;

		@Enumerated(EnumType.STRING)
		private Status status;

		public Task() {
		}

		public Task(Long id, Status status) {
			this.id = id;
			this.status = status;
		}
	}
}
