/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
