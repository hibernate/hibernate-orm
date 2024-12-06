/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Classification;
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
import org.hibernate.query.spi.QueryImplementor;
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
					byte[] bytes1 = (byte[]) session.createQuery( "select X'DEADBEEF'" ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes1), is( "deadbeef") );
					byte[] bytes2 = (byte[]) session.createQuery( "select X'deadbeef'" ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes2), is( "deadbeef") );

					byte[] bytes3 = (byte[]) session.createQuery( "select {0xDE, 0xAD, 0xBE, 0xEF}" ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes3), is( "deadbeef") );
					byte[] bytes4 = (byte[]) session.createQuery( "select {0xde, 0xad, 0xbe, 0xef}" ).getSingleResult();
					assertThat( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes4), is( "deadbeef") );
				}
		);
	}

	@Test
	@JiraKey("HHH-16737")
	public void testUntypedIntegralLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select 1 from Human h where h.bigIntegerValue = 9876543210" ).getResultList();
					session.createQuery( "select 1 from Human h where h.bigIntegerValue = 98765432109876543210" ).getResultList();
				}
		);
	}

	@Test
	@JiraKey("HHH-16737")
	public void testUntypedDecimalLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select 1 from Human h where h.bigDecimalValue = 199999.99f" ).getResultList();
				}
		);
	}

	@Test
	public void testJavaString(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select \"\\n\"" ).getSingleResult(), is( "\n" ) );
					assertThat( session.createQuery( "select J'\\n'" ).getSingleResult(), is( "\n" ) );
					assertThat( session.createQuery( "select J'\\''" ).getSingleResult(), is( "'" ) );
				}
		);
	}

	@Test
	public void testJdbcTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t 12:30:00}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}" ).list();
				}
		);
	}

	@Test
	public void testJdbcDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d 1999-12-31}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d '1999-12-31'}" ).list();
				}
		);
	}

	@Test
	public void testJdbcTimestampLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts 1999-12-31 12:30:00}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '1999-12-31 12:30:00'}" ).list();
				}
		);
	}

	@Test
	public void testLocalDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDate = {1999-12-31}" ).list();
				}
		);
	}

	@Test
	public void testLocalTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalTime = {12:59:59}" ).list();
				}
		);
	}

	@Test
	public void testDateTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// todo (6.0) : removed this difference between the string-literal form and the date-time-field form (with/without 'T')

					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDateTime = {1999-12-31 12:59:59}" ).list();

					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 +01:00}" ).list();

					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 CST}" ).list();
				}
		);
	}

	@Test
	public void isolated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theLocalDateTime = {1999-12-31 12:59:59}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theZonedDateTime = {1999-12-31 12:59:59 +01:00}" ).list();
				}
		);
	}

	@Test
	public void testTimestampLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00'}" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00" )
							.list();
				}
		);
	}

	@Test
	public void testTimestampLiteralWithOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00+05:00'}" )
							.list();
//					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00+05'}" )
//							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00+05:00" )
							.list();
					//ambiguous, now disallowed!
//					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00+05" )
//							.list();

					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00 GMT'}" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 GMT" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 'GMT'" )
							.list();
				}
		);
	}

	@Test
	public void testTimestampLiteralWithZoneRegionId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00 US/Pacific'}" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 US/Pacific" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 'US/Pacific'" )
							.list();
				}
		);
	}

	@Test
	public void testDateLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d '2018-01-01'}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = date 2018-01-01" ).list();
				}
		);
	}

	@Test
	public void testTimeLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30:00" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30:00.123" ).list();
				}
		);
	}

	@Test
	public void testSelectDatetimeLiterals(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Object localDateTime = session.createQuery("select datetime 1999-07-23 23:59").getSingleResult();
					assertThat( localDateTime, is( instanceOf(LocalDateTime.class) ) );
					assertThat( localDateTime, equalTo(LocalDateTime.of(1999,7,23,23,59)) );
					Object localDate = session.createQuery("select date 1999-07-23").getSingleResult();
					assertThat( localDate, is( instanceOf(LocalDate.class) ) );
					assertThat( localDate, equalTo(LocalDate.of(1999,7,23)) );
					Object localTime = session.createQuery("select time 23:59").getSingleResult();
					assertThat( localTime, is( instanceOf(LocalTime.class) ) );
					assertThat( localTime, equalTo(LocalTime.of(23,59)) );

					assertThat( session.createQuery( "select local datetime" ).getSingleResult(),
							is( instanceOf(LocalDateTime.class) ) );
					assertThat( session.createQuery( "select local date" ).getSingleResult(),
							is( instanceOf(LocalDate.class) ) );
					assertThat( session.createQuery( "select local time" ).getSingleResult(),
							is( instanceOf(LocalTime.class) ) );

					assertThat( session.createQuery( "select instant" ).getSingleResult(),
							is( instanceOf(Instant.class) ) );

					assertThat( session.createQuery( "select current timestamp" ).getSingleResult(),
							is( instanceOf(Timestamp.class) ) );
					assertThat( session.createQuery( "select current date" ).getSingleResult(),
							is( instanceOf(Date.class) ) );
					assertThat( session.createQuery( "select current time" ).getSingleResult(),
							is( instanceOf(Time.class) ) );
				}
		);
	}

	@Test
	public void testBooleanLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select true, false" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theBoolean = true" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theBoolean = false or e1.theBoolean = true" )
							.list();
				}
		);
	}

	@Test
	public void testHexadecimalLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 0x1A2B" )
							.getSingleResult(), is(6699) );
				}
		);
	}

	@Test
	public void testOctalLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 015053" )
							.getSingleResult(), is(6699) );
				}
		);
	}

	@Test
	public void testBigLiterals(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 10000000000000000bi" )
							.getSingleResult(), is( BigInteger.valueOf(10000000000000000L) ) );
					assertThat( session.createQuery( "select 9999999999999.9999bd" )
							.getSingleResult(), is( BigDecimal.valueOf(99999999999999999L, 4) ) );
				}
		);
	}

	@Test
	public void testEnumLiteralInPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Zoo where classification=COOL" ).getResultList();
					session.createQuery( "from Zoo where classification=Classification.COOL" ).getResultList();
					session.createQuery( "from Zoo where classification=org.hibernate.testing.orm.domain.animal.Classification.COOL" ).getResultList();
				}
		);
	}

	@Test
	public void testIntegerLiteralInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 1" ).getSingleResult(), is( 1 ) );
					assertThat( session.createQuery( "select 1_000_000" ).getSingleResult(), is( 1_000_000 ) );
					assertThat( session.createQuery( "select 1_000_000L" ).getSingleResult(), is( 1_000_000L ) );
				}
		);
	}

	@Test
	public void testFloatingPointLiteralInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createQuery( "select 1.0" ).getSingleResult(), is( 1.0 ) );
					assertThat( session.createQuery( "select 123.456" ).getSingleResult(), is( 123.456 ) );
					assertThat( session.createQuery( "select 123.456F" ).getSingleResult(), is( 123.456F ) );
					assertThat( session.createQuery( "select 123.456D" ).getSingleResult(), is( 123.456D ) );
					assertThat( session.createQuery( "select 1.23e45" ).getSingleResult(), is( 1.23e45 ) );
				}
		);
	}

	@Test
	public void testEnumLiteralInSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						final QueryImplementor<Object[]> query = session.createQuery( "select 1, org.hibernate.testing.orm.domain.animal.Classification.LAME" );
						final Object[] result = query.getSingleResult();
						final Object classification = result[ 1 ];

						Assertions.assertThat( classification ).isEqualTo( Classification.LAME );
					}

					{
						final QueryImplementor<Classification> query = session.createQuery( "select org.hibernate.testing.orm.domain.animal.Classification.LAME" );
						final Classification result = query.getSingleResult();

						Assertions.assertThat( result ).isEqualTo( Classification.LAME );
					}
				}
		);
	}
}
