/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.jpa.test.criteria.mapsid;

import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Cody Lerum
 */
public class MapsIdOnetoOneSelectTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Post.class, PostDetails.class
		};
	}

	@Before
	public void prepareTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Post post = new Post();
		post.setId( 1 );
		post.setName( "Name" );
		em.persist( post );

		PostDetails details = new PostDetails();
		details.setName( "Details Name" );
		details.setPost( post );
		em.persist( details );

		em.getTransaction().commit();
		em.close();
	}

	@TestForIssue(jiraKey = "HHH-9296")
	@Test
	public void selectByParent() {
		EntityManager em = createEntityManager();
		try {
			Post post = em.find( Post.class, 1 );

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<PostDetails> query = cb.createQuery( PostDetails.class );
			Root<PostDetails> root = query.from( PostDetails.class );
			query.where( cb.equal( root.get( "post" ), post ) );
			final PostDetails result = em.createQuery( query ).getSingleResult();
			assertNotNull( result );
		}
		finally {
			em.close();
		}
	}

	@TestForIssue(jiraKey = "HHH-9296")
	@Test
	public void findByParentId() {
		EntityManager em = createEntityManager();
		try {
			Post post = em.find( Post.class, 1 );
			PostDetails result = em.find( PostDetails.class, post.getId() );
			assertNotNull( result );
		}
		finally {
			em.close();
		}
	}
}
