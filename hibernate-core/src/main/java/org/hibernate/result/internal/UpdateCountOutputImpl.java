/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
