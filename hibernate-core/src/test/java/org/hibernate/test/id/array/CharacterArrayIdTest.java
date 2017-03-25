/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.array;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Piotr Krauzowicz <p.krauzowicz@visiona.pl>
 * @author Gail Badner
 */
public class CharacterArrayIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { DemoEntity.class };
	}

	@Before
	public void prepare() {
		Session s = openSession();
		s.getTransaction().begin();
		for ( int i = 0; i < 3; i++ ) {
			DemoEntity entity = new DemoEntity();
			entity.id = new Character[] {
					(char) ( i + 1 ),
					(char) ( i + 2 ),
					(char) ( i + 3 ),
					(char) ( i + 4 )
			};
			entity.name = "Simple name " + i;
			s.persist( entity );
		}
		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanup() {
		Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from CharacterArrayIdTest$DemoEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	/**
	 * Removes two records from database.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8999")
	public void testMultipleDeletions() {
		Session s = openSession();
		s.getTransaction().begin();
		Query query = s.createQuery( "SELECT s FROM CharacterArrayIdTest$DemoEntity s" );
		List results = query.list();
		s.delete( results.get( 0 ) );
		s.delete( results.get( 1 ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		query = s.createQuery( "SELECT s FROM CharacterArrayIdTest$DemoEntity s" );
		assertEquals( 1, query.list().size() );
		s.getTransaction().commit();
		s.close();
	}

	/**
	 * Updates two records from database.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8999")
	public void testMultipleUpdates() {
		Session s = openSession();
		s.getTransaction().begin();
		Query query = s.createQuery( "SELECT s FROM CharacterArrayIdTest$DemoEntity s" );
		List<DemoEntity> results = (List<DemoEntity>) query.list();
		results.get( 0 ).name = "Different 0";
		results.get( 1 ).name = "Different 1";
		final String lastResultName = results.get( 0 ).name;
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		query = s.createQuery( "SELECT s FROM CharacterArrayIdTest$DemoEntity s" );
		results = (List<DemoEntity>) query.list();
		final Set<String> names = new HashSet<String>(  );
		for ( DemoEntity entity : results ) {
			names.add( entity.name );
		}
		assertTrue( names.contains( "Different 0" ) );
		assertTrue( names.contains( "Different 1" ) );
		assertTrue( names.contains( lastResultName ) );
		s.getTransaction().commit();
		s.close();
	}


	@Entity
	@Table(name="DemoEntity")
	public static class DemoEntity {
		@Id
		public Character[] id;
		public String name;
	}
}
