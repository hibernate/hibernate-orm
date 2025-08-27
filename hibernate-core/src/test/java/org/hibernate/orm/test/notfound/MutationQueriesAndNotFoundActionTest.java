/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				MutationQueriesAndNotFoundActionTest.User.class,
				MutationQueriesAndNotFoundActionTest.Comment.class
		}
)
@SessionFactory( useCollectingStatementInspector = true )
@JiraKey("HHH-16878")
public class MutationQueriesAndNotFoundActionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final User user1 = new User( 1L, "test 1" );
					final User user2 = new User( 2L, "test 2" );
					session.persist( user1 );
					session.persist( user2 );
					session.persist( new Comment( 1L, "example 1", user1 ) );
					session.persist( new Comment( 2L, "example 2", user2 ) );
					session.persist( new Comment( 3L, "example 3", user1 ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				session -> {
					statementInspector.clear();

					int affectedComments = session.createMutationQuery(
									"update Comment c set c.text = :text where c.user = :user" )
							.setParameter( "text", "updated" )
							.setParameter( "user", session.getReference( User.class, 1L ) )
							.executeUpdate();

					assertThat( affectedComments ).isEqualTo( 2 );
					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					assertThat( statementInspector.getSqlQueries().get( 0 ) ).matches( (sql) -> {
						return sql.contains( " join " ) || sql.contains( "exists" );
					} );
				}
		);
	}

	@Test
	public void testUpdateWithImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int affectedComments = session.createMutationQuery( "update Comment c set c.text = :text where c.user.name = :userName" )
							.setParameter( "text", "updated" )
							.setParameter( "userName", "test 1" )
							.executeUpdate();

					assertThat( affectedComments ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testUpdateSet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int affectedComments = session.createMutationQuery(
									"update Comment c set c.user = :user" )
							.setParameter( "user", session.getReference( User.class, 2L ) )
							.executeUpdate();

					assertThat( affectedComments ).isEqualTo( 3 );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int affectedComments = session.createMutationQuery( "delete from Comment c where c.user = :user" )
							.setParameter( "user", session.getReference( User.class, 1L ) )
							.executeUpdate();

					assertThat( affectedComments ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testDeleteWithImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int affectedComments = session.createMutationQuery( "delete from Comment c where c.user.name = :userName" )
							.setParameter( "userName", "test 1" )
							.executeUpdate();

					assertThat( affectedComments ).isEqualTo( 2 );
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		@Id
		public Long id;

		public String name;

		public User() {
		}

		public User(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Comment")
	@Table(name = "comments")
	public static class Comment {

		@Id
		public Long id;

		public String text;

		@ManyToOne
		@JoinColumn(name = "user_id")
		@NotFound(action = NotFoundAction.IGNORE)
		public User user;

		public Comment() {
		}

		public Comment(Long id, String text, User user) {
			this.id = id;
			this.text = text;
			this.user = user;
		}
	}
}
