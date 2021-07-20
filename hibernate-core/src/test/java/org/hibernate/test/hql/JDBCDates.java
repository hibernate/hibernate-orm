/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.QueryException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Yordan Gigov
 */
@TestForIssue(jiraKey = "HHH-8653")
public class JDBCDates extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
			A.class,
			B.class,
			C.class
		};
	}

	@Test
	public void testDate() {
		doInHibernate( this::sessionFactory, em -> {
			A a = new A();
			a.setId( 1 );
			a.setTheDate( LocalDate.of( 2020, 01, 01 ) );
			em.persist( a );
			em.flush();
			A b = em.createQuery( "SELECT o FROM A o WHERE o.theDate = {d '2020-01-01'}", A.class ).getSingleResult();
			assertEquals( a.getId(), b.getId() );
		} );
	}

	@Test
	public void testTime() {
		doInHibernate( this::sessionFactory, em -> {
			B a = new B();
			a.setId( 1 );
			a.setTheTime( LocalTime.of( 4, 13, 22 ) );
			em.persist(a);
			em.flush();
			B b = em.createQuery( "SELECT o FROM B o WHERE o.theTime = {t '04:13:22'}", B.class ).getSingleResult();
			assertEquals( a.getId(), b.getId() );
		} );
	}

	@Test
	public void testDateTime() {
		doInHibernate( this::sessionFactory, em -> {
			C a = new C();
			a.setId( 1 );
			a.setTheDateTime( LocalDateTime.of( 2020, 01, 01, 12, 2, 44 ) );
			em.persist( a );
			em.flush();
			C b = em.createQuery( "SELECT o FROM C o WHERE o.theDateTime = {ts '2020-01-01 12:02:44'}", C.class ).getSingleResult();
			assertEquals( a.getId(), b.getId() );
		} );
	}

	@Test(expected = QueryException.class)
	public void testInvalidDate1() {
		doInHibernate( this::sessionFactory, em -> {
			try {
				// Bad syntax. Should cause QuerySyntaxException
				// Instead it causes a QueryException that gets wrapped
				// in an IllegalArgumentException
				em.createQuery( "SELECT o FROM A o WHERE o.theDate = { d '2020-01-01'}", A.class ).getResultList();
			}
			catch ( IllegalArgumentException ex ) {
				if ( ex.getCause() instanceof QueryException ) {
					throw (QueryException) ex.getCause();
				}
			}
		} );
	}

	@Test(expected = QueryException.class)

	public void testInvalidTime1() {
		doInHibernate( this::sessionFactory, em -> {
			try {
				em.createQuery( "SELECT o FROM B o WHERE o.theTime = { t '04:13:22'}", B.class ).getResultList();
			}
			catch ( IllegalArgumentException ex ) {
				if ( ex.getCause() instanceof QueryException ) {
					throw (QueryException) ex.getCause();
				}
			}
		} );
	}

	@Test(expected = QueryException.class)
	public void testInvalidDateTime1() {
		doInHibernate( this::sessionFactory, em -> {
			try {
				em.createQuery( "SELECT o FROM C o WHERE o.theDateTime = { ts '2020-01-01 12:02:44'}", C.class ).getResultList();
			}
			catch ( IllegalArgumentException ex ) {
				if ( ex.getCause() instanceof QueryException ) {
					throw (QueryException) ex.getCause();
				}
			}
		} );
	}

	@Test(expected = QueryException.class)
	public void testInvalidDate2() {
		doInHibernate( this::sessionFactory, em -> {
			try {
				em.createQuery( "SELECT o FROM A o WHERE o.theDate = {d '2020-01-01' }", A.class ).getResultList();
			}
			catch ( IllegalArgumentException ex ) {
				if ( ex.getCause() instanceof QueryException ) {
					throw (QueryException) ex.getCause();
				}
			}
		} );
	}

	@Test(expected = QueryException.class)
	public void testInvalidTime2() {
		doInHibernate( this::sessionFactory, em -> {
			try {
				em.createQuery( "SELECT o FROM B o WHERE o.theTime = {t '04:13:22' }", B.class ).getResultList();
			}
			catch ( IllegalArgumentException ex ) {
				if ( ex.getCause() instanceof QueryException ) {
					throw (QueryException) ex.getCause();
				}
			}
		} );
	}

	@Test(expected = QueryException.class)
	public void testInvalidDateTime2() {
		doInHibernate( this::sessionFactory, em -> {
			try {
				em.createQuery( "SELECT o FROM C o WHERE o.theDateTime = {ts '2020-01-01 12:02:44' }", C.class ).getResultList();
			}
			catch ( IllegalArgumentException ex ) {
				if ( ex.getCause() instanceof QueryException ) {
					throw (QueryException) ex.getCause();
				}
			}
		} );
	}

	@Entity(name = "A")
	public static class A {

		@Id
		private Integer id;

		@Column
		private LocalDate theDate;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalDate getTheDate() {
			return theDate;
		}

		public void setTheDate(LocalDate theDate) {
			this.theDate = theDate;
		}
	}

	@Entity(name = "B")
	public static class B {

		@Id
		private Integer id;

		@Column
		private LocalTime theTime;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalTime getTheTime() {
			return theTime;
		}

		public void setTheTime(LocalTime theTime) {
			this.theTime = theTime;
		}
	}

	@Entity(name = "C")
	public static class C {

		@Id
		private Integer id;

		@Column
		private LocalDateTime theDateTime;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LocalDateTime getTheDateTime() {
			return theDateTime;
		}

		public void setTheDateTime(LocalDateTime theDateTime) {
			this.theDateTime = theDateTime;
		}
	}

}
