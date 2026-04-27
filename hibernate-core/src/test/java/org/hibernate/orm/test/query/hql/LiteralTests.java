/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Classification;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import org.assertj.core.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("deprecation")
@ServiceRegistry
@DomainModel( standardModels = {StandardDomainModel.GAMBIT, StandardDomainModel.ANIMAL} )
@SessionFactory
public class LiteralTests {

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix does not support binary literals")
	public void testBinaryLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					byte[] bytes1 = session.createQuery( "select X'DEADBEEF'", byte[].class ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes1), is( "deadbeef") );
					byte[] bytes2 = session.createQuery( "select X'deadbeef'", byte[].class ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes2), is( "deadbeef") );

					byte[] bytes3 = session.createQuery( "select {0xDE, 0xAD, 0xBE, 0xEF}", byte[].class ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes3), is( "deadbeef") );
					byte[] bytes4 = session.createQuery( "select {0xde, 0xad, 0xbe, 0xef}", byte[].class ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes4), is( "deadbeef") );
				}
		);
	}

	@Test
	@JiraKey("HHH-16737")
	public void testUntypedIntegralLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select 1 from Human h where h.bigIntegerValue = 9876543210", Integer.class ).getResultList();
					session.createQuery( "select 1 from Human h where h.bigIntegerValue = 98765432109876543210", Integer.class ).getResultList();
				}
		);
	}

	@Test
	@JiraKey("HHH-16737")
	public void testUntypedDecimalLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select 1 from Human h where h.bigDecimalValue = 199999.99f", Integer.class ).getResultList();
				}
		);
	}

	@Test
	public void testJavaString(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select \"\\n\"", String.class ).getSingleResult(), is( "\n" ) );
					assertThat( session.createQuery( "select J'\\n'", String.class ).getSingleResult(), is( "\n" ) );
					assertThat( session.createQuery( "select J'\\''" , String.class ).getSingleResult(), is( "'" ) );
				}
		);
	}

	@Test
	public void testJdbcTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t 12:30:00}", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testJdbcDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d 1999-12-31}", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d '1999-12-31'}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testJdbcTimestampLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts 1999-12-31 12:30:00}", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '1999-12-31 12:30:00'}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testLocalDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDate = {1999-12-31}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testLocalTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalTime = {12:59:59}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testDateTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// todo (6.0) : removed this difference between the string-literal form and the date-time-field form (with/without 'T')

					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDateTime = {1999-12-31 12:59:59}", EntityOfBasics.class ).list();

					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 +01:00}", EntityOfBasics.class ).list();

					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 CST}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void isolated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDateTime = {1999-12-31 12:59:59}", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 +01:00}", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testTimestampLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00'}", EntityOfBasics.class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00", EntityOfBasics.class )
							.list();
				}
		);
	}

	@Test
	public void testTimestampLiteralWithOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00+05:00'}", EntityOfBasics.class )
							.list();
//					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00+05'}", EntityOfBasics.class )
//							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00+05:00", EntityOfBasics.class )
							.list();
					//ambiguous, now disallowed!
//					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00+05", EntityOfBasics.class )
//							.list();

					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00 GMT'}", EntityOfBasics.class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 GMT", EntityOfBasics.class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 'GMT'", EntityOfBasics.class )
							.list();
				}
		);
	}

	@Test
	public void testTimestampLiteralWithZoneRegionId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00 US/Pacific'}", EntityOfBasics.class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 US/Pacific", EntityOfBasics.class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 'US/Pacific'", EntityOfBasics.class )
							.list();
				}
		);
	}

	@Test
	public void testDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d '2018-01-01'}", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = date 2018-01-01", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30:00", EntityOfBasics.class ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30:00.123", EntityOfBasics.class ).list();
				}
		);
	}

	@Test
	public void testSelectDatetimeLiterals(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LocalDateTime localDateTime = session.createQuery("select datetime 1999-07-23 23:59", LocalDateTime.class).getSingleResult();
					assertThat( localDateTime, is( instanceOf(LocalDateTime.class) ) );
					assertThat( localDateTime, equalTo(LocalDateTime.of(1999,7,23,23,59)) );
					LocalDate localDate = session.createQuery("select date 1999-07-23", LocalDate.class).getSingleResult();
					assertThat( localDate, is( instanceOf(LocalDate.class) ) );
					assertThat( localDate, equalTo(LocalDate.of(1999,7,23)) );
					LocalTime localTime = session.createQuery("select time 23:59", LocalTime.class).getSingleResult();
					assertThat( localTime, is( instanceOf(LocalTime.class) ) );
					assertThat( localTime, equalTo(LocalTime.of(23,59)) );

					assertThat( session.createQuery( "select local datetime", LocalDateTime.class ).getSingleResult(),
							is( instanceOf(LocalDateTime.class) ) );
					assertThat( session.createQuery( "select local date", LocalDate.class ).getSingleResult(),
							is( instanceOf(LocalDate.class) ) );
					assertThat( session.createQuery( "select local time", LocalTime.class ).getSingleResult(),
							is( instanceOf(LocalTime.class) ) );

					assertThat( session.createQuery( "select instant", Instant.class ).getSingleResult(),
							is( instanceOf(Instant.class) ) );

					assertThat( session.createQuery( "select current timestamp", Timestamp.class ).getSingleResult(),
							is( instanceOf(Timestamp.class) ) );
					assertThat( session.createQuery( "select current date", Date.class ).getSingleResult(),
							is( instanceOf(Date.class) ) );
					assertThat( session.createQuery( "select current time", Time.class ).getSingleResult(),
							is( instanceOf(Time.class) ) );
				}
		);
	}

	@Test
	public void testBooleanLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select true, false", Object[].class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theBoolean = true", EntityOfBasics.class )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theBoolean = false or e1.theBoolean = true", EntityOfBasics.class )
							.list();
				}
		);
	}

	@Test
	public void testHexadecimalLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 0x1A2B", Integer.class )
							.getSingleResult(), is(6699) );
				}
		);
	}

	@Test
	public void testOctalLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 015053", Integer.class )
							.getSingleResult(), is(6699) );
				}
		);
	}

	@Test
	public void testBigLiterals(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 10000000000000000bi", BigInteger.class )
							.getSingleResult(), is( BigInteger.valueOf(10000000000000000L) ) );
					assertThat( session.createQuery( "select 9999999999999.9999bd", BigDecimal.class )
							.getSingleResult(), is( BigDecimal.valueOf(99999999999999999L, 4) ) );
				}
		);
	}

	@Test
	public void testEnumLiteralInPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Zoo where classification=COOL", Object.class ).getResultList();
					session.createQuery( "from Zoo where classification=Classification.COOL", Object.class ).getResultList();
					session.createQuery( "from Zoo where classification=org.hibernate.testing.orm.domain.animal.Classification.COOL", Object.class ).getResultList();
				}
		);
	}

	@Test
	public void testIntegerLiteralInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 1", Integer.class ).getSingleResult(), is( 1 ) );
					assertThat( session.createQuery( "select 1_000_000", Integer.class ).getSingleResult(), is( 1_000_000 ) );
					assertThat( session.createQuery( "select 1_000_000L", Long.class ).getSingleResult(), is( 1_000_000L ) );
				}
		);
	}

	@Test
	public void testFloatingPointLiteralInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 1.0", Double.class ).getSingleResult(), is( 1.0 ) );
					assertThat( session.createQuery( "select 123.456", Double.class ).getSingleResult(), is( 123.456 ) );
					assertThat( session.createQuery( "select 123.456F", Float.class ).getSingleResult(), is( 123.456F ) );
					assertThat( session.createQuery( "select 123.456D", Double.class ).getSingleResult(), is( 123.456D ) );
					assertThat( session.createQuery( "select 1.23e45", Double.class ).getSingleResult(), is( 1.23e45 ) );
				}
		);
	}

	@Test
	public void testEnumLiteralInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						final var query = session.createQuery( "select 1, org.hibernate.testing.orm.domain.animal.Classification.LAME", Object[].class );
						final Object[] result = query.getSingleResult();
						final Object classification = result[ 1 ];

						Assertions.assertThat( classification ).isEqualTo( Classification.LAME );
					}

					{
						final var query = session.createQuery( "select org.hibernate.testing.orm.domain.animal.Classification.LAME", Classification.class );
						final Classification result = query.getSingleResult();

						Assertions.assertThat( result ).isEqualTo( Classification.LAME );
					}
				}
		);
	}
}
