/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.Collections;
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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.Exporter;

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
		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		);

		try {
			String formattedTableName = context.format( tableName );
			StringBuilder createTable =
					new StringBuilder( tableCreateString( table.hasPrimaryKey() ) )
							.append( ' ' )
							.append( formattedTableName )
							.append( " (" );


			boolean isPrimaryKeyIdentity = table.hasPrimaryKey()
					&& table.getIdentifierValue() != null
					&& table.getIdentifierValue().isIdentityColumn(
							( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
									.getIdentifierGeneratorFactory(),
							dialect
					);
			// TODO: this is the much better form moving forward as we move to metamodel
			//boolean isPrimaryKeyIdentity = hasPrimaryKey
			//				&& table.getPrimaryKey().getColumnSpan() == 1
			//				&& table.getPrimaryKey().getColumn( 0 ).isIdentity();

			// Try to find out the name of the primary key in case the dialect needs it to create an identity
			String pkColName = null;
			if ( table.hasPrimaryKey() ) {
				pkColName = table.getPrimaryKey().getColumns().get(0).getQuotedName( dialect );
			}

			boolean isFirst = true;
			for ( Column column : table.getColumns() ) {
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					createTable.append( ", " );
				}

				String colName = column.getQuotedName( dialect );
				createTable.append( colName );

				if ( isPrimaryKeyIdentity && colName.equals( pkColName ) ) {
					// to support dialects that have their own identity data type
					if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
						createTable.append( ' ' ).append(
								column.getSqlType( metadata.getDatabase().getTypeConfiguration(), dialect, metadata )
						);
					}
					String identityColumnString = dialect.getIdentityColumnSupport()
							.getIdentityColumnString( column.getSqlTypeCode(metadata) );
					createTable.append( ' ' ).append( identityColumnString );
				}
				else {
					final String columnType = column.getSqlType(
							metadata.getDatabase().getTypeConfiguration(),
							dialect,
							metadata
					);
					if ( column.hasSpecializedTypeDeclaration() ) {
						createTable.append( ' ' ).append( column.getSpecializedTypeDeclaration() );
					}
					else if ( column.getGeneratedAs() == null || dialect.hasDataTypeBeforeGeneratedAs() ) {
						createTable.append( ' ' ).append( columnType );
					}

					String defaultValue = column.getDefaultValue();
					if ( defaultValue != null ) {
						createTable.append( " default " ).append( defaultValue );
					}

					String generatedAs = column.getGeneratedAs();
					if ( generatedAs != null) {
						createTable.append( dialect.generatedAs( generatedAs ) );
					}

					if ( column.isNullable() ) {
						createTable.append( dialect.getNullColumnString( columnType ) );
					}
					else {
						createTable.append( " not null" );
					}

				}

				if ( column.isUnique() && !table.isPrimaryKey( column ) ) {
					String keyName = Constraint.generateName( "UK_", table, column );
					UniqueKey uk = table.getOrCreateUniqueKey( keyName );
					uk.addColumn( column );
					createTable.append(
							dialect.getUniqueDelegate()
									.getColumnDefinitionUniquenessFragment( column, context )
					);
				}

				if ( dialect.supportsColumnCheck() && column.hasCheckConstraint() ) {
					createTable.append( column.checkConstraint() );
				}

				String columnComment = column.getComment();
				if ( columnComment != null ) {
					createTable.append( dialect.getColumnComment( columnComment ) );
				}
			}
			if ( table.hasPrimaryKey() ) {
				createTable.append( ", " )
						.append( table.getPrimaryKey().sqlConstraintString( dialect ) );
			}

			createTable.append( dialect.getUniqueDelegate().getTableCreationUniqueConstraintsFragment( table, context ) );

			applyTableCheck( table, createTable );

			createTable.append( ')' );

			if ( table.getComment() != null ) {
				createTable.append( dialect.getTableComment( table.getComment() ) );
			}

			applyTableTypeString( createTable );

			List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( createTable.toString() );

			applyComments( table, formattedTableName, sqlStrings );

			applyInitCommands( table, sqlStrings, context );

			return sqlStrings.toArray(StringHelper.EMPTY_STRINGS);
		}
		catch (Exception e) {
			throw new MappingException( "Error creating SQL create commands for table : " + tableName, e );
		}
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
				sqlStrings.add( "comment on table " + formattedTableName + " is '" + table.getComment() + "'" );
			}
			for ( Column column : table.getColumns() ) {
				String columnComment = column.getComment();
				if ( columnComment != null ) {
					sqlStrings.add( "comment on column " + formattedTableName + '.' + column.getQuotedName( dialect ) + " is '" + columnComment + "'" );
				}
			}
		}
	}

	protected void applyInitCommands(Table table, List<String> sqlStrings, SqlStringGenerationContext context) {
		for ( InitCommand initCommand : table.getInitCommands( context ) ) {
			Collections.addAll( sqlStrings, initCommand.getInitCommands() );
		}
	}

	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableTypeString() );
	}

	protected void applyTableCheck(Table table, StringBuilder buf) {
		if ( dialect.supportsTableCheck() ) {
			for (String constraint : table.getCheckConstraints() ) {
				buf.append( ", check (" )
						.append( constraint )
						.append( ')' );
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

		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		);
		buf.append( context.format( tableName ) )
				.append( dialect.getCascadeConstraintsString() );

		if ( dialect.supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}

		return new String[] { buf.toString() };
	}
}
