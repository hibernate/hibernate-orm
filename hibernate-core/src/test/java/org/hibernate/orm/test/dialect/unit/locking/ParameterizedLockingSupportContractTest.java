package org.hibernate.orm.test.dialect.unit.locking;

import jakarta.persistence.Timeout;

import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Database-free provider factory contract regression for HHH-20980.
class ParameterizedLockingSupportContractTest {
	private static final TransactionConcurrencyResolver RESOLVER = (dialect, metadata, connection, declaration, access) -> {
		throw new AssertionError( "The factory must retain the resolver without invoking it" );
	};

	@Test
	void explicitFalseDisablesInnerJoinLocking() {
		assertFalse( explicit( false ).getMetadata().supportsInnerJoins() );
	}

	@Test
	void explicitTrueEnablesInnerJoinLocking() {
		assertTrue( explicit( true ).getMetadata().supportsInnerJoins() );
	}

	@Test
	void omittedFlagDefaultsToInnerJoinLocking() {
		assertTrue( omitted().getMetadata().supportsInnerJoins() );
	}

	@Test
	void explicitFalsePreservesResolverAndOtherMetadata() {
		assertPreserved( explicit( false ) );
	}

	@Test
	void explicitTruePreservesResolverAndOtherMetadata() {
		assertPreserved( explicit( true ) );
	}

	@Test
	void omittedFlagPreservesResolverAndOtherMetadata() {
		assertPreserved( omitted() );
	}

	static LockingSupport explicit(boolean supportsInnerJoins) {
		return StandardLockingSupports.parameterized(
				PessimisticLockStyle.CLAUSE, RowLockStrategy.COLUMN,
				LockTimeoutType.CONNECTION, LockTimeoutType.QUERY, LockTimeoutType.NONE,
				OuterJoinLockingType.IDENTIFIED, supportsInnerJoins, RESOLVER
		);
	}

	static LockingSupport omitted() {
		return StandardLockingSupports.parameterized(
				PessimisticLockStyle.CLAUSE, RowLockStrategy.COLUMN,
				LockTimeoutType.CONNECTION, LockTimeoutType.QUERY, LockTimeoutType.NONE,
				OuterJoinLockingType.IDENTIFIED, RESOLVER
		);
	}

	private static void assertPreserved(LockingSupport support) {
		assertSame( RESOLVER, support.getTransactionConcurrencyResolver() );
		assertMetadata( support );
	}

	static void assertMetadata(LockingSupport support) {
		final var metadata = support.getMetadata();
		assertAll(
				() -> assertEquals( PessimisticLockStyle.CLAUSE, metadata.getPessimisticLockStyle() ),
				() -> assertEquals( RowLockStrategy.COLUMN, metadata.getReadRowLockStrategy() ),
				() -> assertEquals( RowLockStrategy.COLUMN, metadata.getWriteRowLockStrategy() ),
				() -> assertEquals( OuterJoinLockingType.IDENTIFIED, metadata.getOuterJoinLockingType() ),
				() -> assertEquals( LockTimeoutType.CONNECTION, metadata.getLockTimeoutType( Timeout.milliseconds( 1250 ) ) ),
				() -> assertEquals( LockTimeoutType.QUERY, metadata.getLockTimeoutType( Timeouts.NO_WAIT ) ),
				() -> assertEquals( LockTimeoutType.NONE, metadata.getLockTimeoutType( Timeouts.SKIP_LOCKED ) ),
				() -> assertEquals( LockTimeoutType.QUERY, metadata.getLockTimeoutType( Timeouts.WAIT_FOREVER ) ),
				() -> assertSame( ConnectionLockTimeoutStrategy.NONE, support.getConnectionLockTimeoutStrategy() )
		);
	}
}
