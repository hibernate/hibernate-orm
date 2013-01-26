/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
