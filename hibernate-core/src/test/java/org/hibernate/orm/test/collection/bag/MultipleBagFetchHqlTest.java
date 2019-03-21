/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.loader.MultipleBagFetchException;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FailureExpected("Multiple Bag Fetch detection not yet implemented")
public class MultipleBagFetchHqlTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Post.class,
				PostComment.class,
				Tag.class
		};
	}

	@Test
	public void testMultipleBagFetchHql() {

		final Post post = new Post();
		post.setId( 1L );
		post.setTitle( String.format( "Post nr. %d", 1 ) );
		PostComment comment = new PostComment();
		comment.setId( 1L );
		comment.setReview( "Excellent!" );

		inTransaction(
				session -> {
					session.persist( post );
					session.persist( comment );
					post.comments.add( comment );
				}
		);

		try {
			inTransaction( session -> {
				session.createQuery(
						"select p " +
								"from Post p " +
								"join fetch p.tags " +
								"join fetch p.comments " +
								"where p.id = :id"
				)
						.setParameter( "id", 1L )
						.uniqueResult();
				fail( "Should throw org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags" );
			} );

		}
		catch (IllegalArgumentException expected) {
			// MultipleBagFetchException was converted to IllegalArgumentException
			assertTrue( MultipleBagFetchException.class.isInstance( expected.getCause() ) );
		}
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		private String title;

		@OneToMany(fetch = FetchType.LAZY)
		private List<PostComment> comments = new ArrayList<PostComment>();

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "post_tag",
				joinColumns = @JoinColumn(name = "post_id"),
				inverseJoinColumns = @JoinColumn(name = "tag_id")
		)
		private List<Tag> tags = new ArrayList<Tag>();

		public Post() {
		}

		public Post(Long id) {
			this.id = id;
		}

		public Post(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Tag> getTags() {
			return tags;
		}
	}

	@Entity(name = "PostComment")
	@Table(name = "post_comment")
	public static class PostComment {

		@Id
		private Long id;

		private String review;

		public PostComment() {
		}

		public PostComment(String review) {
			this.review = review;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getReview() {
			return review;
		}

		public void setReview(String review) {
			this.review = review;
		}
	}

	@Entity(name = "Tag")
	@Table(name = "tag")
	public static class Tag {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
