/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Hook for strategies which need to add a column in the underlying id table
 * to hold a "Session uid".
 *
 * @author Steve Ebersole
 */
public interface SessionUidSupport {
	String SESSION_ID_COLUMN_NAME = "hib_sess_id";

	/**
	 * Singleton access
	 */
	SessionUidSupport NONE = new SessionUidSupport() {

		@Override
		public  boolean needsSessionUidColumn() {
			return false;
		}

		@Override
		public void addColumn(IdTable idTable) {
		}

		@Override
		public String extractUid(SharedSessionContractImplementor session) {
			return null;
		}
	};

	boolean needsSessionUidColumn();

	/**
	 * Get the column used to store the session uid in the id table
	 */
	void addColumn(IdTable idTable);

	/**
	 * Given a session, extract the corresponding uid
	 */
	String extractUid(SharedSessionContractImplementor session);
}
