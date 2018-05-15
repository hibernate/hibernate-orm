/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;

/**
 * The default UniqueDelegate implementation for most dialects.  Uses
 * separate create/alter statements to apply uniqueness to a column.
 * 
 * @author Brett Meyer
 */
public class DefaultUniqueDelegate implements UniqueDelegate {
	protected final Dialect dialect;

	/**
	 * Constructs DefaultUniqueDelegate
	 *
	 * @param dialect The dialect for which we are handling unique constraints
	 */
	public DefaultUniqueDelegate( Dialect dialect ) {
		this.dialect = dialect;
	}

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getColumnDefinitionUniquenessFragment(Column column) {
		return "";
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(ExportableTable table) {
		return "";
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {

		final String tableName = jdbcServices
				.getJdbcEnvironment()
				.getQualifiedObjectNameFormatter()
				.format( ( ( ExportableTable) uniqueKey.getTable() ).getQualifiedTableName(), dialect );

		final String constraintName = uniqueKey.getName().render( dialect );
		return dialect.getAlterTableString( tableName )
				+ " add constraint " + constraintName + " " + uniqueConstraintSql( uniqueKey );

	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {

		final String tableName = jdbcServices
				.getJdbcEnvironment()
				.getQualifiedObjectNameFormatter()
				.format( ( ( ExportableTable) uniqueKey.getTable() ).getQualifiedTableName(), dialect );

		final StringBuilder buf = new StringBuilder( dialect.getAlterTableString( tableName ));
		buf.append(getDropUnique() );
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			buf.append( "if exists " );
		}
		buf.append( uniqueKey.getName().render( dialect ) );
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			buf.append( " if exists" );
		}
		return buf.toString();
	}

	protected String getDropUnique(){
		return " drop constraint ";
	}

	protected String uniqueConstraintSql(UniqueKey uniqueKey) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "unique (" );
		boolean isFirst = true;
		for ( PhysicalColumn column : uniqueKey.getColumns() ) {
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				sb.append( ", " );
			}
			sb.append( column.getName().render( dialect ) );
			if ( uniqueKey.getColumnOrderMap().containsKey( column ) ) {
				sb.append( " " ).append( uniqueKey.getColumnOrderMap().get( column ) );
			}
		}

		return sb.append( ')' ).toString();
	}

}
