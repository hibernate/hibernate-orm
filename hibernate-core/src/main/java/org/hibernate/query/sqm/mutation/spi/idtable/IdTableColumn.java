/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.QualifiedObjectNameFormatterStandardImpl;
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

	public IdTableColumn(IdTable idTable,PhysicalColumn entityIdPkColumn) {
		super(
				idTable,
				entityIdPkColumn.getName(),
				entityIdPkColumn.getSqlTypeDescriptorAccess(),
				entityIdPkColumn.getJavaTypeDescriptorAccess(),
				entityIdPkColumn.getDefaultValue(),
				entityIdPkColumn.getSqlTypeName(),
				false,
				false,
				entityIdPkColumn.getTypeConfiguration()
		);
	}

	@Override
	public IdTable getSourceTable() {
		return (IdTable) super.getSourceTable();
	}

	@Override
	public String render(String qualifier) {
		final Dialect dialect = getSourceTable().getEntityDescriptor()
				.getTypeConfiguration()
				.getSessionFactory()
				.getJdbcServices()
				.getDialect();

		final String base = getName().render( dialect );
		if ( qualifier == null ) {
			return base;
		}
		else {
			return qualifier + '.' + base;
		}
	}

	@Override
	public String render() {
		return getName().render();
	}
}
