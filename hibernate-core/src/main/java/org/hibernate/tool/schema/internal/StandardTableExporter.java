/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.naming.spi.QualifiedName;
import org.hibernate.naming.spi.QualifiedNameParser;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardTableExporter implements Exporter<ExportableTable> {
	protected final Dialect dialect;

	public StandardTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(ExportableTable table, JdbcServices jdbcServices) {
		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				table.getCatalogName(),
				table.getSchemaName(),
				table.getTableName()
		);

		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		StringBuilder buf =
				new StringBuilder( tableCreateString( table.hasPrimaryKey() ) )
						.append( ' ' )
						.append(
								jdbcEnvironment.getQualifiedObjectNameFormatter().format(
										tableName,
										jdbcEnvironment.getDialect()
								)
						)
						.append( " (" );


		// this is the much better form moving forward as we move to metamodel
		//boolean isPrimaryKeyIdentity = hasPrimaryKey
		//				&& table.getPrimaryKey().getColumnSpan() == 1
		//				&& table.getPrimaryKey().getColumn( 0 ).isIdentity();

		// Try to find out the name of the primary key in case the dialect needs it to create an identity
		String pkColName = null;
		if ( table.hasPrimaryKey() ) {
			PhysicalColumn pkColumn = table.getPrimaryKey().getColumns().iterator().next();
			pkColName = pkColumn.getName().render();
		}

		boolean isFirst = true;
		for ( PhysicalColumn col : table.getPhysicalColumns() ) {
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				buf.append( ", " );
			}
			String colName = col.getName().render( dialect );

			buf.append( colName ).append( ' ' );

			if ( table.isPrimaryKeyIdentity() && colName.equals( pkColName ) ) {
				// to support dialects that have their own identity data type
				if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
					buf.append( dialect.getTypeName( col.getSqlTypeDescriptor().getJdbcTypeCode() ) );
				}
				buf.append( ' ' ).append(
						dialect.getIdentityColumnSupport()
								.getIdentityColumnString( col.getSqlTypeDescriptor().getJdbcTypeCode() )
				);
			}
			else {
				buf.append( col.getSqlTypeName() );

				String defaultValue = col.getDefaultValue();
				if ( defaultValue != null ) {
					buf.append( " default " ).append( defaultValue );
				}

				if ( col.isNullable() ) {
					buf.append( dialect.getNullColumnString() );
				}
				else {
					buf.append( " not null" );
				}

			}

			if ( col.isUnique() ) {
				buf.append(
						dialect.getUniqueDelegate()
								.getColumnDefinitionUniquenessFragment( col )
				);
			}

			if ( col.getCheckConstraint() != null && dialect.supportsColumnCheck() ) {
				buf.append( " check (" )
						.append( col.getCheckConstraint() )
						.append( ")" );
			}

			String columnComment = col.getComment();
			if ( columnComment != null ) {
				buf.append( dialect.getColumnComment( columnComment ) );
			}
		}
		if ( table.hasPrimaryKey() ) {
			appendPrimaryKey( table, buf );
		}

		buf.append( dialect.getUniqueDelegate().getTableCreationUniqueConstraintsFragment( table ) );

		applyTableCheck( table, buf );

		buf.append( ')' );

		if ( table.getComment() != null ) {
			buf.append( dialect.getTableComment( table.getComment() ) );
		}

		applyTableTypeString( buf );

		List<String> sqlStrings = new ArrayList<String>();
		sqlStrings.add( buf.toString() );

		applyComments( table, tableName, sqlStrings );

		applyInitCommands( table, sqlStrings );

		return sqlStrings.toArray( new String[ sqlStrings.size() ] );
	}

	@Override
	public String[] getSqlDropStrings(ExportableTable table, JdbcServices jdbcServices) {
		StringBuilder buf = new StringBuilder( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}

		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				table.getCatalogName(),
				table.getSchemaName(),
				table.getTableName()
		);
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		buf.append( jdbcEnvironment.getQualifiedObjectNameFormatter().format( tableName, jdbcEnvironment.getDialect() ) )
				.append( dialect.getCascadeConstraintsString() );

		if ( dialect.supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}

		return new String[] { buf.toString() };
	}

	private void appendPrimaryKey(ExportableTable table, StringBuilder buf) {
		buf.append( ", primary key (" );
		boolean firstColumn = true;
		for ( PhysicalColumn column : table.getPrimaryKey().getColumns() ) {
			if ( firstColumn == true ) {
				firstColumn = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( column.getName().render( dialect ) );

		}
		buf.append( ')' );
	}

	protected void applyComments(ExportableTable table, QualifiedName tableName, List<String> sqlStrings) {
		if ( dialect.supportsCommentOn() ) {
			if ( table.getComment() != null ) {
				sqlStrings.add( "comment on table " + tableName + " is '" + table.getComment() + "'" );
			}
			for(PhysicalColumn column : table.getPhysicalColumns()){
				if( PhysicalColumn.class.isInstance( column )) {
					String columnComment = column.getComment();
					if ( columnComment != null ) {
						sqlStrings.add( "comment on column " + tableName + '.'
												+ column.getName().render( dialect ) + " is '" + columnComment + "'" );
					}
				}
			}
		}
	}

	protected void applyInitCommands(ExportableTable table, List<String> sqlStrings) {
		for ( InitCommand initCommand : table.getInitCommands() ) {
			Collections.addAll( sqlStrings, initCommand.getInitCommands() );
		}
	}

	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableTypeString() );
	}

	protected void applyTableCheck(ExportableTable table, StringBuilder buf) {
		if ( dialect.supportsTableCheck() ) {
			final Iterator<String> checkConstraints = table.getCheckConstraints().iterator();
			while ( checkConstraints.hasNext() ) {
				buf.append( ", check (" )
						.append( checkConstraints.next() )
						.append( ')' );
			}
		}
	}

	protected String tableCreateString(boolean hasPrimaryKey) {
		return hasPrimaryKey ? dialect.getCreateTableString() : dialect.getCreateMultisetTableString();
	}
}
