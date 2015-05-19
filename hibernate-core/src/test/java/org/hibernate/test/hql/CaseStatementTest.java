/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
