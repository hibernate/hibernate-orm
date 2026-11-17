package org.hibernate.orm.test.type;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that {@link TimeUtcAsJdbcTimeJdbcType} handles times whose UTC
 * representation falls before or after the epoch day, e.g. {@code 00:00:00+01:00}.
 */
public class TimeUtcAsJdbcTimeJdbcTypeTest {

	private TimeZone originalTimeZone;

	@BeforeEach
	void setUp() {
		originalTimeZone = TimeZone.getDefault();
		// A zone with DST history around the epoch makes java.sql.Time.valueOf unreliable
		TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Berlin" ) );
	}

	@AfterEach
	void tearDown() {
		TimeZone.setDefault( originalTimeZone );
	}

	@Test
	void testBeforeEpoch() {
		// 00:00+01:00 is 23:00 UTC of the previous day, i.e. before the epoch
		final OffsetTime value = OffsetTime.of( 0, 0, 0, 0, ZoneOffset.ofHours( 1 ) );
		assertEquals( -3_600_000L, bind( value ).getTime() );
		assertEquals( value.atDate( LocalDate.EPOCH ).toInstant(), roundTrip( value ).atDate( LocalDate.EPOCH ).toInstant() );
	}

	@Test
	void testAfterEpochDay() {
		// 23:30-02:00 is 01:30 UTC of the next day
		final OffsetTime value = OffsetTime.of( 23, 30, 0, 0, ZoneOffset.ofHours( -2 ) );
		assertEquals( 86_400_000L + 90 * 60_000L, bind( value ).getTime() );
		assertEquals( value.atDate( LocalDate.EPOCH ).toInstant(), roundTrip( value ).atDate( LocalDate.EPOCH ).toInstant() );
	}

	@Test
	void testWithinEpochDay() {
		final OffsetTime value = OffsetTime.of( 12, 15, 30, 500_000_000, ZoneOffset.ofHours( 2 ) );
		assertEquals( 10 * 3_600_000L + 15 * 60_000L + 30_500L, bind( value ).getTime() );
		assertEquals( value.withOffsetSameInstant( ZoneOffset.UTC ), roundTrip( value ) );
	}

	private static Time bind(OffsetTime value) {
		final Time[] bound = new Time[1];
		final PreparedStatement st = (PreparedStatement) Proxy.newProxyInstance(
				TimeUtcAsJdbcTimeJdbcTypeTest.class.getClassLoader(),
				new Class<?>[] { PreparedStatement.class },
				(proxy, method, args) -> {
					if ( method.getName().equals( "setTime" ) ) {
						bound[0] = (Time) args[1];
					}
					return null;
				}
		);
		try {
			TimeUtcAsJdbcTimeJdbcType.INSTANCE.getBinder( OffsetTimeJavaType.INSTANCE ).bind( st, value, 1, null );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
		return bound[0];
	}

	private static OffsetTime roundTrip(OffsetTime value) {
		final Time time = bind( value );
		final ResultSet rs = (ResultSet) Proxy.newProxyInstance(
				TimeUtcAsJdbcTimeJdbcTypeTest.class.getClassLoader(),
				new Class<?>[] { ResultSet.class },
				(proxy, method, args) -> switch ( method.getName() ) {
					case "getTime" -> time;
					case "wasNull" -> false;
					default -> null;
				}
		);
		try {
			return TimeUtcAsJdbcTimeJdbcType.INSTANCE.getExtractor( OffsetTimeJavaType.INSTANCE ).extract( rs, 1, null );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}
}
