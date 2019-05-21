/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class PersistentTableSessionUidSupport implements SessionUidSupport {
	@Override
	public boolean needsSessionUidColumn() {
		return true;
	}

	@Override
	public void addColumn(IdTable idTable) {
		idTable.addColumn(
				new IdTableSessionUidColumn(
						idTable,
						null,
						"CHAR(36)",
						null
				)
		);
	}

	@Override
	public String extractUid(SharedSessionContractImplementor session) {
		return session.getSessionIdentifier().toString();
	}
}
