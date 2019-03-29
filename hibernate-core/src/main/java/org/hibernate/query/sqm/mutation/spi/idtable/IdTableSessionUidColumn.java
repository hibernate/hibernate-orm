/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import org.hibernate.naming.Identifier;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class IdTableSessionUidColumn extends IdTableColumn {
	public IdTableSessionUidColumn(
			IdTable containingTable,
			String defaultValue,
			String sqlTypeDefinition,
			TypeConfiguration typeConfiguration) {
		super(
				containingTable,
				Identifier.toIdentifier( SessionUidSupport.SESSION_ID_COLUMN_NAME ),
				VarcharSqlDescriptor.INSTANCE,
				StringJavaDescriptor.INSTANCE,
				defaultValue,
				sqlTypeDefinition,
				typeConfiguration
		);
	}
}
