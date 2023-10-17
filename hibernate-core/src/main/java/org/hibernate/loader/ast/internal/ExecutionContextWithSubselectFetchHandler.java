/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.sql.exec.internal.BaseExecutionContext;

class ExecutionContextWithSubselectFetchHandler extends BaseExecutionContext {

	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;

	public ExecutionContextWithSubselectFetchHandler(
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		if ( subSelectFetchableKeysHandler != null ) {
			subSelectFetchableKeysHandler.addKey( holder );
		}
	}

}
