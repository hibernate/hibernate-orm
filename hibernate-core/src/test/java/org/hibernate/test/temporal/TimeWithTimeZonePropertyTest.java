/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.temporal;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@SkipForDialect(value = PostgreSQL81Dialect.class, comment = "Unsupported Types value: 2,013")
@SkipForDialect(value = H2Dialect.class, comment = "Unknown data type: \"2013\"")
@SkipForDialect(value = MySQLDialect.class, comment = "Unsupported SQL type: TIME_WITH_TIMEZONE")
@SkipForDialect(value = Oracle9iDialect.class, comment = "Invalid column type")
@SkipForDialect(value = SQLServer2008Dialect.class, comment = "Comparison mismatch")
public class TimeWithTimeZonePropertyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}

	@Test
	public void testTime() {
		final OffsetTime time = OffsetTime.of(
				18, 22, 19, 0,
				OffsetDateTime.now().getOffset()
		);

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			event.setId( 1L );
			event.setStartsAt( time );

			entityManager.persist( event );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = entityManager.find( Event.class, 1L );
			assertEquals( time, event.getStartsAt() );
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private long id;

		@Type( type = "time_with_timezone" )
		private OffsetTime startsAt;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public OffsetTime getStartsAt() {
			return startsAt;
		}

		public void setStartsAt(OffsetTime startsAt) {
			this.startsAt = startsAt;
		}
	}
}
