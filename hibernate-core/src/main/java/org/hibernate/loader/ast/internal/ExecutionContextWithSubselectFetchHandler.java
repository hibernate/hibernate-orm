/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;

class ExecutionContextWithSubselectFetchHandler extends BaseExecutionContext {

	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;
	private final boolean readOnly;

	public ExecutionContextWithSubselectFetchHandler(
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
		this( session, subSelectFetchableKeysHandler, false );
	}

	public ExecutionContextWithSubselectFetchHandler(
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler,
			boolean readOnly) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		this.readOnly = readOnly;
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		if ( subSelectFetchableKeysHandler != null ) {
			subSelectFetchableKeysHandler.addKey( holder );
		}
	}

	@Override
	public QueryOptions getQueryOptions() {
		return readOnly ? QueryOptions.READ_ONLY : super.getQueryOptions();
	}
}
