/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Same as {@link LazyToOneTest} except here we have bytecode-enhanced entities
 * via {@link BytecodeEnhanced}
 */
@DomainModel(
		annotatedClasses = {
				Airport.class, Flight.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class InstrumentedLazyToOneTest {

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
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

	@AfterEach
	protected void cleanupTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				(session) -> {
					session.createMutationQuery( "delete Flight" ).executeUpdate();
					session.createMutationQuery( "delete Airport" ).executeUpdate();
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-13658", reason = "Flight#origination is not treated as lazy.  Not sure why exactly" )
	public void testEnhancedButProxyNotAllowed(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final Flight flight1 = session.byId( Flight.class ).load( 1 );

					// unlike the other 2 tests we should get 2 db queries here
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 2L );

					assertThat( Hibernate.isInitialized( flight1 ) ).isTrue();

					assertThat( Hibernate.isPropertyInitialized( flight1, "origination" ) ).isTrue();
					// this should be a non-enhanced proxy
					assertThat( Hibernate.isInitialized( flight1.getOrigination() ) ).isFalse();

					assertThat( Hibernate.isPropertyInitialized( flight1, "destination" ) ).isFalse();
					// the NO_PROXY here should trigger an EAGER load
					assertThat( Hibernate.isInitialized( flight1.getDestination() ) ).isFalse();
				}
		);
	}
}
