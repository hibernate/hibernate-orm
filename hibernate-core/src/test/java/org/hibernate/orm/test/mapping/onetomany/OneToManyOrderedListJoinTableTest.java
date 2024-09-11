/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		OneToManyOrderedListJoinTableTest.Post.class,
		OneToManyOrderedListJoinTableTest.Comment.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18598" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-15591" )
public class OneToManyOrderedListJoinTableTest {
	@Test
	public void testNormalAddAndRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			final Comment comment = new Comment( 3L, "comment_3" );
			session.persist( comment );
			comments.add( comment );
		} );
		scope.inTransaction( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			assertThat( comments ).extracting( Comment::getId ).containsExactly( 1L, 2L, 3L );
			comments.remove( 2 );
			comments.remove( 1 );
		} );
		scope.inSession( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			assertThat( comments ).extracting( Comment::getId ).containsExactly( 1L );
		} );
	}

	@Test
	public void testSwapElementsAtZeroAndOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			final Comment comment1 = comments.get( 0 );
			final Comment comment2 = comments.get( 1 );
			comments.set( 0, comment2 );
			comments.set( 1, comment1 );
		} );
		scope.inSession( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			assertThat( comments ).extracting( Comment::getId ).containsExactly( 2L, 1L );
		} );
	}

	@Test
	public void testAddAtZeroDeleteAtTwo(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			final Comment comment = new Comment( 3L, "comment_3" );
			session.persist( comment );
			comments.add( 0, comment );
			comments.remove( 2 );
		} );
		scope.inSession( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			assertThat( comments ).extracting( Comment::getId ).containsExactly( 3L, 1L );
		} );
	}

	@Test
	public void testAddSameElementTwice(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			comments.add( comments.size(), comments.get( 0 ) );
			assertThat( comments ).hasSize( 3 );
		} );
		scope.inSession( session -> {
			final List<Comment> comments = session.find( Post.class, 1L ).getComments();
			assertThat( comments ).extracting( Comment::getId ).containsExactly( 1L, 2L, 1L );
			assertThat( comments.get( 0 ) ).isSameAs( comments.get( 2 ) );
		} );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Post post = new Post();
			post.setId( 1L );
			session.persist( post );

			final String[] strings = new String[] { "comment_1", "comment_2" };
			for ( int i = 0; i < strings.length; i++ ) {
				final Comment comment = new Comment( (long) i + 1, strings[i] );
				post.getComments().add( comment );
				session.persist( comment );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "Post" )
	static class Post {
		@Id
		private Long id;

		@OneToMany
		@JoinTable( name = "post_comments" )
		@OrderColumn( name = "order_col" )
		private List<Comment> comments = new ArrayList<>();

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Comment> getComments() {
			return comments;
		}
	}

	@Entity( name = "Comment" )
	@Table( name = "comment_table" )
	static class Comment {
		@Id
		private Long id;

		@Column( name = "text_col" )
		private String text;

		public Comment() {
		}

		public Comment(Long id, String text) {
			this.id = id;
			this.text = text;
		}

		public Long getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}
}
