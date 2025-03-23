/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

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
