/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;

class ExecutionContextWithSubselectFetchHandler extends BaseExecutionContext {

	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;

	public ExecutionContextWithSubselectFetchHandler(
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
	}

	@Override
	public void registerLoadingEntityEntry(EntityKey entityKey, LoadingEntityEntry entry) {
		if ( subSelectFetchableKeysHandler != null ) {
			subSelectFetchableKeysHandler.addKey( entityKey, entry );
		}
	}

}
