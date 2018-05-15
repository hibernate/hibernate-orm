/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.naming.Identifier;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A column in a IdTable.  As the column exists in the
 * IdTable, we know a few things about it inherently, such as:
 *
 * the column is part of the PK for the IdTable
 * therefore, the column is non-nullable
 *
 * @author Steve Ebersole
 */
public class IdTableColumn extends PhysicalColumn {
	public IdTableColumn(
			IdTable containingTable,
			Identifier columnName,
			SqlTypeDescriptor sqlTypeDescriptor,
			BasicJavaDescriptor javaTypeDescriptor,
			String defaultValue,
			String sqlTypeDefinition,
			TypeConfiguration typeConfiguration) {
		super(
				containingTable,
				columnName,
				() -> sqlTypeDescriptor,
				() -> javaTypeDescriptor,
				defaultValue,
				sqlTypeDefinition,
				false,
				false,
				typeConfiguration
		);
	}

	public IdTableColumn(IdTable idTable,PhysicalColumn column) {
		super(
				idTable,
				column.getName(),
				column.getSqlTypeDescriptorAccess(),
				column.getJavaTypeDescriptorAccess(),
				column.getDefaultValue(),
				column.getSqlTypeName(),
				false,
				false,
				column.getTypeConfiguration()
		);
	}

	@Override
	public IdTable getSourceTable() {
		return (IdTable) super.getSourceTable();
	}
}
