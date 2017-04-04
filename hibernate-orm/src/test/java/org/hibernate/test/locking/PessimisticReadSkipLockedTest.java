package org.hibernate.test.locking;

import org.hibernate.LockMode;

/**
 * @author Vlad Mihalcea
 */
public class PessimisticReadSkipLockedTest
		extends AbstractSkipLockedTest {

	@Override
	protected LockMode lockMode() {
		return LockMode.PESSIMISTIC_READ;
	}

}
