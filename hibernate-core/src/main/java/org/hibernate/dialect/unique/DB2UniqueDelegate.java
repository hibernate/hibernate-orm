/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import java.util.Collection;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;

/**
 * DB2 does not allow unique constraints on nullable columns.  Rather than
 * forcing "not null", use unique *indexes* instead.
 * 
 * @author Brett Meyer
 */
public class DB2UniqueDelegate extends DefaultUniqueDelegate {
	/**
	 * Constructs a DB2UniqueDelegate
	 *
	 * @param dialect The dialect
	 */
	public DB2UniqueDelegate( Dialect dialect ) {
		super( dialect );
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		if ( hasNullable( uniqueKey ) ) {
			return buildSqlCreateIndexString(
					dialect,
					uniqueKey.getName().getText(),
					jdbcEnvironment.getQualifiedObjectNameFormatter().format(
							( ( ExportableTable) uniqueKey.getTable() ).getQualifiedTableName(),
							jdbcEnvironment.getDialect()
					),
					uniqueKey.getColumns(),
					uniqueKey.getColumnOrderMap(),
					true
			);
		}
		else {
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, jdbcServices );
		}
	}
	
	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		if ( hasNullable( uniqueKey ) ) {
			return buildSqlDropIndexString(
					uniqueKey.getName().getText(),
					jdbcEnvironment.getQualifiedObjectNameFormatter().format(
							( ( ExportableTable) uniqueKey.getTable() ).getQualifiedTableName(),
							jdbcEnvironment.getDialect()
					)
			);
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, jdbcServices );
		}
	}

	private boolean hasNullable(UniqueKey uniqueKey) {
		for ( PhysicalColumn column : uniqueKey.getColumns() ) {
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	private String buildSqlDropIndexString(
			String name,
			String tableName) {
		return "drop index " + StringHelper.qualify( tableName, name );
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			String tableName,
			Collection<PhysicalColumn> columns,
			java.util.Map<PhysicalColumn, String> columnOrderMap,
			boolean unique) {
		StringBuilder buf = new StringBuilder( "create" )
				.append( unique ? " unique" : "" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ? name : StringHelper.unqualify( name ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );
		boolean isFirst = true;
		for ( PhysicalColumn column : columns ) {
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( column.getName().render( dialect ) );
			if ( columnOrderMap.containsKey( column ) ) {
				buf.append( " " ).append( columnOrderMap.get( column ) );
			}
		}
		buf.append( ")" );
		return buf.toString();
	}
}
