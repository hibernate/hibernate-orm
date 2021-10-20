/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.AfterLoadAction;
import org.hibernate.persister.entity.Loadable;
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
	public void invokeAfterLoadActions(SharedSessionContractImplementor session, Object entity, Loadable persister) {
		// don't do anything
	}
}
