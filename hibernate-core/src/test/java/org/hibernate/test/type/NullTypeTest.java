/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Jordan Gigov
 */
@TestForIssue( jiraKey = "HHH-10997" )
public class NullTypeTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ Persons.class };
	}

	@Test
	public void testPositionalInsert() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			Query q;

			// unnamed queries
			q = em.createNativeQuery( "INSERT INTO persons_for_insert(id, the_name) VALUES ( ?1 , ?2 )" );
			// Didn't the parameters used to start at 1 ?
			q.setParameter( 1, 1L );
			q.setParameter( 2, null );
			q.executeUpdate();

			et.commit();
		}
		catch ( Exception e ) {
			if ( et.isActive() ) {
				et.rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testNamedInsert() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			Query q;

			q = em.createNativeQuery( "INSERT INTO persons_for_insert(id, the_name) VALUES ( :id , :data )" );
			q.setParameter( "id", 2L );
			q.setParameter( "data", null );
			q.executeUpdate();

			et.commit();
		}
		catch ( Exception e ) {
			if ( et.isActive() ) {
				et.rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Entity( name = "Persons" )
	@Table( name = "persons_for_insert" )
	public static class Persons {

		@Id
		private Long id;

		@Column( name = "the_name" )
		private String name;

		public Persons() {
		}

		public Persons(Long id, String name) {
			this.id = id;
			this.name = name;
		}

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
