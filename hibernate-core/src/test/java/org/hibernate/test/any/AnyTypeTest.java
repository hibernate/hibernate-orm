/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.any;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-1663")
public class AnyTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "any/Person.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		// having second level cache causes a condition whereby the original test case would not fail...
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Test
	public void testFlushProcessing() {
		Person person = new Person();
		Address address = new Address();
		person.setData( address );
		inTransaction(
				session -> {
					session.saveOrUpdate( person );
					session.saveOrUpdate( address );
				}
		);

		inTransaction(
				session -> {
					Person p = session.load( Person.class, person.getId() );
					p.setName( "makingpersondirty" );
				}
		);

		inTransaction(
				session -> {
					session.delete( person );
				}
		);
	}

	@Test
	public void testJoinFetchOfAnAnyTypeAttribute() {
		// Query translator should dis-allow join fetching of an <any/> mapping.  Let's make sure it does...
		Session session = openSession();
		try {
			session.beginTransaction();
			session.createQuery( "select p from Person p join fetch p.data" ).list();
			session.getTransaction().commit();
		}
		catch (IllegalArgumentException e) {
			//expected
			assertTyping( SemanticException.class, e.getCause() );
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
		}
		catch (SemanticException qe) {
			//expected
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
		}
		finally {
			session.close();
		}
	}
}
