package org.hibernate.orm.test.type.descriptor.java;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@BaseUnitTest
public class JdbcTimeJavaTypeDescriptorTest {

	@Test
	@JiraKey("HHH-17229")
	public void testUnwrap() {
		final JavaType<Date> javaType = JdbcTimeJavaType.INSTANCE;

		final Time sqlTime = new Time(
				LocalDate.EPOCH.atTime( LocalTime.of( 0, 1, 2, 0 ) )
						.toInstant( ZoneOffset.ofHours( 4 ) )
						.plusMillis( 123 )
						.toEpochMilli()
		);
		final LocalTime wrappedSqlTime = javaType.unwrap( sqlTime, LocalTime.class, null );
		assertThat( wrappedSqlTime ).isEqualTo( LocalTime.of( 20, 1, 2, 123_000_000 ) );
	}
}
