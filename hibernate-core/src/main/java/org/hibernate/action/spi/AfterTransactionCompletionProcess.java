/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract representing some process that needs to occur during after transaction completion.
 *
 * @author Steve Ebersole
 */
public interface AfterTransactionCompletionProcess {
	/**
	 * Perform whatever processing is encapsulated here after completion of the transaction.
	 *
	 * @param success Did the transaction complete successfully?  True means it did.
	 * @param session The session on which the transaction is completing.
	 */
	void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session);
}
