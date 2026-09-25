package org.hibernate.testing.orm;

import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.testing.orm.junit.PermitsWriteAfterReadStatement;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.lock.spi.Operation.READ;
import static org.hibernate.dialect.lock.spi.Operation.WRITE;
import static org.hibernate.testing.orm.ConcurrencyCheckResult.*;
import static org.hibernate.testing.orm.TransactionConcurrencyChecks.*;

/// Verifies the prerequisites independently of any dialect's resolver.
///
/// @since 8.0
/// @author Steve Ebersole
class TransactionConcurrencyChecksTest {
	@ParameterizedTest
	@EnumSource(BlockingDuration.class)
	void interpretsProtectionLifetime(BlockingDuration duration) {
		final var concurrency = TransactionConcurrencies.builder( "test" )
				.blocking( READ, WRITE, duration ).blocking( WRITE, READ, duration ).build();
		assertThat( permitsReadWhileWriteUncommitted( concurrency ) ).isEqualTo( switch ( duration ) {
			case NONE -> MATCH;
			case STATEMENT, TRANSACTION -> NON_MATCH;
			case UNKNOWN, CONDITIONAL -> UNDETERMINED;
		} );
		assertThat( permitsWriteAfterReadStatement( concurrency ) ).isEqualTo( switch ( duration ) {
			case NONE, STATEMENT -> MATCH;
			case TRANSACTION -> NON_MATCH;
			case UNKNOWN, CONDITIONAL -> UNDETERMINED;
		} );
	}

	@Test
	void preservesDirectionAndUncertainty() {
		final var concurrency = TransactionConcurrencies.builder( "asymmetric" )
				.blocking( READ, WRITE, BlockingDuration.STATEMENT )
				.blocking( WRITE, READ, BlockingDuration.TRANSACTION ).build();
		assertThat( permitsWriteAfterReadStatement( concurrency ) ).isEqualTo( MATCH );
		assertThat( permitsReadWhileWriteUncommitted( concurrency ) ).isEqualTo( NON_MATCH );
		assertThat( MATCH.reversed() ).isEqualTo( NON_MATCH );
		assertThat( NON_MATCH.reversed() ).isEqualTo( MATCH );
		assertThat( UNDETERMINED.reversed() ).isEqualTo( UNDETERMINED );
		assertThatThrownBy( () -> new PermitsWriteAfterReadStatement().apply( new H2Dialect() ) )
				.isInstanceOf( IllegalStateException.class );
	}

	@ParameterizedTest
	@EnumSource(value = Operation.class, names = { "READ", "SHARED_LOCK_READ", "UPDATE_LOCK_READ" })
	void statelessRequiresAllGuarantees(Operation operation) {
		for ( int mask = 0; mask < 8; mask++ ) {
			final var concurrency = TransactionConcurrencies.builder( "guarantees" )
					.read( operation, new Guarantees( (mask & 1) != 0, true, (mask & 2) != 0, (mask & 4) != 0, true ) )
					.build();
			assertThat( supportsStatelessOptimisticLocking( concurrency ) ).isEqualTo( mask == 7 );
		}
	}

	@Test
	void unsupportedLocksAndFreshnessAloneDoNotEstablishStatelessSupport() {
		assertThat( supportsStatelessOptimisticLocking( TransactionConcurrencies.builder( "unknown" ).build() ) ).isFalse();
		assertThat( supportsStatelessOptimisticLocking( TransactionConcurrencies.builder( "current only" )
				.read( Operation.CURRENT_READ, new Guarantees( true, true, true, true, true ) ).build() ) ).isFalse();
	}
}
