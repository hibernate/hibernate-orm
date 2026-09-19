/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;

public class NoCallbackExecutionContext extends BaseExecutionContext {

	public NoCallbackExecutionContext(@Nonnull SharedSessionContractImplementor session) {
		super( session );
	}

	@Nullable
	@Override
	public Callback getCallback() {
		return null;
//		throw new UnsupportedOperationException( "Follow-on locking not supported yet" );
	}

}
