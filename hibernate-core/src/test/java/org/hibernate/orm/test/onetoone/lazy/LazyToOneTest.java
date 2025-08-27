/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.lazy;

import java.util.Date;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey(value = "HHH-12842")
@DomainModel(
		annotatedClasses = {
				LazyToOneTest.Post.class,
				LazyToOneTest.PostDetails.class
		}
)
@SessionFactory
public class LazyToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Post post = new Post();
			post.setDetails( new PostDetails() );
			post.getDetails().setCreatedBy( "ME" );
			post.getDetails().setCreatedOn( new Date() );
			post.setTitle( "title" );
			s.persist( post );
		} );
	}

	@Test
	public void testOneToOneLazyLoading(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			PostDetails post = (PostDetails) s.createQuery( "select a from PostDetails a" ).getResultList().get( 0 );
			assertFalse( isInitialized( post.post ) );
		} );
	}

	@Entity(name = "PostDetails")
	@Table(name = "post_details")
	public static class PostDetails {

		@Id
		private Long id;

		@Column(name = "created_on")
		private Date createdOn;

		@Column(name = "created_by")
		private String createdBy;

		@MapsId
		@OneToOne(fetch = FetchType.LAZY, mappedBy = "details", optional = false)
		private Post post;

		public PostDetails() {
		}

		public PostDetails(String createdBy) {
			createdOn = new Date();
			this.createdBy = createdBy;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(String createdBy) {
			this.createdBy = createdBy;
		}

		public Post getPost() {
			return post;
		}

		public void setPost(Post post) {
			this.post = post;
		}

	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@PrimaryKeyJoinColumn
		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private PostDetails details;


		public void setDetails(PostDetails details) {
			if ( details == null ) {
				if ( this.details != null ) {
					this.details.setPost( null );
				}
			}
			else {
				details.setPost( this );
			}
			this.details = details;
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

		public PostDetails getDetails() {
			return details;
		}
	}

}
