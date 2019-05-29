/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.domain.StandardDomainModel;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * @author Gavin King
 */
public class FunctionTests extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel(metadataSources);
	}

	@Test
	public void testCurrentDateTimeFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select current_time, current_date, current_timestamp from EntityOfBasics e")
							.list();
					session.createQuery("select current time, current date, current datetime from EntityOfBasics e")
							.list();
					session.createQuery("from EntityOfBasics e where e.theDate > current_date and e.theTime > current_time and e.theTimestamp > current_timestamp")
							.list();
					session.createQuery("from EntityOfBasics e where e.theDate > current date and e.theTime > current time and e.theTimestamp > current datetime")
							.list();
					session.createQuery("select current_instant, current instant from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testConcatFunction() {
		inTransaction(
				session -> {
					session.createQuery("select concat('foo', e.theString, 'bar') from EntityOfBasics e")
							.list();
					session.createQuery("select 'foo' || e.theString || 'bar' from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testCoalesceFunction() {
		inTransaction(
				session -> {
					session.createQuery("select coalesce(null, e.gender, e.convertedGender, e.ordinalGender) from EntityOfBasics e")
							.list();
					session.createQuery("select ifnull(e.gender, e.convertedGender) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testNullifFunction() {
		inTransaction(
				session -> {
					session.createQuery("select nullif(e.theString, '') from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testTrigFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select sin(e.theDouble), cos(e.theDouble), tan(e.theDouble), asin(e.theDouble), acos(e.theDouble), atan(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select atan2(sin(e.theDouble), cos(e.theDouble)) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testMathFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select abs(e.theInt), sign(e.theInt), mod(e.theInt, 2) from EntityOfBasics e")
							.list();
					session.createQuery("select abs(e.theDouble), sign(e.theDouble), sqrt(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select exp(e.theDouble), ln(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select power(e.theDouble, 2.5) from EntityOfBasics e")
							.list();
					session.createQuery("select ceiling(e.theDouble), floor(e.theDouble) from EntityOfBasics e")
							.list();
					session.createQuery("select round(e.theDouble, 3) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testTimestampAddDiffFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select function('timestampadd',month,2,current date) from EntityOfBasics e")
							.list();
					session.createQuery("select function('timestampdiff',hour,e.theTimestamp,current datetime) from EntityOfBasics e")
							.list();

					session.createQuery("select timestampadd(month,2,current date) from EntityOfBasics e")
							.list();
					session.createQuery("select timestampdiff(hour,e.theTimestamp,current datetime) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testAsciiChrFunctions() {
		inTransaction(
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
	public void testLowerUpperFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select lower(e.theString), upper(e.theString) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testLengthFunction() {
		inTransaction(
				session -> {
					session.createQuery("select length(e.theString) from EntityOfBasics e where length(e.theString) > 1")
							.list();
				}
		);
	}

	@Test
	public void testSubstringFunction() {
		inTransaction(
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
	public void testPositionFunction() {
		inTransaction(
				session -> {
					session.createQuery("select position('hello' in e.theString) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testLocateFunction() {
		inTransaction(
				session -> {
					session.createQuery("select locate('hello', e.theString) from EntityOfBasics e")
							.list();
					session.createQuery("select locate('hello', e.theString, e.theInteger) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testReplaceFunction() {
		inTransaction(
				session -> {
					session.createQuery("select replace(e.theString, 'hello', 'goodbye') from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testTrimFunction() {
		inTransaction(
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
	public void testCastFunction() {
		inTransaction(
				session -> {
					session.createQuery("select cast(e.theDate as string), cast(e.theTime as string), cast(e.theTimestamp as string) from EntityOfBasics e")
							.list();
					session.createQuery("select cast(e.id as string), cast(e.theInt as string), cast(e.theDouble as string) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testStrFunction() {
		inTransaction(
				session -> {
					session.createQuery("select str(e.theDate), str(e.theTime), str(e.theTimestamp) from EntityOfBasics e")
							.list();
					session.createQuery("select str(e.id), str(e.theInt), str(e.theDouble) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testIntervalAddExpressions() {
		inTransaction(
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
	public void testIntervalSubExpressions() {
		inTransaction(
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
	public void testIntervalAddSubExpressions() {
		inTransaction(
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
	public void testIntervalScaleExpressions() {
		inTransaction(
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
	public void testIntervalDiffExpressions() {
		inTransaction(
				session -> {
					session.createQuery("select (e.theDate - e.theDate) by year from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theDate - e.theDate) by month from EntityOfBasics e")
							.list();
					session.createQuery("select (e.theDate - e.theDate) by day from EntityOfBasics e")
							.list();

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


					session.createQuery("select current_timestamp - (current_timestamp - e.theTimestamp) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testExtractFunction() {
		inTransaction(
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

					session.createQuery("select extract(offset hour from e.theTime) from EntityOfBasics e")
							.list();
					session.createQuery("select extract(offset hour minute from e.theTime) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(offset from e.theTimestamp) from EntityOfBasics e")
							.list();

					session.createQuery("select extract(time from e.theTimestamp), extract(date from e.theTimestamp) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testExtractFunctions() {
		inTransaction(
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
	public void testLeastGreatestFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select least(1, 2, e.theInt, -1), greatest(1, e.theInt, 2, -1) from EntityOfBasics e")
							.list();
					session.createQuery("select least(0.0, e.theDouble), greatest(0.0, e.theDouble, 2.0) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testCountFunction() {
		inTransaction(
				session -> {
					session.createQuery("select count(*) from EntityOfBasics e")
							.list();
					session.createQuery("select count(e) from EntityOfBasics e")
							.list();
					session.createQuery("select count(distinct e) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testAggregateFunctions() {
		inTransaction(
				session -> {
					session.createQuery("select avg(e.theDouble), avg(abs(e.theDouble)), min(e.theDouble), max(e.theDouble), sum(e.theDouble), sum(e.theInt) from EntityOfBasics e")
							.list();
					session.createQuery("select avg(distinct e.theInt), sum(distinct e.theInt) from EntityOfBasics e")
							.list();
				}
		);
	}

	@Test
	public void testFormat() {
		inTransaction(
				session -> {
					EntityOfBasics entity = new EntityOfBasics();
					entity.setId(123);
					entity.setTheDate( new Date( 1974, 3, 25 ) );
					entity.setTheTime( new Time( System.currentTimeMillis() ) );
					entity.setTheTimestamp( new Timestamp( System.currentTimeMillis() ) );
					session.persist(entity);
					session.flush();

					session.createQuery("select format(e.theTime as 'hh:mm:ss aa') from EntityOfBasics e")
							.list();
					session.createQuery("select format(e.theDate as 'dd/MM/yy'), format(e.theDate as 'EEEE, MMMM dd, yyyy') from EntityOfBasics e")
							.list();
					session.createQuery("select format(e.theTimestamp as 'dd/MM/yyyy ''at'' HH:mm:ss') from EntityOfBasics e")
							.list();
				}
		);
	}

}