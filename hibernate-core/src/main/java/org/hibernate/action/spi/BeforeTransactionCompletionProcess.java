/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.spi;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Contract representing some process that needs to occur during before transaction completion.
 *
 * @author Steve Ebersole
 */
public interface BeforeTransactionCompletionProcess {
	/**
	 * Perform whatever processing is encapsulated here before completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 */
	void doBeforeTransactionCompletion(SessionImplementor session);
}
