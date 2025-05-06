/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.LockModeType;

/**
 * @author Steve Ebersole
 */
public class NonSelectQueryLockMode extends AbstractJPATest {

	@Test
	public void testNonSelectQueryGetLockMode() {
		Assertions.assertThrows(
				IllegalStateException.class,
				() -> inTransaction(
						session -> session.createQuery( "delete Item" ).getLockMode()
				)
		);
	}

	@Test
	public void testNonSelectQuerySetLockMode() {
		Assertions.assertThrows(
				IllegalStateException.class,
				() -> inTransaction(
						session -> session.createQuery( "delete Item" ).setLockMode( LockModeType.PESSIMISTIC_WRITE )
				)
		);
	}
}
