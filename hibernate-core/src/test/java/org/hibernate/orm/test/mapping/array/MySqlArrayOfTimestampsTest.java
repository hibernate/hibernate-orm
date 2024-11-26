/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.array;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = MySqlArrayOfTimestampsTest.Foo.class)
@SessionFactory
@JiraKey("HHH-18881")
class MySqlArrayOfTimestampsTest {

	private static final LocalDateTime[] dataArray = {
			// Unix epoch start if you're in the UK
			LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0, 0 ),
			// pre-Y2K
			LocalDateTime.of( 1999, Month.DECEMBER, 31, 23, 59, 59, 0 ),
			// We survived! Why was anyone worried?
			LocalDateTime.of( 2000, Month.JANUARY, 1, 0, 0, 0, 0 ),
			// Silence will fall!
			LocalDateTime.of( 2010, Month.JUNE, 26, 20, 4, 0, 0 ),
			// 2024 summer time
			LocalDateTime.of( 2024, 6, 20, 0, 0, 0 ),
			// 2023 winer time
			LocalDateTime.of( 2023, 12, 22, 0, 0, 0 )
	};

	private TimeZone currentDefault;

	@BeforeAll
	void setTimeZone() {
		currentDefault = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Zagreb" ) );
	}

	@AfterAll
	void restoreTimeZone() {
		TimeZone.setDefault( currentDefault );
	}

	@Test
	@Order(1)
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(MariaDBDialect.class)
	public void testLocalDateTime(SessionFactoryScope scope) {

		final Integer basicId = scope.fromTransaction( session -> {
			Foo basic = new Foo();
			basic.localDateTimeArray = dataArray;
			basic.localDateTimeField = dataArray[0];
			session.persist( basic );
			return basic.id;
		} );

		scope.inTransaction( session -> {
			Foo found = session.find( Foo.class, basicId );
			assertThat( found.localDateTimeField ).isEqualTo( dataArray[0] );
			assertThat( found.localDateTimeArray ).isEqualTo( dataArray );
		} );
	}


	@Test
	@Order(2)
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(MariaDBDialect.class)
	public void testDate(SessionFactoryScope scope) {
		Date[] dataArray = {Calendar.getInstance().getTime(), Calendar.getInstance().getTime()};

		final Integer basicId = scope.fromTransaction( session -> {
			Foo basic = new Foo();
			basic.dateArray = dataArray;
			basic.dateField = dataArray[0];
			session.persist( basic );
			return basic.id;
		} );

		scope.inTransaction( session -> {
			Foo found = session.find( Foo.class, basicId );
			assertThat( found.dateField.getTime() ).isEqualTo( dataArray[0].getTime() );
			for ( int i = 0; i < dataArray.length; i++ ) {
				assertThat( found.dateArray[i].getTime() ).isEqualTo( dataArray[i].getTime() );
			}
		} );
	}

	private static final LocalDateTime SUMMER = LocalDate.of( 2024, 6, 20 ).atStartOfDay();
	private static final LocalDateTime WINTER = LocalDate.of( 2023, 12, 22 ).atStartOfDay();
	private static final LocalDate EPOCH = LocalDate.of( 1970, Month.JANUARY, 1 );

	private static final TimeZone[] TEST_TIME_ZONES = Stream.of(
			"Africa/Monrovia",
			"Europe/Zagreb",
			"Asia/Singapore",
			"Europe/Tallinn",
			"Europe/Minsk",
			"America/Anchorage"
	).map( TimeZone::getTimeZone ).toArray( TimeZone[]::new );

	@Test
	void encodeThenDecodeLocalDateTime() {
		for ( final TimeZone zone : TEST_TIME_ZONES ) {
			final TimeZone currentTimeZone = TimeZone.getDefault();
			TimeZone.setDefault( zone );
			try {
				for ( LocalDateTime dateTime : dataArray ) {
					final MySqlAppender appender = new MySqlAppender();
					final Timestamp expected = Timestamp.valueOf( dateTime );
					JdbcTimestampJavaType.INSTANCE.appendEncodedString( appender, expected );
					final Date actual = JdbcTimestampJavaType.INSTANCE.fromEncodedString( appender.stringBuilder, 0,
							appender.stringBuilder.length() );
					Assertions.assertEquals( expected, actual );
				}
			}
			finally {
				TimeZone.setDefault( currentTimeZone );
			}
		}
	}

	@Entity(name = "Foo")
	public static class Foo {
		@Id
		@GeneratedValue
		public Integer id;
		public Date[] dateArray;
		public LocalDateTime[] localDateTimeArray;
		public Date dateField;
		public LocalDateTime localDateTimeField;
	}

	private static class MySqlAppender implements SqlAppender {

		private final StringBuilder stringBuilder = new StringBuilder();

		@Override
		public void appendSql(String fragment) {
			stringBuilder.append( fragment );
		}
	}
}
