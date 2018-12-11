/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.any;

import java.util.HashSet;
import javax.persistence.PersistenceException;

import org.hibernate.JDBCException;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.hql.internal.ast.QuerySyntaxException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-1663" )
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
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person();
		Address address = new Address();
		person.setData( address );
		session.saveOrUpdate(person);
		session.saveOrUpdate(address);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
        person = (Person) session.load( Person.class, person.getId() );
        person.setName("makingpersondirty");
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( person );
		session.getTransaction().commit();
		session.close();
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
			assertTyping( QuerySyntaxException.class, e.getCause() );
			session.getTransaction().rollback();
		}
		catch (QuerySyntaxException qe) {
			//expected
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testFetchEager () {
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person();
		Address address = new Address();
		address.setLines(new HashSet());
		person.setData( address );
		session.saveOrUpdate(person);
		session.saveOrUpdate(address);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		person = (Person) session.load( Person.class, person.getId() );
		address = (Address) person.getData();
		session.getTransaction().commit();
		session.close();

		address.getLines();
	}

	@Test
	public void testFetchLazy () {
		Session session = openSession();
		session.beginTransaction();
		LazyPerson person = new LazyPerson();
		Address address = new Address();
		address.setLines(new HashSet());
		person.setData( address );
		session.saveOrUpdate(person);
		session.saveOrUpdate(address);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		person = (LazyPerson) session.load( LazyPerson.class, person.getId() );
		address = (Address) person.getData();
		session.getTransaction().commit();
		session.close();

		try {
			address.getLines();
			fail( "should not get the property string after session closed." );
		}
		catch (LazyInitializationException e) {
			// expected
		}
		catch (Exception e) {
			fail( "should not throw exception other than LazyInitializationException." );
		}
	}
}
