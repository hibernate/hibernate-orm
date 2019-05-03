/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.noop;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Hill
 */
public class NoopPropertiesTest extends BaseCoreFunctionalTestCase {

	private static final Map<String, Integer> NAME_TO_ORDER_MAP = new HashMap<String, Integer>() {{
		put( "Harry", 10 );
		put( "Sally", 20 );
		put( "Frank", 30 );
		put( "Mary", 40 );
		put( "Jill", 50 );
		put( "Jane", 60 );
	}};

	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/noop/Noop.hbm.xml" };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13389")
	public void testNoopAccessorSqlUpdate() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		prepareData( s, false );
		s.flush();
		s.clear();
		assertQueryResults( s );
		s.createQuery( "delete Person" ).executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13389")
	public void testNoopAccessorHqlUpdate() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		prepareData( s, true );
		s.flush();
		s.clear();
		assertQueryResults( s );
		s.createQuery( "delete Person" ).executeUpdate();
		t.commit();
		s.close();
	}

	private void prepareData(Session s, boolean hql) {
		NAME_TO_ORDER_MAP.forEach( (name, order) -> {
			Person p = new Person();
			p.setName( name );
			s.persist( p );
			s.flush();
			if ( hql ) {
				s.createQuery( "UPDATE Person SET personOrder = :personOrder WHERE name = :name" )
						.setParameter( "personOrder", order )
						.setParameter( "name", name )
						.executeUpdate();
			}
			else {
				s.createSQLQuery( "UPDATE PROPREF_PERS SET PERSON_ORDER = " + order + " WHERE NAME = '" + name + "';" )
						.executeUpdate();
			}
		} );
	}

	private void assertQueryResults(Session s) {
		{
			List<Person> persons = s.createQuery( "FROM Person p ORDER BY personOrder DESC", Person.class ).list();
			List<String> personNames = persons.stream().map( Person::getName ).collect( Collectors.toList() );
			assertEquals( Arrays.asList( "Jane", "Jill", "Mary", "Frank", "Sally", "Harry" ), personNames );
		}
		{
			List<Person> persons = s.createQuery(
					"FROM Person p WHERE personOrder < :personOrder ORDER BY personOrder",
					Person.class
			).setParameter( "personOrder", 40 ).list();
			List<String> personNames = persons.stream().map( Person::getName ).collect( Collectors.toList() );
			assertEquals( Arrays.asList( "Harry", "Sally", "Frank" ), personNames );
		}
	}
}
