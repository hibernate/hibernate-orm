/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone;

import org.hibernate.Hibernate;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Same as {@link LazyToOneTest} except here we have bytecode-enhanced entities
 * via {@link BytecodeEnhancerRunner}
 */
@RunWith( BytecodeEnhancerRunner.class )
public class InstrumentedLazyToOneTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Airport.class, Flight.class };
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void prepareTest() throws Exception {
		inTransaction(
				(session) -> {
					final Airport austin = new Airport( 1, "AUS" );
					final Airport baltimore = new Airport( 2, "BWI" );

					final Flight flight1 = new Flight( 1, "ABC-123", austin, baltimore );
					final Flight flight2 = new Flight( 2, "ABC-987", baltimore, austin );

					session.persist( austin );
					session.persist( baltimore );

					session.persist( flight1 );
					session.persist( flight2 );
				}
		);
	}

	@Override
	protected void cleanupTestData() throws Exception {
		inTransaction(
				(session) -> {
					session.createQuery( "delete Flight" ).executeUpdate();
					session.createQuery( "delete Airport" ).executeUpdate();
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-13658", message = "Flight#origination is not treated as lazy.  Not sure why exactly" )
	public void testEnhancedButProxyNotAllowed() {
		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		inTransaction(
				(session) -> {
					final Flight flight1 = session.byId( Flight.class ).load( 1 );

					// unlike the other 2 tests we should get 2 db queries here
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertThat( Hibernate.isInitialized( flight1 ), is( true ) );

					assertThat( Hibernate.isPropertyInitialized( flight1, "origination" ), is( true ) );
					// this should be a non-enhanced proxy
					assertThat( Hibernate.isInitialized( flight1.getOrigination() ), is( false ) );

					assertThat( Hibernate.isPropertyInitialized( flight1, "destination" ), is( false ) );
					// the NO_PROXY here should trigger an EAGER load
					assertThat( Hibernate.isInitialized( flight1.getDestination() ), is( false ) );
				}
		);
	}
}
