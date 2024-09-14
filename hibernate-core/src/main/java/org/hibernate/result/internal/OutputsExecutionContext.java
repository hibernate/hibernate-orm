/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;

public class OutputsExecutionContext extends BaseExecutionContext {
	private final Callback callback = new CallbackImpl();

	public OutputsExecutionContext(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public QueryOptions getQueryOptions() {
		return QueryOptions.READ_WRITE;
	}

	@Override
	public Callback getCallback() {
		return callback;
	}

}
