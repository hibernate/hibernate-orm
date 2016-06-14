/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class TupleQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class};
	}

	@Test
	public void testGetAliasReturnNullIfNoAliasExist() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			User u = new User( "Fab" );
			em.persist( u );
			em.getTransaction().commit();

			TypedQuery<Tuple> query = em.createQuery( "SELECT u.firstName from User u", Tuple.class );

			List<Tuple> result = query.getResultList();
			List<TupleElement<?>> elements = result.get( 0 ).getElements();

			assertThat( elements.size(), is( 1 ) );
			final String alias = elements.get( 0 ).getAlias();
			assertThat( alias, is( nullValue() ) );
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testGetAlias() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			User u = new User( "Fab" );
			em.persist( u );
			em.getTransaction().commit();

			TypedQuery<Tuple> query = em.createQuery( "SELECT u.firstName as fn from User u", Tuple.class );

			List<Tuple> result = query.getResultList();
			List<TupleElement<?>> elements = result.get( 0 ).getElements();

			assertThat( elements.size(), is( 1 ) );
			final String alias = elements.get( 0 ).getAlias();
			assertThat( alias, is( "fn" ) );
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Entity(name = "User")
	@Table(name = "USERS")
	public static class User {
		@Id
		@GeneratedValue
		long id;

		String firstName;

		public User() {
		}

		public User(String firstName) {
			this.firstName = firstName;
		}
	}
}
