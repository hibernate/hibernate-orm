package org.hibernate.orm.test.notfound;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				MutationQueriesAndNotFoundActionTest.User.class,
				MutationQueriesAndNotFoundActionTest.Comment.class
		}
)
@SessionFactory
@JiraKey("HHH-16878")
public class MutationQueriesAndNotFoundActionTest {
	private static final User user1 = new User( 1l, "test 1" );

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user2 = new User( 2l, "test 2" );
					session.persist( user1 );
					session.persist( user2 );
					session.persist( new Comment( 1l, "example 1", user1 ) );
					session.persist( new Comment( 2l, "example 2", user2 ) );
					session.persist( new Comment( 3l, "example 3", user1 ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Comment" ).executeUpdate();
					session.createMutationQuery( "delete from User" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int affectedComments = session.createMutationQuery(
									"UPDATE Comment c SET c.text = :text WHERE c.user = :user" )
							.setParameter( "text", "updated" )
							.setParameter( "user", user1 )
							.executeUpdate();

					assertThat( affectedComments ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int affectedComments = session.createMutationQuery( "delete from Comment c where c.user = :user" )
							.setParameter( "user", user1 )
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
