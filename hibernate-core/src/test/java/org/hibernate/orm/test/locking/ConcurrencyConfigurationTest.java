package org.hibernate.orm.test.locking;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.junit.jupiter.api.Assertions.*;

/// A dialect's Java superclass must not substitute another database's contracts.
///
/// @author Steve Ebersole
class ConcurrencyConfigurationTest {
	static Stream<Dialect> spannerDialects() {
		return Stream.of( new SpannerDialect(), new SpannerPostgreSQLDialect() );
	}

	@ParameterizedTest
	@MethodSource("spannerDialects")
	void spannerUsesItsOwnBaselineAndFacts(Dialect dialect) {
		final var configurations = ConcurrencyConfiguration.configurations( dialect ).toList();
		assertEquals( 1, configurations.size() );
		final var configuration = configurations.get( 0 );
		assertEquals( TRANSACTION_SERIALIZABLE, configuration.isolation() );
		assertFalse( configuration.knownReadProfile( dialect ) );
		assertFalse( configuration.sharedRead( dialect ) );
		assertFalse( configuration.currentReadConflicts( dialect ) );
		assertNull( configuration.ordinaryCurrentRead( dialect ), "Read-lock mode must be observed, not inherited from PostgreSQL" );
	}
}
