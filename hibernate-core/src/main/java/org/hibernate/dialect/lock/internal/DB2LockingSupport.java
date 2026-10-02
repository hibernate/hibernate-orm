package org.hibernate.dialect.lock.internal;


import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;

import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.RowLockStrategy;

import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;

/**
 * LockingSupport for DB2Dialect
 *
 * @author Steve Ebersole
 */
public class DB2LockingSupport extends LockingSupportParameterized implements LockingClauseRenderer {
	private static final String LUW_SHARE_CLAUSE = " for read only with rs use and keep share locks";
	private static final String LUW_UPDATE_CLAUSE = " for read only with rs use and keep update locks";
	private static final String I_UPDATE_CLAUSE = " for update with rs";
	/**
	 * Waits for the outcome of an uncommitted write instead of reading the
	 * currently committed row, regardless of the {@code CUR_COMMIT} setting,
	 * without retaining any lock.
	 */
	private static final String LUW_CURRENT_READ_CLAUSE = " with cs wait for outcome";

	/**
	 * Builds a locking-strategy for DB2 LUW.
	 */
	public static DB2LockingSupport forDB2(boolean supportsSkipLocked) {
		return new DB2LockingSupport(
				RowLockStrategy.NONE,
				supportsSkipLocked,
				LUW_SHARE_CLAUSE,
				LUW_UPDATE_CLAUSE,
				// with CUR_COMMIT enabled (the default for new databases), plain reads
				// return the currently committed row instead of waiting
				false,
				LUW_CURRENT_READ_CLAUSE
		);
	}

	/**
	 * Builds a locking-strategy for DB2 iOS.
	 */
	public static DB2LockingSupport forDB2i() {
		return new DB2LockingSupport(
				RowLockStrategy.NONE,
				true,
				I_UPDATE_CLAUSE,
				I_UPDATE_CLAUSE,
				true,
				null
		);
	}

	/**
	 * Builds a locking-strategy preserving the historical DB2 i legacy-Dialect
	 * clause form inherited from DB2 LUW.
	 */
	public static DB2LockingSupport forLegacyDB2i() {
		return new DB2LockingSupport(
				RowLockStrategy.NONE,
				true,
				LUW_SHARE_CLAUSE,
				LUW_UPDATE_CLAUSE,
				true,
				null
		);
	}

	/**
	 * Builds a locking-strategy for DB2 on zOS.
	 */
	public static DB2LockingSupport forDB2z() {
		return new DB2LockingSupport(
				// https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statement-update-clause
				RowLockStrategy.COLUMN,
				true,
				LUW_SHARE_CLAUSE,
				LUW_UPDATE_CLAUSE,
				// readers wait for uncommitted updates even with currently-committed access
				true,
				null
		);
	}

	private final boolean supportsSkipLocked;
	private final String shareClause;
	private final String updateClause;
	private final String currentReadClause;

	private DB2LockingSupport(
			RowLockStrategy rowLockStrategy,
			boolean supportsSkipLocked,
			String shareClause,
			String updateClause,
			boolean readsWaitForUncommittedWrites,
			String currentReadClause) {
		super(
				PessimisticLockStyle.CLAUSE,
				rowLockStrategy,
				false,
				false,
				supportsSkipLocked,
				OuterJoinLockingType.FULL,
				readsWaitForUncommittedWrites
		);
		this.supportsSkipLocked = supportsSkipLocked;
		this.shareClause = shareClause;
		this.updateClause = updateClause;
		this.currentReadClause = currentReadClause;
	}

	@Override
	public TransactionConcurrencyResolver getTransactionConcurrencyResolver() {
		return new DB2TransactionConcurrencyResolver( currentReadClause != null, !shareClause.equals( updateClause ) );
	}

	@Override
	public String renderCurrentReadClause(TransactionConcurrency concurrency) {
		return currentReadClause != null ? currentReadClause : super.renderCurrentReadClause( concurrency );
	}

	@Override
	public LockingClauseRenderer getLockingClauseRenderer() {
		return this;
	}

	@Override
	public String render(LockingClauseRequest request) {
		final String clause = request.lockKind() == PessimisticLockKind.SHARE ? shareClause : updateClause;
		return request.timeout().milliseconds() == SKIP_LOCKED_MILLI && supportsSkipLocked
				? clause + " skip locked data"
				: clause;
	}
}
