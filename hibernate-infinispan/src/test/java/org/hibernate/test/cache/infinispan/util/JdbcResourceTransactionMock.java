package org.hibernate.test.cache.infinispan.util;

import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.junit.Assert.assertEquals;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class JdbcResourceTransactionMock implements JdbcResourceTransaction {
	private TransactionStatus status = TransactionStatus.NOT_ACTIVE;

	@Override
	public void begin() {
		assertEquals(TransactionStatus.NOT_ACTIVE, status);
		status = TransactionStatus.ACTIVE;
	}

	@Override
	public void commit() {
		assertEquals(TransactionStatus.ACTIVE, status);
		status = TransactionStatus.COMMITTED;
	}

	@Override
	public void rollback() {
		status = TransactionStatus.ROLLED_BACK;
	}

	@Override
	public TransactionStatus getStatus() {
		return status;
	}
}
