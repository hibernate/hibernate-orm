package org.hibernate.test.locking;

import org.hibernate.LockMode;

/**
 * @author Vlad Mihalcea
 */
public class PessimisticWriteSkipLockedTest
		extends AbstractSkipLockedTest {

	@Override
	protected LockMode lockMode() {
		return LockMode.PESSIMISTIC_WRITE;
	}
}
