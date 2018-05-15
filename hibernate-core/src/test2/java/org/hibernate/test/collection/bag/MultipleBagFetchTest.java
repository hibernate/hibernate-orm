/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.bag;

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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.loader.MultipleBagFetchException;

import org.junit.Test;

import static org.junit.Assert.fail;

public class MultipleBagFetchTest {

	@Test
	public void testEntityWithMultipleJoinFetchedBags() {
		StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder().build();

		Metadata metadata = new MetadataSources( standardRegistry )
				.addAnnotatedClass( Post.class )
				.addAnnotatedClass( PostComment.class )
				.addAnnotatedClass( Tag.class )
				.getMetadataBuilder()
				.build();
		try {
			metadata.buildSessionFactory();
			fail( "MultipleBagFetchException should have been thrown." );
		}
		catch (MultipleBagFetchException expected) {
		}
	}

	@Entity(name = "Post")
	@Table(name = "post")
	public static class Post {

		@Id
		private Long id;

		private String title;

		@OneToMany(fetch = FetchType.EAGER)
		private List<PostComment> comments = new ArrayList<PostComment>();

		@ManyToMany(fetch = FetchType.EAGER)
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
