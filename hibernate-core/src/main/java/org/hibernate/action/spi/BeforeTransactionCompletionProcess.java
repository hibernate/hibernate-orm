/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.spi;

import org.hibernate.engine.spi.TransactionCompletionCallbacks;

/**
 * Contract representing some process that needs to occur during before transaction completion.
 *
 * @author Steve Ebersole
 */
public interface BeforeTransactionCompletionProcess
		extends TransactionCompletionCallbacks.BeforeCompletionCallback {
}
