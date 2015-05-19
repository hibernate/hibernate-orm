/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter.hql;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Tests for application of filters
 *
 * @author Steve Ebersole
 */
public class BasicFilteredBulkManipulationTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[]{
			"filter/hql/filter-defs.hbm.xml",
			"filter/hql/Basic.hbm.xml"
		};
	}

	@Test
	public void testBasicFilteredHqlDelete() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Person( "Steve", 'M' ) );
		s.save( new Person( "Emmanuel", 'M' ) );
		s.save( new Person( "Gail", 'F' ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", new Character( 'M' ) );
		int count = s.createQuery( "delete Person" ).executeUpdate();
		assertEquals( 2, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testBasicFilteredHqlUpdate() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Person( "Shawn", 'M' ) );
		s.save( new Person( "Sally", 'F' ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", new Character( 'M' ) );
		int count = s.createQuery( "update Person p set p.name = 'Shawn'" ).executeUpdate();
		assertEquals( 1, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
