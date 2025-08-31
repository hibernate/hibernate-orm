/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.spi;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;

/**
 * Contract representing some process that needs to occur during before transaction completion.
 *
 * @author Steve Ebersole
 */
public interface BeforeTransactionCompletionProcess extends TransactionCompletionCallbacks.BeforeCompletionCallback {
	/**
	 * Perform whatever processing is encapsulated here before completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 * @deprecated Use {@linkplain #doBeforeTransactionCompletion(SharedSessionContractImplementor)} instead.
	 */
	@Deprecated(since = "7.2", forRemoval = true)
	default void doBeforeTransactionCompletion(SessionImplementor session) {
		doBeforeTransactionCompletion( (SharedSessionContractImplementor) session );
	}
}
