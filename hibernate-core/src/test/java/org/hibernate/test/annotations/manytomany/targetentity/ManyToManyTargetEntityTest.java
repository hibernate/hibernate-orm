/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.manytomany.targetentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner (extracted from ManyToManyTest authored by Emmanuel Bernard)
 */
public class ManyToManyTargetEntityTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testBasic() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Employer er = new Employer();
		Employee ee = new Employee();
		s.persist( ee );
		Set erColl = new HashSet();
		Collection eeColl = new ArrayList();
		erColl.add( ee );
		eeColl.add( er );
		er.setEmployees( erColl );
		ee.setEmployers( eeColl );
		//s.persist(ee);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		er = (Employer) s.load( Employer.class, er.getId() );
		assertNotNull( er );
		assertNotNull( er.getEmployees() );
		assertEquals( 1, er.getEmployees().size() );
		Employee eeFromDb = (Employee) er.getEmployees().iterator().next();
		assertEquals( ee.getId(), eeFromDb.getId() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		ee = (Employee) s.get( Employee.class, ee.getId() );
		assertNotNull( ee );
		assertFalse( "ManyToMany mappedBy lazyness", Hibernate.isInitialized( ee.getEmployers() ) );
		tx.commit();
		assertFalse( "ManyToMany mappedBy lazyness", Hibernate.isInitialized( ee.getEmployers() ) );
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		ee = (Employee) s.get( Employee.class, ee.getId() );
		assertNotNull( ee );
		er = ee.getEmployers().iterator().next();
		assertTrue( "second join non lazy", Hibernate.isInitialized( er ) );
		s.delete( er );
		s.delete( ee );
		tx.commit();
		s.close();
	}

	@Test
	public void testOrderByEmployee() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Employer employer = new Employer();
		Employee employee1 = new Employee();
		employee1.setName( "Emmanuel" );
		Employee employee2 = new Employee();
		employee2.setName( "Alice" );
		s.persist( employee1 );
		s.persist( employee2 );
		Set erColl = new HashSet();
		Collection eeColl = new ArrayList();
		Collection eeColl2 = new ArrayList();
		erColl.add( employee1 );
		erColl.add( employee2 );
		eeColl.add( employer );
		eeColl2.add( employer );
		employer.setEmployees( erColl );
		employee1.setEmployers( eeColl );
		employee2.setEmployers( eeColl2 );

		s.flush();
		s.clear();

		employer = (Employer) s.get( Employer.class, employer.getId() );
		assertNotNull( employer );
		assertNotNull( employer.getEmployees() );
		assertEquals( 2, employer.getEmployees().size() );
		Employee eeFromDb = (Employee) employer.getEmployees().iterator().next();
		assertEquals( employee2.getName(), eeFromDb.getName() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Employee.class,
				Employer.class
		};
	}

}
