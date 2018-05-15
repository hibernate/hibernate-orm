/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.ZonedDateTimeType;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class ZonedDateTimeTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {ZonedDateTimeEvent.class};
	}

	@Test
	public void testZoneDateTimeWithHoursZoneOffset() {
		final ZonedDateTime expectedStartDate = ZonedDateTime.of(
				2015,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.ofHours( 5 )
		);

		saveZoneDateTimeEventWithStartDate( expectedStartDate );

		checkSavedZonedDateTimeIsEqual( expectedStartDate );
		compareSavedZonedDateTimeWith( expectedStartDate );
	}

	@Test
	public void testZoneDateTimeWithUTCZoneOffset() {
		final ZonedDateTime expectedStartDate = ZonedDateTime.of(
				1,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.UTC
		);

		saveZoneDateTimeEventWithStartDate( expectedStartDate );

		checkSavedZonedDateTimeIsEqual( expectedStartDate );
		compareSavedZonedDateTimeWith( expectedStartDate );
	}

	@Test
	public void testRetrievingEntityByZoneDateTime() {

		final ZonedDateTime startDate = ZonedDateTime.of(
				1,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.ofHours( 3 )
		);

		saveZoneDateTimeEventWithStartDate( startDate );

		final Session s = openSession();
		try {
			Query query = s.createQuery( "from ZonedDateTimeEvent o where o.startDate = :date" );
			query.setParameter( "date", startDate, ZonedDateTimeType.INSTANCE );
			List<ZonedDateTimeEvent> list = query.list();
			assertThat( list.size(), is( 1 ) );
		}
		finally {
			s.close();
		}
	}

	private void checkSavedZonedDateTimeIsEqual(ZonedDateTime startdate) {
		final Session s = openSession();
		try {
			final ZonedDateTimeEvent zonedDateTimeEvent = s.get( ZonedDateTimeEvent.class, 1L );
			assertThat( zonedDateTimeEvent.startDate.isEqual( startdate ), is( true ) );
		}
		finally {
			s.close();
		}
	}

	private void compareSavedZonedDateTimeWith(ZonedDateTime startdate) {
		final Session s = openSession();
		try {
			final ZonedDateTimeEvent zonedDateTimeEvent = s.get( ZonedDateTimeEvent.class, 1L );
			assertThat(
					ZonedDateTimeType.INSTANCE.getComparator().compare( zonedDateTimeEvent.startDate, startdate ),
					is( 0 )
			);
		}
		finally {
			s.close();
		}
	}

	private void saveZoneDateTimeEventWithStartDate(ZonedDateTime startdate) {
		final ZonedDateTimeEvent event = new ZonedDateTimeEvent();
		event.id = 1L;
		event.startDate = startdate;

		final Session s = openSession();
		s.getTransaction().begin();
		try {
			s.save( event );
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

	@Entity(name = "ZonedDateTimeEvent")
	@Table(name = "ZONE_DATE_TIME_EVENT")
	public static class ZonedDateTimeEvent {

		@Id
		private Long id;

		@Column(name = "START_DATE")
		private ZonedDateTime startDate;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
