/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.internal;

import org.hibernate.result.UpdateCountOutput;

/**
 * Implementation of UpdateCountOutput
 *
 * @author Steve Ebersole
 */
class UpdateCountOutputImpl implements UpdateCountOutput {
	private final int updateCount;

	public UpdateCountOutputImpl(int updateCount) {
		this.updateCount = updateCount;
	}

	@Override
	public int getUpdateCount() {
		return updateCount;
	}

	@Override
	public boolean isResultSet() {
		return false;
	}
}
