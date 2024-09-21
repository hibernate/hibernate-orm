/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

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
