/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import org.hibernate.type.UUIDCharType;

/**
 * @author Steve Ebersole
 */
public class IdTableSessionUidColumn extends IdTableColumn {
	public IdTableSessionUidColumn(IdTable containingTable) {
		super(
				containingTable,
				IdTableHelper.SESSION_ID_COLUMN_NAME,
				UUIDCharType.INSTANCE,
				null
		);
	}
}
