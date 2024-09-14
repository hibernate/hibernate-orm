/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.LockOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;

/**
 * @author Christian Beikov
 */
public class SimpleQueryOptions extends QueryOptionsAdapter {

	private final LockOptions lockOptions;
	private final Boolean readOnlyEnabled;

	public SimpleQueryOptions(LockOptions lockOptions, Boolean readOnlyEnabled) {
		this.lockOptions = lockOptions;
		this.readOnlyEnabled = readOnlyEnabled;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Boolean isReadOnly() {
		return readOnlyEnabled;
	}
}
