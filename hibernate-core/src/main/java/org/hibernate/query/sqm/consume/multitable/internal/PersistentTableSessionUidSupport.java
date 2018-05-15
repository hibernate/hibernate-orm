/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.naming.Identifier;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableColumn;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.SessionUidSupport;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

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
				new IdTableColumn(
						idTable,
						Identifier.toIdentifier( SESSION_ID_COLUMN_NAME ),
						VarcharSqlDescriptor.INSTANCE,
						StringJavaDescriptor.INSTANCE,
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
