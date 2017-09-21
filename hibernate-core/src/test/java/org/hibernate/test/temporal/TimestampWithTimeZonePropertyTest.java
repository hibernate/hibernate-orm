/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.temporal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@SkipForDialect(value = PostgreSQL81Dialect.class, comment = "Unsupported Types value: 2,014")
@SkipForDialect(value = H2Dialect.class, comment = "Unknown data type: \"2014\"")
@SkipForDialect(value = MySQLDialect.class, comment = "Unsupported SQL type: TIMESTAMP_WITH_TIMEZONE")
@SkipForDialect(value = Oracle9iDialect.class, comment = "Invalid column type")
public class TimestampWithTimeZonePropertyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}


	@Test
	public void testTime() {
		final OffsetDateTime timestamp = OffsetDateTime.of(
				2017, 5, 23, 18, 21, 57, 0,
				ZoneOffset.ofHoursMinutes( 3, 0 )
		);

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			event.setId( 1L );
			event.setCreatedOn( timestamp );

			entityManager.persist( event );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = entityManager.find( Event.class, 1L );
			assertEquals( timestamp, event.getCreatedOn() );
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private long id;

		@Type( type = "timestamp_with_timezone" )
		private OffsetDateTime createdOn;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public OffsetDateTime getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(OffsetDateTime createdOn) {
			this.createdOn = createdOn;
		}
	}
}
