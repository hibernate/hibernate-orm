/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class CaseStatementTest extends BaseCoreFunctionalTestCase {

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;
		private String name;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void testSimpleCaseStatementFixture() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		s.createQuery( "select case p.name when 'Steve' then 'x' else 'y' end from Person p" )
				.list();

		t.commit();
		s.close();
	}

	@Test
	public void testSimpleCaseStatementWithParamResult() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		s.createQuery( "select case p.name when 'Steve' then :opt1 else p.name end from Person p" )
				.setString( "opt1", "x" )
				.list();

		t.commit();
		s.close();
	}

	@Test
	public void testSimpleCaseStatementWithParamAllResults() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		try {
			s.createQuery( "select case p.name when 'Steve' then :opt1 else :opt2 end from Person p" )
					.setString( "opt1", "x" )
					.setString( "opt2", "y" )
					.list();
			fail( "was expecting an exception" );
		}
		catch (QueryException expected) {
			// expected
		}

		s.createQuery( "select case p.name when 'Steve' then cast( :opt1 as string ) else cast( :opt2 as string) end from Person p" )
				.setString( "opt1", "x" )
				.setString( "opt2", "y" )
				.list();

		t.commit();
		s.close();
	}

	@Test
	public void testSearchedCaseStatementFixture() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		s.createQuery( "select case when p.name = 'Steve' then 'x' else 'y' end from Person p" )
				.list();

		t.commit();
		s.close();
	}

	@Test
	public void testSearchedCaseStatementWithParamResult() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		s.createQuery( "select case when p.name = 'Steve' then :opt1 else p.name end from Person p" )
				.setString( "opt1", "x" )
				.list();

		t.commit();
		s.close();
	}

	@Test
	public void testSearchedCaseStatementWithAllParamResults() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		try {
			s.createQuery( "select case when p.name = 'Steve' then :opt1 else :opt2 end from Person p" )
					.setString( "opt1", "x" )
					.setString( "opt2", "y" )
					.list();
			fail( "was expecting an exception" );
		}
		catch (QueryException expected) {
			// expected
		}

		s.createQuery( "select case when p.name = 'Steve' then cast( :opt1 as string) else :opt2 end from Person p" )
				.setString( "opt1", "x" )
				.setString( "opt2", "y" )
				.list();

		t.commit();
		s.close();
	}
}
