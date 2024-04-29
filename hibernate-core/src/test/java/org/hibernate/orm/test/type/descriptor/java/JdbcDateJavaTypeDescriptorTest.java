package org.hibernate.orm.test.type.descriptor.java;

import java.sql.Date;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.type.descriptor.java.JdbcDateJavaType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@BaseUnitTest
public class JdbcDateJavaTypeDescriptorTest {

	@Test
	public void testToString() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;

		final String utilDate = javaType.toString( new java.util.Date( 0 ) );
		assertThat( utilDate ).isEqualTo( "1970-01-01" );

		final String sqlDate = javaType.toString( new java.sql.Date( 0 ) );
		assertThat( sqlDate ).isEqualTo( "1970-01-01" );
	}

	@Test
	public void testIsInstance() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;

		javaType.isInstance( new java.sql.Date( 0 ) );
		javaType.isInstance( new java.util.Date( 0 ) );
	}

	@Test
	@JiraKey("HHH-18036")
	public void testWrap() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;

		final Date sqlDate = new Date( 0 );
		final java.util.Date wrappedSqlDate = javaType.wrap( sqlDate, null );
		assertThat( wrappedSqlDate ).isSameAs( sqlDate );

		final java.util.Date utilDate = new java.util.Date( 0 );
		final java.util.Date wrappedUtilDate = javaType.wrap( utilDate, null );
		assertThat( wrappedUtilDate ).isInstanceOf( java.sql.Date.class );

		final java.util.Date utilDateWithTime = java.util.Date.from( ZonedDateTime.of(
				2000,
				1,
				1,
				12,
				0,
				0,
				0,
				ZoneOffset.UTC
		).toInstant() );
		final java.util.Date wrappedUtilDateWithTime = javaType.wrap( utilDateWithTime, null );
		assertThat( wrappedUtilDateWithTime ).isEqualTo( java.sql.Date.valueOf( "2000-01-01" ) );
	}
}
