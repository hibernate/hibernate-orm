/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceSameAttributeNameTest.BaseObj.class,
		JoinedInheritanceSameAttributeNameTest.Comment.class,
		JoinedInheritanceSameAttributeNameTest.Author.class,
		JoinedInheritanceSameAttributeNameTest.Post.class,
		JoinedInheritanceSameAttributeNameTest.AuthorEmbedded.class,
} )
@SessionFactory
@JiraKey( "HHH-16166" )
public class JoinedInheritanceSameAttributeNameTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author = new Author( "comments" );
			author.setName( "Marco" );
			final Post post = new Post();
			final Comment comment = new Comment( post, author );
			post.getComments().add( comment );
			session.persist( post );
			session.persist( comment );
			session.persist( author );
			final AuthorEmbedded authorEmbedded = new AuthorEmbedded( "Andrea", "comments" );
			session.persist( authorEmbedded );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Comment" ).executeUpdate();
			session.createMutationQuery( "delete from Post" ).executeUpdate();
			session.createMutationQuery( "delete from Author" ).executeUpdate();
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Post post = session.createQuery( "from Post", Post.class ).getSingleResult();
			assertThat( post ).isNotNull();
			assertThat( post.getComments() ).hasSize( 1 );
			final Comment comment = post.getComments().iterator().next();
			assertThat( comment.getPost() ).isEqualTo( post );
			assertThat( comment.getAuthor().getName() ).isEqualTo( "Marco" );
			assertThat( comment.getAuthor().getComments() ).isEqualTo( "comments" );
		} );
	}

	@Entity( name = "BaseObj" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class BaseObj {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Comment" )
	@Table( name = "Comments" )
	public static class Comment extends BaseObj {
		@ManyToOne
		private Post post;

		@ManyToOne
		private Author author;

		public Comment() {
		}

		public Comment(Post post, Author author) {
			this.post = post;
			this.author = author;
		}

		public Post getPost() {
			return post;
		}

		public Author getAuthor() {
			return author;
		}
	}

	/**
	 * This sub-entity has the same attribute name
	 * as {@link Author} but with a different (Collection) type
	 */
	@Entity( name = "Post" )
	public static class Post extends BaseObj {
		@OneToMany( mappedBy = "post" )
		private Set<Comment> comments;

		public Post() {
			this.comments = new HashSet<>();
		}

		public Set<Comment> getComments() {
			return comments;
		}
	}

	/**
	 * This sub-entity has the same attribute name
	 * as {@link Post} but with a different (SimpleValue) type
	 */
	@Entity( name = "Author" )
	public static class Author extends BaseObj {
		private String comments;

		public Author() {
		}

		public Author(String comments) {
			this.comments = comments;
		}

		public String getComments() {
			return comments;
		}

		public void setComments(String comments) {
			this.comments = comments;
		}
	}

	@Embeddable
	public static class NestedEmbeddable {
		@ElementCollection
		@CollectionTable(
				name = "author_comments",
				joinColumns = @JoinColumn( name = "name", referencedColumnName = "name" )
		)
		private Set<String> comments;

		private Integer testProperty;

		public NestedEmbeddable() {
			comments = new HashSet<>();
		}

		public Set<String> getComments() {
			return comments;
		}

		public void setComments(Set<String> comments) {
			this.comments = comments;
		}

		public Integer getTestProperty() {
			return testProperty;
		}

		public void setTestProperty(Integer testProperty) {
			this.testProperty = testProperty;
		}
	}

	@Embeddable
	public static class AuthorEmbeddable {
		private NestedEmbeddable nestedEmbeddable;

		public AuthorEmbeddable() {
			this.nestedEmbeddable = new NestedEmbeddable();
		}

		public NestedEmbeddable getNestedEmbeddable() {
			return nestedEmbeddable;
		}

		public void setNestedEmbeddable(NestedEmbeddable nestedEmbeddable) {
			this.nestedEmbeddable = nestedEmbeddable;
		}
	}

	@Entity( name = "AuthorEmbedded" )
	public static class AuthorEmbedded extends BaseObj {
		private AuthorEmbeddable authorEmbeddable;

		public AuthorEmbedded() {
		}

		public AuthorEmbedded(String name, String comment) {
			setName( name );
			this.authorEmbeddable = new AuthorEmbeddable();
			this.authorEmbeddable.getNestedEmbeddable().getComments().add( comment );
		}

		public AuthorEmbeddable getAuthorEmbeddable() {
			return authorEmbeddable;
		}

		public void setAuthorEmbeddable(AuthorEmbeddable authorEmbeddable) {
			this.authorEmbeddable = authorEmbeddable;
		}
	}
}
