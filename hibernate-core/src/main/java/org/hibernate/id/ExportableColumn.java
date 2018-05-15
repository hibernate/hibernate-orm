/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class ExportableColumn extends Column {

	private BasicType type;

	/**
	 * @deprecated use instead {@link ExportableColumn#ExportableColumn(Dialect, MappedTable, String, BasicType)}
	 */
	@Deprecated
	public ExportableColumn(Database database, MappedTable table, String name, BasicType type) {
		this(
				database,
				table,
				name,
				type,
				database.getDialect().getTypeName( type.getSqlTypeDescriptor().getJdbcTypeCode() )
		);
	}

	public ExportableColumn(Dialect dialect, MappedTable table, String name, BasicType type) {
		this(
				table,
				name,
				type,
				dialect.getTypeName( type.getSqlTypeDescriptor().getJdbcTypeCode() )
		);
	}

	/**
	 * @deprecated use instead {@link ExportableColumn#ExportableColumn(MappedTable, String, BasicType, String)}
	 */
	@Deprecated
	public ExportableColumn(
			Database database,
			MappedTable table,
			String name,
			BasicType type,
			String dbTypeDeclaration) {
		this( table, name, type, dbTypeDeclaration );
	}

	public ExportableColumn(
			MappedTable table,
			String name,
			BasicType type,
			String dbTypeDeclaration) {
		super( name, false );
		if ( table != null ) {
			setTableName( table.getNameIdentifier() );
		}

		setSqlType( dbTypeDeclaration );
		this.type = type;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return type.getSqlTypeDescriptor();
	}

	@Override
	protected BasicJavaDescriptor getJavaTypeDescriptor() {
		return type.getJavaTypeDescriptor();
	}
}
