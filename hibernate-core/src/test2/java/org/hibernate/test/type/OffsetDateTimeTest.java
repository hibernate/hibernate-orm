/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.OffsetDateTimeType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10372")
public class OffsetDateTimeTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {OffsetDateTimeEvent.class};
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

		saveOffsetDateTimeEventWithStartDate( expectedStartDate );

		checkSavedOffsetDateTimeIsEqual( expectedStartDate );
		compareSavedOffsetDateTimeWith( expectedStartDate );
	}

	@Test
	public void testOffsetDateTimeWithUTCZoneOffset() {
		final OffsetDateTime expectedStartDate = OffsetDateTime.of(
				1,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.UTC
		);

		saveOffsetDateTimeEventWithStartDate( expectedStartDate );

		checkSavedOffsetDateTimeIsEqual( expectedStartDate );
		compareSavedOffsetDateTimeWith( expectedStartDate );
	}

	@Test
	public void testRetrievingEntityByOffsetDateTime() {

		final OffsetDateTime startDate = OffsetDateTime.of(
				1,
				1,
				1,
				0,
				0,
				0,
				0,
				ZoneOffset.ofHours( 3 )
		);

		saveOffsetDateTimeEventWithStartDate( startDate );

		final Session s = openSession();
		try {
			Query query = s.createQuery( "from OffsetDateTimeEvent o where o.startDate = :date" );
			query.setParameter( "date", startDate, OffsetDateTimeType.INSTANCE );
			List<OffsetDateTimeEvent> list = query.list();
			assertThat( list.size(), is( 1 ) );
		}
		finally {
			s.close();
		}
	}

	private void checkSavedOffsetDateTimeIsEqual(OffsetDateTime startdate) {
		final Session s = openSession();
		try {
			final OffsetDateTimeEvent offsetDateEvent = s.get( OffsetDateTimeEvent.class, 1L );
			assertThat( offsetDateEvent.startDate.isEqual( startdate ), is( true ) );
		}
		finally {
			s.close();
		}
	}

	private void compareSavedOffsetDateTimeWith(OffsetDateTime startdate) {
		final Session s = openSession();
		try {
			final OffsetDateTimeEvent offsetDateEvent = s.get( OffsetDateTimeEvent.class, 1L );
			assertThat(
					OffsetDateTimeType.INSTANCE.getComparator().compare( offsetDateEvent.startDate, startdate ),
					is( 0 )
			);
		}
		finally {
			s.close();
		}
	}

	private void saveOffsetDateTimeEventWithStartDate(OffsetDateTime startdate) {
		final OffsetDateTimeEvent event = new OffsetDateTimeEvent();
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
