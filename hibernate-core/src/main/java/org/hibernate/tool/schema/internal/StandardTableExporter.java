/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.Exporter;

import static java.util.Collections.addAll;
import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;

/**
 * An {@link Exporter} for {@linkplain Table tables}.
 *
 * @author Steve Ebersole
 */
public class StandardTableExporter implements Exporter<Table> {

	protected final Dialect dialect;

	public StandardTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(
			Table table,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final QualifiedName tableName = getTableName(table);

		try {
			final String formattedTableName = context.format( tableName );

			final StringBuilder createTable =
					new StringBuilder( tableCreateString( table.hasPrimaryKey() ) )
							.append( ' ' )
							.append( formattedTableName )
							.append( " (" );

			boolean isFirst = true;
			for ( Column column : table.getColumns() ) {
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					createTable.append( ", " );
				}
				appendColumn( createTable, column, table, metadata, dialect, context );
			}
			if ( table.hasPrimaryKey() ) {
				createTable.append( ", " ).append( table.getPrimaryKey().sqlConstraintString( dialect ) );
			}

			createTable.append( dialect.getUniqueDelegate().getTableCreationUniqueConstraintsFragment( table, context ) );

			applyTableCheck( table, createTable );

			createTable.append( ')' );

			if ( table.getComment() != null ) {
				createTable.append( dialect.getTableComment( table.getComment() ) );
			}

			applyTableTypeString( createTable );

			final List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( createTable.toString() );
			applyComments( table, formattedTableName, sqlStrings );
			applyInitCommands( table, sqlStrings, context );
			return sqlStrings.toArray( EMPTY_STRINGS );
		}
		catch (Exception e) {
			throw new MappingException( "Error creating SQL create commands for table : " + tableName, e );
		}
	}

	static void appendColumn(
			StringBuilder statement,
			Column column,
			Table table,
			Metadata metadata,
			Dialect dialect,
			SqlStringGenerationContext context) {

		statement.append( column.getQuotedName( dialect ) );

		final String columnType = column.getSqlType( metadata.getDatabase().getTypeConfiguration(), dialect, metadata );
		if ( isIdentityColumn( column, table, metadata, dialect ) ) {
			// to support dialects that have their own identity data type
			if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
				statement.append( ' ' ).append( columnType );
			}
			final String identityColumnString = dialect.getIdentityColumnSupport()
					.getIdentityColumnString( column.getSqlTypeCode( metadata ) );
			statement.append( ' ' ).append( identityColumnString );
		}
		else {
			if ( column.hasSpecializedTypeDeclaration() ) {
				statement.append( ' ' ).append( column.getSpecializedTypeDeclaration() );
			}
			else if ( column.getGeneratedAs() == null || dialect.hasDataTypeBeforeGeneratedAs() ) {
				statement.append( ' ' ).append( columnType );
			}

			final String defaultValue = column.getDefaultValue();
			if ( defaultValue != null ) {
				statement.append( " default " ).append( defaultValue );
			}

			final String generatedAs = column.getGeneratedAs();
			if ( generatedAs != null) {
				statement.append( dialect.generatedAs( generatedAs ) );
			}

			if ( column.isNullable() ) {
				statement.append( dialect.getNullColumnString(columnType) );
			}
			else {
				statement.append( " not null" );
			}
		}

		if ( column.isUnique() && !table.isPrimaryKey(column) ) {
			final String keyName = Constraint.generateName( "UK_", table, column);
			final UniqueKey uk = table.getOrCreateUniqueKey( keyName );
			uk.addColumn(column);
			statement.append(
					dialect.getUniqueDelegate().getColumnDefinitionUniquenessFragment( column, context )
			);
		}

		if ( dialect.supportsColumnCheck() && column.hasCheckConstraint() ) {
			statement.append( column.checkConstraint() );
		}

		final String columnComment = column.getComment();
		if ( columnComment != null ) {
			statement.append( dialect.getColumnComment( columnComment ) );
		}
	}

	private static boolean isIdentityColumn(Column column, Table table, Metadata metadata, Dialect dialect) {
		// Try to find out the name of the primary key in case the dialect needs it to create an identity
		return isPrimaryKeyIdentity( table, metadata, dialect )
			&& column.getQuotedName( dialect ).equals( getPrimaryKeyColumnName( table, dialect ) );
	}

	private static String getPrimaryKeyColumnName(Table table, Dialect dialect) {
		return table.hasPrimaryKey()
				? table.getPrimaryKey().getColumns().get(0).getQuotedName( dialect )
				: null;
	}

	private static boolean isPrimaryKeyIdentity(Table table, Metadata metadata, Dialect dialect) {
		// TODO: this is the much better form moving forward as we move to metamodel
		//return hasPrimaryKey
		//				&& table.getPrimaryKey().getColumnSpan() == 1
		//				&& table.getPrimaryKey().getColumn( 0 ).isIdentity();
		MetadataImplementor metadataImplementor = (MetadataImplementor) metadata;
		return table.hasPrimaryKey()
			&& table.getIdentifierValue() != null
			&& table.getIdentifierValue().isIdentityColumn(
					metadataImplementor.getMetadataBuildingOptions().getIdentifierGeneratorFactory(),
					dialect
			);
	}

	/**
	 * @param table The table.
	 * @param tableName The qualified table name.
	 * @param sqlStrings The list of SQL strings to add comments to.
	 * @deprecated Use {@link #applyComments(Table, String, List)} instead.
	 */
	// For backwards compatibility with subclasses that happen to call this method...
	@Deprecated
	protected void applyComments(Table table, QualifiedTableName tableName, List<String> sqlStrings) {
		applyComments( table, tableName.toString(), sqlStrings );
	}

	/**
	 * @param table The table.
	 * @param formattedTableName The formatted table name.
	 * @param sqlStrings The list of SQL strings to add comments to.
	 */
	protected void applyComments(Table table, String formattedTableName, List<String> sqlStrings) {
		if ( dialect.supportsCommentOn() ) {
			if ( table.getComment() != null ) {
				sqlStrings.add( "comment on table "
						+ formattedTableName
						+ " is '" + table.getComment() + "'" );
			}
			for ( Column column : table.getColumns() ) {
				String columnComment = column.getComment();
				if ( columnComment != null ) {
					sqlStrings.add( "comment on column "
							+ formattedTableName + '.' + column.getQuotedName( dialect )
							+ " is '" + columnComment + "'" );
				}
			}
		}
	}

	protected void applyInitCommands(Table table, List<String> sqlStrings, SqlStringGenerationContext context) {
		for ( InitCommand initCommand : table.getInitCommands( context ) ) {
			addAll( sqlStrings, initCommand.getInitCommands() );
		}
	}

	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableTypeString() );
	}

	protected void applyTableCheck(Table table, StringBuilder buf) {
		if ( dialect.supportsTableCheck() ) {
			for (String constraint : table.getCheckConstraints() ) {
				buf.append( ", check (" ).append( constraint ).append( ')' );
			}
		}
	}

	protected String tableCreateString(boolean hasPrimaryKey) {
		return hasPrimaryKey ? dialect.getCreateTableString() : dialect.getCreateMultisetTableString();

	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
		StringBuilder buf = new StringBuilder( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}
		buf.append( context.format( getTableName( table ) ) )
				.append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}
		return new String[] { buf.toString() };
	}

	private static QualifiedName getTableName(Table table) {
		return new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		);
	}
}
