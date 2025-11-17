/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lisandro Fernandez (kelechul at gmail dot com)
 */
@JiraKey("HHH-13815")
@Jpa(
		annotatedClasses = {
				BidirectionalOneToManyMergeTest.Post.class,
				BidirectionalOneToManyMergeTest.PostComment.class,
		}
)
public class BidirectionalOneToManyMergeTest {


	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist(
					new Post( "High-Performance Java Persistence" ).setId( 1L )
			);
		} );
	}

	@Test
	public void testMerge(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Post post = entityManager.find( Post.class, 1L );
			post.addComment( new PostComment( "This post rocks!", post ) );
			post.getComments().isEmpty();
			entityManager.merge( post );
		} );
	}

	@Entity
	public static class Post {

		@Id
		private Long id;

		private String title;

		@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<PostComment> comments = new ArrayList<>();

		public Post() {
		}

		public Post(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public Post setId(Long id) {
			this.id = id;
			return this;
		}

		public String getTitle() {
			return title;
		}

		public Post setTitle(String title) {
			this.title = title;
			return this;
		}

		public List<PostComment> getComments() {
			return comments;
		}

		private Post setComments(List<PostComment> comments) {
			this.comments = comments;
			return this;
		}

		public Post addComment(PostComment comment) {
			comments.add( comment );
			comment.setPost( this );

			return this;
		}

		public Post removeComment(PostComment comment) {
			comments.remove( comment );
			comment.setPost( null );

			return this;
		}
	}

	@Entity
	public static class PostComment {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String review;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "post_id")
		private Post post;

		public PostComment() {
		}

		public PostComment(String review, Post post) {
			this.review = review;
			this.post = post;
		}

		public Long getId() {
			return id;
		}

		public PostComment setId(Long id) {
			this.id = id;
			return this;
		}

		public String getReview() {
			return review;
		}

		public PostComment setReview(String review) {
			this.review = review;
			return this;
		}

		public Post getPost() {
			return post;
		}

		public PostComment setPost(Post post) {
			this.post = post;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !(o instanceof PostComment) ) {
				return false;
			}
			return id != null && id.equals( ((PostComment) o).getId() );
		}

		@Override
		public int hashCode() {
			return 31;
		}
	}
}
