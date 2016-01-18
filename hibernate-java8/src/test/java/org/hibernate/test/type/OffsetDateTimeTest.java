/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class OffsetDateTimeTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {OffsetDateTimeEvent.class};
	}

	@Test
	public void testOffsetDateTimeWithUTCZoneOffsetAndYearLessThan1848() {
		final OffsetDateTime expectedStartDate = OffsetDateTime.of(
				1847,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.UTC
		);

		testOffsetDateTime( expectedStartDate );
	}

	@Test
	public void testOffsetDateTimeWithHoursZoneOffset() {
		final OffsetDateTime expectedStartDate = OffsetDateTime.of(
				2015,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.ofHours( 5 )
		);

		testOffsetDateTime( expectedStartDate );
	}

	@Test
	public void testOffsetDateTimeWithUTCZoneOffset() {
		final OffsetDateTime expectedStartDate = OffsetDateTime.of(
				1848,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.UTC
		);

		testOffsetDateTime( expectedStartDate );
	}

	private void testOffsetDateTime(OffsetDateTime expectedStartDate) {
		final OffsetDateTimeEvent event = new OffsetDateTimeEvent();
		event.id = 1L;
		event.startDate = expectedStartDate;

		Session s = openSession();
		s.getTransaction().begin();
		try {
			s.save( event );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			final OffsetDateTimeEvent offsetDateEvent = s.get( OffsetDateTimeEvent.class, 1L );
			assertThat( offsetDateEvent.startDate, is( expectedStartDate ) );
		}
		finally {
			s.close();
		}
	}

	@Entity(name = "OffsetDateTimeEvent")
	@Table(name = "OFFSET_DATE_TIME_EVENT")
	public static class OffsetDateTimeEvent {

		@Id
		private Long id;

		@Column(name = "START_DATE")
		private OffsetDateTime startDate;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
