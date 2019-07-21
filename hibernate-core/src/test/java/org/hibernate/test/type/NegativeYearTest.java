/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Yordan Gigov
 */
public class NegativeYearTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
			A.class
		};
	}

	@Test
	public void testNegative() {
		doInHibernate( this::sessionFactory, em -> {
			LocalDate ld = LocalDate.of( -200, 01, 01 );
			LocalDateTime ldt = LocalDateTime.of( ld, LocalTime.of( 12, 0 ) );
			OffsetDateTime odt = OffsetDateTime.of( ldt, ZoneOffset.ofHours( -6 ) );
			A a = new A();
			a.setId( 1 );
			a.setTheDate( ld );
			a.setTheDateTime( ldt );
			a.setOffsetDateTime( odt );
			em.persist( a );
			em.flush();
			em.clear();
			A b = em.find( A.class, a.getId() );
			assertEquals( a.getTheDate(), b.getTheDate() );
			assertEquals( a.getTheDateTime(), b.getTheDateTime() );
			// Not testing using .equals(Object) method, because original
			// timezone offset is often discarded in the databases, and also
			// in java.sql.Timestamp, and an absolute time is used with
			// conversion to global/session timezone.
			assertTrue( a.getOffsetDateTime().isEqual( b.getOffsetDateTime() ) );
		} );
	}

	@Entity(name = "A")
	public static class A {

		@Id
		private Integer id;

		@Column
		private LocalDate theDate;

		@Column
		private LocalDateTime theDateTime;

		@Column
		private OffsetDateTime offsetDateTime;

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

		public LocalDateTime getTheDateTime() {
			return theDateTime;
		}

		public void setTheDateTime(LocalDateTime theDateTime) {
			this.theDateTime = theDateTime;
		}

		public OffsetDateTime getOffsetDateTime() {
			return offsetDateTime;
		}

		public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
			this.offsetDateTime = offsetDateTime;
		}

	}

}
