/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;

public class NoCallbackExecutionContext extends BaseExecutionContext {

	public NoCallbackExecutionContext(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public Callback getCallback() {
		return null;
//		throw new UnsupportedOperationException( "Follow-on locking not supported yet" );
	}

}
