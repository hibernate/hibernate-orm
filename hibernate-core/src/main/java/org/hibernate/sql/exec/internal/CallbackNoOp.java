/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.AfterLoadAction;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.exec.spi.Callback;

/**
 * Implementation of Callback that does nothing
 */
public class CallbackNoOp implements Callback {
	/**
	 * Singleton access
	 */
	public static final CallbackNoOp NO_OP_CALLBACK = new CallbackNoOp();

	@Override
	public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
		// don't do anything
	}

	@Override
	public void invokeAfterLoadActions(Object entity, EntityMappingType entityMappingType, SharedSessionContractImplementor session) {
		// don't do anything
	}

	@Override
	public boolean hasAfterLoadActions() {
		return false;
	}
}
