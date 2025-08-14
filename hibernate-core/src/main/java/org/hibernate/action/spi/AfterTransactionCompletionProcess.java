/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.spi;

import org.hibernate.engine.spi.TransactionCompletionCallbacks;

/**
 * Contract representing some process that needs to occur during after transaction completion.
 *
 * @author Steve Ebersole
 */
public interface AfterTransactionCompletionProcess extends TransactionCompletionCallbacks.AfterCompletionCallback {
}
