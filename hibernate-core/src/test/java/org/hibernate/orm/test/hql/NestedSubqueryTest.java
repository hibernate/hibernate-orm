/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

@DomainModel(
		annotatedClasses = {
				NestedSubqueryTest.Post.class,
				NestedSubqueryTest.PostComment.class
		}
)
@SkipForDialect(dialectClass = H2Dialect.class, majorVersion = 1, reason = "It seems that selecting two columns in a subquery with the same name doesn't work for H2 1.x even with aliasing at table alias level")
@SessionFactory
@JiraKey(value = "HHH-15731")
public class NestedSubqueryTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					short[] latestSQLStandards = {
							2016,
							2011,
							2008
					};

					String[] comments = {
							"great",
							"excellent",
							"awesome"
					};

					int post_id = 1;
					int comment_id = 1;

					for ( int i = 0; i < latestSQLStandards.length; i++ ) {
						short sqlStandard = latestSQLStandards[i];
						Post post = new Post()
								.setId( post_id++ )
								.setTitle( String.format( "SQL:%d", sqlStandard ) );

						session.persist( post );

						for ( int j = 0; j < comments.length - i; j++ ) {
							session.persist(
									new PostComment()
											.setId( comment_id++ )
											.setReview( String.format( "SQL:%d is %s!", sqlStandard, comments[j] ) )
											.setPost( post )
							);
						}
					}

					session.persist(
							new Post()
									.setId( post_id++ )
									.setTitle( "JPA 3.0" )
					);
					session.persist(
							new Post()
									.setId( post_id++ )
									.setTitle( "JPA 2.2" )
					);
					session.persist(
							new Post()
									.setId( post_id++ )
									.setTitle( "JPA 2.1" )
					);
					session.persist(
							new Post()
									.setId( post_id++ )
									.setTitle( "JPA 2.0" )
					);
					session.persist(
							new Post()
									.setId( post_id )
									.setTitle( "JPA 1.0" )
					);
				}
		);
	}


	@Test
	public void testNestedHqlSubQueries(SessionFactoryScope scope) {
		String queryString =
				" SELECT post_id AS post_id, post_title AS post_title, comment_id AS comment_id, comment_review AS comment_review "
						+ " FROM ( "
						+ " 	SELECT p.id AS post_id, p.title AS post_title, pc.id AS comment_id, pc.review AS comment_review "
						+ " 		FROM PostComment pc JOIN pc.post p "
						+ " 		WHERE p.title LIKE :title"
						+ " 	) p_pc"
						+ "	ORDER BY post_id, comment_id";

		scope.inTransaction(
				session -> {
					List<Tuple> results = session.createQuery(
									queryString,
									Tuple.class
							)
							.setParameter( "title", "SQL%" )
							.getResultList();
				}
		);
	}

	@Test
	public void testNestedHqlSubQueries2(SessionFactoryScope scope) {
		String queryString =
				" SELECT post_id AS pid, post_title AS pt, comment_id AS cid, comment_review AS cr "
						+ " FROM ( "
						+ " 	SELECT post_id AS post_id, post_title AS post_title, comment_id AS comment_id,comment_review AS comment_review "
						+ " 		FROM ( "
						+ "				SELECT p.id AS post_id, p.title AS post_title, pc.id AS comment_id, pc.review AS comment_review "
						+ "					FROM PostComment pc JOIN pc.post p WHERE p.title LIKE :title "
						+ " 			) p_pc"
						+ " ) p_pc_r "
						+ " WHERE comment_review like :review ORDER BY post_id, comment_id ";

		scope.inTransaction(
				session -> {
					List<Tuple> results = session.createQuery(
									queryString,
									Tuple.class
							)
							.setParameter( "title", "SQL%" )
							.setParameter( "review", "/%" )
							.getResultList();
				}
		);
	}


	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Integer id;

		private String title;

		public Integer getId() {
			return id;
		}

		public Post setId(Integer id) {
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
	}

	@Entity(name = "PostComment")
	@Table(name = "post_comment")
	public static class PostComment {

		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Post post;

		private String review;

		public Integer getId() {
			return id;
		}

		public PostComment setId(Integer id) {
			this.id = id;
			return this;
		}

		public Post getPost() {
			return post;
		}

		public PostComment setPost(Post post) {
			this.post = post;
			return this;
		}

		public String getReview() {
			return review;
		}

		public PostComment setReview(String review) {
			this.review = review;
			return this;
		}
	}
}
