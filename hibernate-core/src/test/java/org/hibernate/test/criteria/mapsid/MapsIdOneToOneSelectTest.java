/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.criteria.mapsid;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Cody Lerum
 */
public class MapsIdOneToOneSelectTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Post.class, PostDetails.class
		};
	}

	@Before
	public void prepareTestData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Post post = new Post();
			post.setId( 1 );
			post.setName( "Name" );
			entityManager.persist( post );

			PostDetails details = new PostDetails();
			details.setName( "Details Name" );
			details.setPost( post );
			entityManager.persist( details );
		});
	}

	@TestForIssue(jiraKey = "HHH-9296")
	@Test
	public void selectByParent() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Post post = entityManager.find( Post.class, 1 );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<PostDetails> query = cb.createQuery( PostDetails.class );
			Root<PostDetails> root = query.from( PostDetails.class );
			query.where( cb.equal( root.get( "post" ), post ) );
			final PostDetails result = entityManager.createQuery( query ).getSingleResult();
			assertNotNull( result );
		});
	}

	@TestForIssue(jiraKey = "HHH-9296")
	@Test
	public void findByParentId() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Post post = entityManager.find( Post.class, 1 );
			PostDetails result = entityManager.find( PostDetails.class, post.getId() );
			assertNotNull( result );
		});
	}

	@Entity(name = "Post")
	public static class Post implements Serializable {

		private static final long serialVersionUID = 1L;

		private Integer id;

		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@NotNull
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Entity(name = "PostDetails")
	public static class PostDetails implements Serializable {

		private static final long serialVersionUID = 1L;

		private Integer id;

		private String name;

		private Post post;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@NotNull
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne
		@JoinColumn(name = "id")
		@MapsId
		public Post getPost() {
			return post;
		}

		public void setPost(Post post) {
			this.post = post;
		}
	}
}
