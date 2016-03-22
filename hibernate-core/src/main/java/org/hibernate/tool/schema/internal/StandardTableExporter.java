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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardTableExporter implements Exporter<Table> {
	protected final Dialect dialect;

	public StandardTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Table table, Metadata metadata) {
		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		);

		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
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


		boolean isPrimaryKeyIdentity = table.hasPrimaryKey()
				&& table.getIdentifierValue() != null
				&& table.getIdentifierValue().isIdentityColumn( metadata.getIdentifierGeneratorFactory(), dialect );
		// this is the much better form moving forward as we move to metamodel
		//boolean isPrimaryKeyIdentity = hasPrimaryKey
		//				&& table.getPrimaryKey().getColumnSpan() == 1
		//				&& table.getPrimaryKey().getColumn( 0 ).isIdentity();

		// Try to find out the name of the primary key in case the dialect needs it to create an identity
		String pkColName = null;
		if ( table.hasPrimaryKey() ) {
			Column pkColumn = (Column) table.getPrimaryKey().getColumns().iterator().next();
			pkColName = pkColumn.getQuotedName( dialect );
		}

		final Iterator columnItr = table.getColumnIterator();
		boolean isFirst = true;
		while ( columnItr.hasNext() ) {
			final Column col = (Column) columnItr.next();
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				buf.append( ", " );
			}
			String colName = col.getQuotedName( dialect );

			buf.append( colName ).append( ' ' );

			if ( isPrimaryKeyIdentity && colName.equals( pkColName ) ) {
				// to support dialects that have their own identity data type
				if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
					buf.append( col.getSqlType( dialect, metadata ) );
				}
				buf.append( ' ' )
						.append( dialect.getIdentityColumnSupport().getIdentityColumnString( col.getSqlTypeCode( metadata ) ) );
			}
			else {
				buf.append( col.getSqlType( dialect, metadata )  );

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
				String keyName = Constraint.generateName( "UK_", table, col );
				UniqueKey uk = table.getOrCreateUniqueKey( keyName );
				uk.addColumn( col );
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
			buf.append( ", " )
					.append( table.getPrimaryKey().sqlConstraintString( dialect ) );
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

	protected void applyComments(Table table, QualifiedName tableName, List<String> sqlStrings) {
		if ( dialect.supportsCommentOn() ) {
			if ( table.getComment() != null ) {
				sqlStrings.add( "comment on table " + tableName + " is '" + table.getComment() + "'" );
			}
			final Iterator iter = table.getColumnIterator();
			while ( iter.hasNext() ) {
				Column column = (Column) iter.next();
				String columnComment = column.getComment();
				if ( columnComment != null ) {
					sqlStrings.add( "comment on column " + tableName + '.' + column.getQuotedName( dialect ) + " is '" + columnComment + "'" );
				}
			}
		}
	}

	protected void applyInitCommands(Table table, List<String> sqlStrings) {
		for ( InitCommand initCommand : table.getInitCommands() ) {
			Collections.addAll( sqlStrings, initCommand.getInitCommands() );
		}
	}

	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableTypeString() );
	}

	protected void applyTableCheck(Table table, StringBuilder buf) {
		if ( dialect.supportsTableCheck() ) {
			final Iterator<String> checkConstraints = table.getCheckConstraintsIterator();
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

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata) {
		StringBuilder buf = new StringBuilder( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}

		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		);
		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
		buf.append( jdbcEnvironment.getQualifiedObjectNameFormatter().format( tableName, jdbcEnvironment.getDialect() ) )
				.append( dialect.getCascadeConstraintsString() );

		if ( dialect.supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}

		return new String[] { buf.toString() };
	}
}
