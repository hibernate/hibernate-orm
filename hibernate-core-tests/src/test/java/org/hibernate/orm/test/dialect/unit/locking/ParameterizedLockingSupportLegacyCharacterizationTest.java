package org.hibernate.orm.test.dialect.unit.locking;

import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Baseline characterization only: the modern overload's legacy read-wait value
/// is accidental observable behavior, pending an explicit compatibility decision.
@SuppressWarnings("removal")
class ParameterizedLockingSupportLegacyCharacterizationTest {
	@Test
	void deprecatedFalseRetainsReadWaitMetadata() {
		final var support = legacy( false );
		assertFalse( support.getMetadata().readsWaitForUncommittedWrites() );
		assertTrue( support.getMetadata().supportsInnerJoins() );
		ParameterizedLockingSupportContractTest.assertMetadata( support );
	}

	@Test
	void deprecatedTrueRetainsReadWaitMetadata() {
		final var support = legacy( true );
		assertTrue( support.getMetadata().readsWaitForUncommittedWrites() );
		assertTrue( support.getMetadata().supportsInnerJoins() );
		ParameterizedLockingSupportContractTest.assertMetadata( support );
	}

	@Test
	void modernFalseCurrentlyAlsoDisablesLegacyReadWait() {
		assertFalse( ParameterizedLockingSupportContractTest.explicit( false ).getMetadata().readsWaitForUncommittedWrites() );
	}

	@Test
	void modernTrueCurrentlyEnablesLegacyReadWait() {
		assertTrue( ParameterizedLockingSupportContractTest.explicit( true ).getMetadata().readsWaitForUncommittedWrites() );
	}

	@Test
	void modernOmittedCurrentlyEnablesLegacyReadWait() {
		assertTrue( ParameterizedLockingSupportContractTest.omitted().getMetadata().readsWaitForUncommittedWrites() );
	}

	private static LockingSupport legacy(boolean readsWaitForUncommittedWrites) {
		return StandardLockingSupports.parameterized(
				PessimisticLockStyle.CLAUSE, RowLockStrategy.COLUMN,
				LockTimeoutType.CONNECTION, LockTimeoutType.QUERY, LockTimeoutType.NONE,
				OuterJoinLockingType.IDENTIFIED, readsWaitForUncommittedWrites
		);
	}
}
