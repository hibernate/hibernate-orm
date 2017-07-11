/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.mapping.Column;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class ExportableColumn extends Column {

	public ExportableColumn(Database database, MappedTable table, String name, BasicType type) {
		this(
				database,
				table,
				name,
				type,
				database.getDialect().getTypeName( type.getColumnDescriptor().getSqlTypeDescriptor().getJdbcTypeCode() )
		);
	}

	public ExportableColumn(
			Database database,
			MappedTable table,
			String name,
			BasicType type,
			String dbTypeDeclaration) {
		super( name );
		if(table!= null){
			setTableName( table.getNameIdentifier() );
		}

		setSqlTypeDescriptor( type.getColumnDescriptor().getSqlTypeDescriptor() );
		setSqlType( dbTypeDeclaration );
	}


}
