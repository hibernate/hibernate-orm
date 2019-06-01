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
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class LiteralTests extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testTimestampLiteral() {
		inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00'}" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime '2018-01-01 12:30:00'" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00" )
							.list();
				}
		);
	}

	@Test
	public void testTimestampLiteralWithOffset() {
		inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00+05:00'}" )
							.list();
//					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00+05'}" )
//							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = offset datetime '2018-01-01 12:30:00+05:00'" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = offset datetime '2018-01-01 12:30:00+05'" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = offset datetime 2018-01-01 12:30:00+05:00" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = offset datetime 2018-01-01 12:30:00+05" )
							.list();

					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00 GMT'}" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime '2018-01-01 12:30:00 GMT'" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 'GMT'" )
							.list();
				}
		);
	}

	@Test
	public void testTimestampLiteralWithZoneRegionId() {
		inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00 US/Pacific'}" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime '2018-01-01 12:30:00 US/Pacific'" )
							.list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTimestamp = datetime 2018-01-01 12:30:00 'US/Pacific'" )
							.list();
				}
		);
	}

	@Test
	public void testDateLiteral() {
		inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = {d '2018-01-01'}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = date '2018-01-01'" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theDate = date 2018-01-01" ).list();
				}
		);
	}

	@Test
	public void testTimeLiteral() {
		inTransaction(
				session -> {
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time '12:30:00'" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30:00" ).list();
					session.createQuery( "from EntityOfBasics e1 where e1.theTime = time 12:30:00.123" ).list();
				}
		);
	}
}
