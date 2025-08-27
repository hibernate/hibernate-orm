/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.internal.BaseExecutionContext;

/**
 * @author Steve Ebersole
 */
public class LockingExecutionContext extends BaseExecutionContext {
	public LockingExecutionContext(SharedSessionContractImplementor session) {
		super( session );
	}
}
