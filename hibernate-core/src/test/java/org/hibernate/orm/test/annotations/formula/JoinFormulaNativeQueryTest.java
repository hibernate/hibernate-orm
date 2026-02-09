/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.formula;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Jpa(
		annotatedClasses = {
				JoinFormulaNativeQueryTest.Post.class,
				JoinFormulaNativeQueryTest.Comment.class,
		}
)
@Jira("https://hibernate.atlassian.net/browse/HHH-20133")
public class JoinFormulaNativeQueryTest {

	@BeforeAll
	protected void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Post post = new Post();
			post.setId( 1L );
			entityManager.persist( post );

			Comment comment1 = new Comment();
			comment1.setId( 1L );
			comment1.setPost( post );
			entityManager.persist( comment1 );

			Comment comment2 = new Comment();
			comment2.setId( 2L );
			comment2.setPost( post );
			entityManager.persist( comment2 );
		} );
	}

	@Test
	public void testHqlQueryFormula(EntityManagerFactoryScope scope) {
		List<Comment> resultList = scope.fromTransaction( entityManager ->
				entityManager.createQuery( "SELECT c FROM Comment c", Comment.class ).getResultList()
		);
		assertThat( resultList ).hasSize( 2 );
		assertThat( resultList.get( 0 ).getAce() ).isEqualTo( 1L );
		assertThat( resultList.get( 1 ).getAce() ).isEqualTo( 1L );
	}

	@Test
	public void testHqlQueryJoinFormula(EntityManagerFactoryScope scope) {
		List<Post> resultList = scope.fromTransaction( entityManager ->
				entityManager.createQuery( "SELECT p FROM Post p", Post.class ).getResultList()
		);
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).getLatestComment() ).isNotNull();
		assertThat( resultList.get( 0 ).getLatestComment().getId() ).isEqualTo( 2L );
		assertThat( resultList.get( 0 ).getOldestComment() ).isNotNull();
		assertThat( resultList.get( 0 ).getOldestComment().getId() ).isEqualTo( 1L );
	}

	@Test
	public void testNativeQueryFormula(EntityManagerFactoryScope scope) {
		//noinspection unchecked
		List<Comment> resultList = scope.fromTransaction( entityManager ->
				entityManager.createNativeQuery( "SELECT c.*, (SELECT 1 FROM DUAL) as ace FROM comment c",
						Comment.class ).getResultList()
		);
		assertThat( resultList ).hasSize( 2 );
		assertThat( resultList.get( 0 ).getAce() ).isEqualTo( 1L );
		assertThat( resultList.get( 1 ).getAce() ).isEqualTo( 1L );
	}

	@Test
	@FailureExpected
	public void testNativeQueryJoinFormula(EntityManagerFactoryScope scope) {
		//noinspection unchecked
		List<Post> resultList = scope.fromTransaction( entityManager ->
				entityManager.createNativeQuery(
						"""
								SELECT p.*,
								(SELECT c.id FROM comment c WHERE c.post_id = p.id ORDER BY c.id ASC LIMIT 1) as oldestComment,
								(SELECT c.id FROM comment c WHERE c.post_id = p.id ORDER BY c.id DESC LIMIT 1) as latestComment
								FROM post p
								""",
						Post.class ).getResultList()
		);
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).getLatestComment() ).isNotNull();
		assertThat( resultList.get( 0 ).getLatestComment().getId() ).isEqualTo( 2L );
		assertThat( resultList.get( 0 ).getOldestComment() ).isNotNull();
		assertThat( resultList.get( 0 ).getOldestComment().getId() ).isEqualTo( 1L );
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula("(SELECT c.id FROM comment c WHERE c.post_id = id ORDER BY c.id DESC LIMIT 1)")
		private Comment latestComment;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinFormula("(SELECT c.id FROM comment c WHERE c.post_id = id ORDER BY c.id ASC LIMIT 1)")
		private Comment oldestComment;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Comment getLatestComment() {
			return latestComment;
		}

		public Comment getOldestComment() {
			return oldestComment;
		}
	}

	@Entity(name = "Comment")
	@Table(name = "comment")
	public static class Comment {

		@Id
		private Long id;

		@Formula("(SELECT 1 FROM DUAL)")
		private Long ace;

		@ManyToOne(fetch = FetchType.LAZY)
		private Post post;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getAce() {
			return ace;
		}

		public Post getPost() {
			return post;
		}

		public void setPost(Post post) {
			this.post = post;
		}

	}

}
