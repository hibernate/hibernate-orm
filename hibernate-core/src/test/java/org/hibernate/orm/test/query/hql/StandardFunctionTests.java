/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.dialect.CockroachDialect;

import org.hamcrest.number.IsCloseTo;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;
import static org.hibernate.testing.orm.domain.gambit.EntityOfBasics.Gender.FEMALE;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("deprecation")
@ServiceRegistry
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory
public class StandardFunctionTests {

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityOfBasics entity = new EntityOfBasics();
					entity.setId(123);
					entity.setTheDate( new Date( 74, 2, 25 ) );
					entity.setTheTime( new Time( 20, 10, 8 ) );
					entity.setTheTimestamp( new Timestamp( 121, 4, 27, 13, 22, 50, 123456789 ) );
					entity.setTheZonedDateTime( ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") ) );
					em.persist(entity);
				}
		);
	}

	@Test
	public void currentTimestampTests(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select current_timestamp from EntityOfBasics" ).list();
					session.createQuery( "select current_timestamp() from EntityOfBasics" ).list();

					session.createQuery( "select e from EntityOfBasics e where e.theTimestamp = current_timestamp" ).list();
					session.createQuery( "select e from EntityOfBasics e where e.theTimestamp = current_timestamp()" ).list();

					session.createQuery( "select e from EntityOfBasics e where current_timestamp between e.theTimestamp and e.theTimestamp" ).list();
					session.createQuery( "select e from EntityOfBasics e where current_timestamp() between e.theTimestamp and e.theTimestamp" ).list();
				}
		);
	}

	@Test
	public void currentDateTests(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select current_date from EntityOfBasics" ).list();
					session.createQuery( "select current_date() from EntityOfBasics" ).list();

					session.createQuery( "select e from EntityOfBasics e where e.theDate = current_date" ).list();
					session.createQuery( "select e from EntityOfBasics e where e.theDate = current_date()" ).list();

					session.createQuery( "select e from EntityOfBasics e where current_date between e.theDate and e.theDate" ).list();
					session.createQuery( "select e from EntityOfBasics e where current_date() between e.theDate and e.theDate" ).list();
				}
		);
	}

	@Test
	public void currentInstantTests(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select current_instant from EntityOfBasics" ).list();
					session.createQuery( "select current_instant() from EntityOfBasics" ).list();

					session.createQuery( "select e from EntityOfBasics e where e.theInstant = current_instant" ).list();
					session.createQuery( "select e from EntityOfBasics e where e.theInstant = current_instant()" ).list();

					session.createQuery( "select e from EntityOfBasics e where current_instant between e.theInstant and e.theInstant" ).list();
					session.createQuery( "select e from EntityOfBasics e where current_instant() between e.theInstant and e.theInstant" ).list();
				}
		);
	}

	@Test
	public void localDateTimeTests(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select local_datetime from EntityOfBasics" ).list();
					session.createQuery( "select local_datetime() from EntityOfBasics" ).list();

					session.createQuery( "select e from EntityOfBasics e where e.theTimestamp = local_datetime" ).list();
					session.createQuery( "select e from EntityOfBasics e where e.theTimestamp = local_datetime()" ).list();

					session.createQuery( "select e from EntityOfBasics e where local_datetime between e.theTimestamp and e.theTimestamp" ).list();
					session.createQuery( "select e from EntityOfBasics e where local_datetime() between e.theTimestamp and e.theTimestamp" ).list();
				}
		);
	}

	@Test
	public void localDateTests(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select local_date from EntityOfBasics" ).list();
					session.createQuery( "select local_date() from EntityOfBasics" ).list();

					session.createQuery( "select e from EntityOfBasics e where e.theDate = local_date" ).list();
					session.createQuery( "select e from EntityOfBasics e where e.theDate = local_date()" ).list();

					session.createQuery( "select e from EntityOfBasics e where local_date between e.theDate and e.theDate" ).list();
					session.createQuery( "select e from EntityOfBasics e where local_date() between e.theDate and e.theDate" ).list();

					assertThat(
							session.createQuery( "select local_date" ).getSingleResult(),
							instanceOf( LocalDate.class )
					);
				}
		);
	}

	@Test
	public void localTimeTests(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select local_time from EntityOfBasics" ).list();
					session.createQuery( "select local_time() from EntityOfBasics" ).list();

					session.createQuery( "select e from EntityOfBasics e where e.theLocalTime = local_time" ).list();
					session.createQuery( "select e from EntityOfBasics e where e.theLocalTime = local_time()" ).list();

					session.createQuery( "select e from EntityOfBasics e where local_time between e.theLocalTime and e.theLocalTime" ).list();
					session.createQuery( "select e from EntityOfBasics e where local_time() between e.theLocalTime and e.theLocalTime" ).list();

					assertThat(
							session.createQuery( "select local_time" ).getSingleResult(),
							instanceOf( LocalTime.class )
					);
				}
		);
	}

	@Test
	public void testConcatFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select concat('foo', e.theString, 'bar') from EntityOfBasics e" ).list();
					session.createQuery( "select 'foo' || e.theString || 'bar' from EntityOfBasics e" ).list();
				}
		);
	}

	@Test
	public void testCoalesceFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					//Derby does not like literal nulls :-/
//					session.createQuery("select coalesce(null, e.gender, org.hibernate.testing.orm.domain.gambit.EntityOfBasics$Gender.MALE) from EntityOfBasics e")
//							.list();
					session.createQuery("select coalesce(nullif(e.gender,org.hibernate.testing.orm.domain.gambit.EntityOfBasics$Gender.FEMALE), e.gender) from EntityOfBasics e")
							.list();
					session.createQuery("select coalesce(nullif(e.gender,?1), e.gender) from EntityOfBasics e")
							.setParameter(1, FEMALE)
							.list();
					session.createQuery("select ifnull(e.gender, org.hibernate.testing.orm.domain.gambit.EntityOfBasics$Gender.MALE) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testNullifFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select nullif(e.theString, '') from EntityOfBasics e").list();
				}
		);
	}

	@Test
	public void testTrigFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select sin(e.theDouble), cos(e.theDouble), tan(e.theDouble), asin(e.theDouble), acos(e.theDouble), atan(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select atan2(sin(e.theDouble), cos(e.theDouble)) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testMathFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select abs(e.theInt), sign(e.theInt), mod(e.theInt, 2) from EntityOfBasics e")
							.list();
					session.createQuery("select abs(e.theDouble), sign(e.theDouble), sqrt(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select exp(e.theDouble), ln(e.theDouble + 1) from EntityOfBasics e")
							.list();
					session.createQuery("select power(e.theDouble + 1, 2.5) from EntityOfBasics e")
							.list();
					session.createQuery("select ceiling(e.theDouble), floor(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select round(cast(e.theDouble as BigDecimal), 3) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testSubstrInsideConcat(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select concat('111', concat('222222', '1')) from EntityOfBasics s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
				}
		);
	}

	@Test
	public void testTimestampAddDiffFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select function('timestampadd',month,2,current date) from EntityOfBasics e")
							.list();
					session.createQuery("select function('timestampdiff',hour,e.theTimestamp,current timestamp) from EntityOfBasics e")
							.list();

					session.createQuery("select timestampadd(month,2,local date) from EntityOfBasics e")
							.list();
					session.createQuery("select timestampdiff(hour,e.theTimestamp,local datetime) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCharCodeConversion.class)
	public void testAsciiChrFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select function('ascii', 'x'), function('chr', 120) from EntityOfBasics w")
							.list();
					session.createQuery("from EntityOfBasics e where function('ascii', 'x') > 0")
							.list();
					session.createQuery("from EntityOfBasics e where function('chr', 120) = 'z'")
							.list();

					session.createQuery("select ascii('x'), chr(120) from EntityOfBasics w")
							.list();
					session.createQuery("from EntityOfBasics e where ascii('x') > 0")
							.list();
					session.createQuery("from EntityOfBasics e where chr(120) = 'z'")
							.list();
				}
		);
	}

	@Test
	public void testLowerUpperFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select lower(e.theString), upper(e.theString) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testLengthFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select length(e.theString) from EntityOfBasics e where length(e.theString) > 1")
							.list();
				}
		);
	}

	@Test
	public void testSubstringFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select substring(e.theString, e.theInt) from EntityOfBasics e")
							.list();
					session.createQuery("select substring(e.theString, 0, e.theInt) from EntityOfBasics e")
							.list();
					session.createQuery("select substring(e.theString from e.theInt) from EntityOfBasics e")
							.list();
					session.createQuery("select substring(e.theString from 0 for e.theInt) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testPositionFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select position('hello' in e.theString) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testLocateFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select locate('hello', e.theString) from EntityOfBasics e")
							.list();
					session.createQuery("select locate('hello', e.theString, e.theInteger) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsReplace.class)
	public void testReplaceFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select replace(e.theString, 'hello', 'goodbye') from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testTrimFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select trim(leading ' ' from e.theString) from EntityOfBasics e")
							.list();
					session.createQuery("select trim(trailing ' ' from e.theString) from EntityOfBasics e")
							.list();
					session.createQuery("select trim(both ' ' from e.theString) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testCastFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select cast(e.theDate as string), cast(e.theTime as string), cast(e.theTimestamp as string) from EntityOfBasics e")
							.list();
					session.createQuery("select cast(e.id as string), cast(e.theInt as string), cast(e.theDouble as string) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testStrFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select str(e.theDate), str(e.theTime), str(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select str(e.id), str(e.theInt), str(e.theDouble) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testIntervalAddExpressions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select e.theDate + 1 year from EntityOfBasics e")
							.list();
					session.createQuery("select e.theDate + 2 month from EntityOfBasics e")
							.list();
					session.createQuery("select e.theDate + 7 day from EntityOfBasics e")
							.list();

					session.createQuery("select e.theTime + 1 hour from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTime + 59 minute from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTime + 30 second from EntityOfBasics e")
							.list();

					session.createQuery("select e.theTimestamp + 1 year from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 2 month from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 7 day from EntityOfBasics e")
							.list();

					session.createQuery("select e.theTimestamp + 1 hour from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 59 minute from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 30 second from EntityOfBasics e")
							.list();

				}
		);
	}

	@Test
	public void testIntervalSubExpressions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select e.theDate - 1 year from EntityOfBasics e")
							.list();
					session.createQuery("select e.theDate - 2 month from EntityOfBasics e")
							.list();
					session.createQuery("select e.theDate - 7 day from EntityOfBasics e")
							.list();

					session.createQuery("select e.theTime - 1 hour from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTime - 59 minute from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTime - 30 second from EntityOfBasics e")
							.list();

					session.createQuery("select e.theTimestamp - 1 year from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp - 2 month from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp - 7 day from EntityOfBasics e")
							.list();

					session.createQuery("select e.theTimestamp - 1 hour from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp - 59 minute from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp - 30 second from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 3.333e-3 second from EntityOfBasics e")
							.list();

				}
		);
	}

	@Test
	public void testIntervalAddSubExpressions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select e.theTimestamp + 4 day - 1 week from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp - 4 day + 2 hour from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + (4 day - 1 week) from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp - (4 day + 2 hour) from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 2 * e.theDuration from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testIntervalScaleExpressions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select e.theTimestamp + 3 * 1 week from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 3 * (4 day - 1 week) from EntityOfBasics e")
							.list();
					session.createQuery("select e.theTimestamp + 3.5 * (4 day - 1 week) from EntityOfBasics e")
							.list();

					session.createQuery("select 4 day by second from EntityOfBasics e")
							.list();
					session.createQuery("select (4 day + 2 hour) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (2 * 4 day) by second from EntityOfBasics e")
							.list();
//					session.createQuery("select (1 year - 1 month) by day from EntityOfBasics e")
//							.list();

					session.createQuery("select (2 * (e.theTimestamp - e.theTimestamp) + 3 * (4 day + 2 hour)) by second from EntityOfBasics e")
							.list();

					session.createQuery("select e.theDuration by second from EntityOfBasics e")
							.list();
					session.createQuery("select (2 * e.theDuration + 3 day) by hour from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testIntervalDiffExpressions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select (e.theDate - e.theDate) by year from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theDate - e.theDate) by month from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theDate - e.theDate) by day from EntityOfBasics e")
							.list();

					session.createQuery("select (e.theTimestamp - e.theTimestamp) by hour from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - e.theTimestamp) by minute from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - e.theTimestamp) by second from EntityOfBasics e")
							.list();

					session.createQuery("select (e.theTimestamp - e.theTimestamp + 4 day) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - (e.theTimestamp + 4 day)) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp + 4 day - e.theTimestamp) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp + 4 day - 2 hour - e.theTimestamp) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - e.theTimestamp + 4 day + 2 hour) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - (e.theTimestamp + 4 day + 2 hour)) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp + (4 day - 1 week) - e.theTimestamp) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - e.theTimestamp + (4 day + 2 hour)) by second from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - (e.theTimestamp + (4 day + 2 hour))) by second from EntityOfBasics e")
							.list();

					// causes numerical overflow on Sybase
//					session.createQuery("select current_timestamp - (current_timestamp - e.theTimestamp) from EntityOfBasics e")
//							.list();
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "unsupported binary operator: <date> - <timestamp(6)>")
	public void testIntervalDiffExpressionsDifferentTypes(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select (e.theDate - e.theTimestamp) by year from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theDate - e.theTimestamp) by month from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theDate - e.theTimestamp) by day from EntityOfBasics e")
							.list();

					session.createQuery("select (e.theTimestamp - e.theDate) by year from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - e.theDate) by month from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theTimestamp - e.theDate) by day from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testExtractFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select extract(year from e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(month from e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(day from e.theDate) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(day of year from e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(day of month from e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(day of week from e.theDate) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(week from e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(quarter from e.theDate) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(hour from e.theTime) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(minute from e.theTime) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(second from e.theTime) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(year from e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(month from e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(day from e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(hour from e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(minute from e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(second from e.theTimestamp) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(time from e.theTimestamp), extract(date from e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(time from local_datetime), extract(date from local_datetime) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(week of month from current_date) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(week of year from current_date) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimezoneTypes.class)
	public void testExtractFunctionTimeZone(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery("select extract(offset hour from e.theZonedDateTime) from EntityOfBasics e")
									.getResultList()
									.get(0),
							anyOf( nullValue(), instanceOf( Integer.class ) )
					);
					assertThat(
							session.createQuery("select extract(offset minute from e.theZonedDateTime) from EntityOfBasics e")
									.getResultList()
									.get(0),
							anyOf( nullValue(), instanceOf( Integer.class ) )
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimezoneTypes.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class, comment = "We extract the offset with a format function")
	public void testExtractFunctionTimeZoneOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> assertThat(
						session.createQuery( "select extract(offset from e.theZonedDateTime) from EntityOfBasics e")
								.getResultList()
								.get( 0 ),
						anyOf( nullValue(), instanceOf(ZoneOffset.class) )
				)
		);
	}

	@Test
	public void isolated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select extract(time from e.theTimestamp), extract(date from e.theTimestamp) from EntityOfBasics e").list();
					session.createQuery("select extract(time from local_datetime), extract(date from local_datetime) from EntityOfBasics e").list();
				}
		);
	}

	@Test
//	@FailureExpected
	public void testExtractFunctionWithAssertions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery(
									"select extract(week of year from {2019-01-01}) from EntityOfBasics b where b.id = 123" )
									.getResultList()
									.get( 0 ),
							is( 1 )
					);
					assertThat(
							session.createQuery(
									"select extract(week of year from {2019-01-01}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 1 )
					);
					assertThat(
							session.createQuery(
									"select extract(week of year from {2019-01-01}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 1 )
					);
					assertThat(
							session.createQuery(
									"select extract(week of year from {2019-01-01}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 1 )
					);

					assertThat(
							session.createQuery(
									"select extract(week of year from {2019-01-05}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 1 )
					);

					assertThat(
							session.createQuery(
									"select extract(week of month from {2019-05-01}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 1 )
					);

					assertThat(
							session.createQuery( "select extract(week from {2019-05-27}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 22 )
					);

					assertThat(
							session.createQuery(
									"select extract(day of year from {2019-05-30}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 150 )
					);
					assertThat(
							session.createQuery(
									"select extract(day of month from {2019-05-27}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 27 )
					);

					assertThat(
							session.createQuery( "select extract(day from {2019-05-31}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 31 )
					);
					assertThat(
							session.createQuery( "select extract(month from {2019-05-31}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 5 )
					);
					assertThat(
							session.createQuery( "select extract(year from {2019-05-31}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 2019 )
					);
					assertThat(
							session.createQuery(
									"select extract(quarter from {2019-05-31}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 2 )
					);

					assertThat(
							session.createQuery(
									"select extract(day of week from {2019-05-27}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 2 )
					);
					assertThat(
							session.createQuery(
									"select extract(day of week from {2019-05-31}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 6 )
					);

					assertThat(
							session.createQuery( "select extract(second from {14:12:10}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 10f )
					);
					assertThat(
							session.createQuery( "select extract(minute from {14:12:10}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 12 )
					);
					assertThat(
							session.createQuery( "select extract(hour from {14:12:10}) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							is( 14 )
					);

					assertThat(
							session.createQuery( "select extract(date from local_datetime) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							instanceOf( LocalDate.class )
					);
					assertThat(
							session.createQuery( "select extract(time from local_datetime) from EntityOfBasics" )
									.getResultList()
									.get( 0 ),
							instanceOf( LocalTime.class )
					);
				}
		);
	}

	@Test
	public void testExtractFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select year(e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select month(e.theDate) from EntityOfBasics e")
							.list();
					session.createQuery("select day(e.theDate) from EntityOfBasics e")
							.list();

					session.createQuery("select hour(e.theTime) from EntityOfBasics e")
							.list();
					session.createQuery("select minute(e.theTime) from EntityOfBasics e")
							.list();
					session.createQuery("select second(e.theTime) from EntityOfBasics e")
							.list();

					session.createQuery("select year(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select month(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select day(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select hour(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select minute(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select second(e.theTimestamp) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testLeastGreatestFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select least(1, 2, e.theInt, -1), greatest(1, e.theInt, 2, -1) from EntityOfBasics e")
							.list();
					session.createQuery("select least(0.0, e.theDouble), greatest(0.0, e.theDouble, 2.0) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testSimpleCountFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select count(*) from EntityOfBasics e")
							.list();
					session.createQuery("select count(1) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testCountFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select count(e) from EntityOfBasics e").list();
					session.createQuery("select count(distinct e) from EntityOfBasics e").list();
				}
		);
	}

	@Test
	public void testAggregateFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery("select avg(e.theDouble), avg(abs(e.theDouble)), min(e.theDouble), max(e.theDouble), sum(e.theDouble), sum(e.theInt) from EntityOfBasics e")
							.list();
					session.createQuery("select sum(distinct e.theInt) from EntityOfBasics e")
							.list();
					session.createQuery("select sum(distinct e.theInt) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	public void testFormat(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
							"select format(e.theDate as 'dd/MM/yy'), format(e.theDate as 'EEEE, MMMM dd, yyyy') from EntityOfBasics e" )
							.list();
					session.createQuery(
							"select format(e.theTimestamp as 'dd/MM/yyyy ''at'' HH:mm:ss') from EntityOfBasics e" )
							.list();

					assertThat(
							session.createQuery(
									"select format(e.theDate as 'EEEE, dd/MM/yyyy') from EntityOfBasics e" )
									.getResultList()
									.get( 0 ),
							is( "Monday, 25/03/1974" )
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "unknown signature: experimental_strftime(time, string)") // could cast the first argument to timestamp to workaround this
	public void testFormatTime(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select format(e.theTime as 'hh:mm:ss a') from EntityOfBasics e" )
							.list();
					assertThat(
							session.createQuery(
											"select format(e.theTime as '''Hello'', hh:mm:ss a') from EntityOfBasics e" )
									.getResultList()
									.get( 0 ),
							isOneOf( "Hello, 08:10:08 PM", "Hello, 08:10:08 pm" )
					);
				}
		);
	}

	@Test
	public void testPi(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery("select pi", Double.class).getSingleResult(),
							IsCloseTo.closeTo( Math.PI, 1e-9 )
					);
				}
		);
	}

	@Test
	public void testDegreesRadians(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery("select degrees(pi)", Double.class).getSingleResult(),
							IsCloseTo.closeTo( 180.0, 1e-9 )
					);
					assertThat(
							session.createQuery("select radians(180.0)", Double.class).getSingleResult(),
							IsCloseTo.closeTo( Math.PI, 1e-9 )
					);
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "unknown signature: log(int, int)") // could cast an argument to double to workaround this
	public void testLog(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat(
							session.createQuery("select log(3,9)", Double.class).getSingleResult(),
							IsCloseTo.closeTo( 2d, 1e-9 )
					);
					assertThat(
							session.createQuery("select log(10,1e12)", Double.class).getSingleResult(),
							IsCloseTo.closeTo( 12d, 1e-9 )
					);
				}
		);
	}
}
