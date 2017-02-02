/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10371")
public class LocalDateTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final LocalDate expectedLocalDate = LocalDate.of( 1, 1, 1 );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {LocalDateEvent.class};
	}

	@Before
	public void setUp() {
		final Session s = openSession();
		s.getTransaction().begin();
		try {
			s.save( new LocalDateEvent( 1L, expectedLocalDate ) );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testLocalDate() {
		final Session s = openSession();
		try {
			final LocalDateEvent localDateEvent = s.get( LocalDateEvent.class, 1L );
			assertThat( localDateEvent.getStartDate(), is( expectedLocalDate ) );
		}
		finally {
			s.close();
		}
	}

	@Entity(name = "LocalDateEvent")
	@Table(name = "LOCAL_DATE_EVENT")
	public static class LocalDateEvent {
		private Long id;
		private LocalDate startDate;

		public LocalDateEvent() {
		}

		public LocalDateEvent(Long id, LocalDate startDate) {
			this.id = id;
			this.startDate = startDate;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column(name = "START_DATE")
		public LocalDate getStartDate() {
			return startDate;
		}

		public void setStartDate(LocalDate startDate) {
			this.startDate = startDate;
		}
	}
}
