/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import from.In;
import in.from.Any;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10953")
public class UpdateEntitiesWithPackageNamesStartingWithKeywordsTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Any.class, In.class};
	}

	@Test
	public void testUpdateEntityWithPackageNameStartingWithIn() {
		Any entity = new Any();
		entity.setProp( "1" );
		try (Session session = openSession()) {

			session.getTransaction().begin();
			try {
				session.save( entity );
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				final Query query = session.createQuery( "UPDATE Any set prop = :prop WHERE id = :id " );
				query.setParameter( "prop", "1" );
				query.setParameter( "id", entity.getId() );
				query.executeUpdate();
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				session.createQuery( "DELETE FROM Any" ).executeUpdate();
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testUpdateEntityWithPackageNameStartingWithFrom() {
		In entity = new In();
		entity.setProp( "1" );
		try (Session session = openSession()) {

			session.getTransaction().begin();
			try {
				session.save( entity );
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				final Query query = session.createQuery( "UPDATE In set prop = :prop WHERE id = :id " );
				query.setParameter( "prop", "1" );
				query.setParameter( "id", entity.getId() );
				query.executeUpdate();
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
		try (Session session = openSession()) {
			session.getTransaction().begin();
			try {
				session.createQuery( "DELETE FROM In" ).executeUpdate();
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}
}
