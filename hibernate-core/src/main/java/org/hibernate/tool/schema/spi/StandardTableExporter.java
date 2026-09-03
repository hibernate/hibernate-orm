/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.constraint.spi.CheckConstraintPlacement;
import org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest;
import org.hibernate.dialect.aggregate.internal.AggregateColumnDescriptorAdapter;
import org.hibernate.dialect.aggregate.spi.AggregateComponentReadRequest;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.sql.Template;
import org.hibernate.type.SqlTypes;

import static java.util.Collections.addAll;
import static java.util.Comparator.comparing;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.appendColumn;

/// Standard exporter for relational [Table] definitions.
///
/// Extend this class only when the database needs to suppress an aggregate
/// column check or reposition a primary-key constraint name. Override the
/// complete [#getSqlCreateStrings(Table, Metadata, SqlStringGenerationContext)]
/// or [#getSqlDropStrings(Table, Metadata, SqlStringGenerationContext)]
/// operation when the database uses a different table-DDL algorithm.
///
/// Retain the supplied Dialect for the lifetime of this exporter and supply the
/// exporter from [Dialect#getTableExporter()].
///
/// @see Dialect#getTableExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public class StandardTableExporter implements Exporter<Table> {

	private final Dialect dialect;

	/// Create a standard table exporter owned by `dialect`.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	public StandardTableExporter(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must not be null" );
		}
		this.dialect = dialect;
	}

	/// Return the Dialect which owns this exporter.
	protected final Dialect dialect() {
		return dialect;
	}

	@Override
	public String[] getSqlCreateStrings(
			Table table,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final var tableName = getTableName( table );
		try {
			final String formattedTableName = context.format( tableName );
			final String ddl = table.isView()
					? appendCreateView( table, formattedTableName )
					: appendCreateTable( table, formattedTableName, metadata, context );

			final List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( ddl );
			applyComments( table, formattedTableName, sqlStrings );
			applyInitCommands( table, sqlStrings, context );
			return sqlStrings.toArray( EMPTY_STRINGS );
		}
		catch (Exception e) {
			throw new MappingException( "Error creating SQL 'create' commands for table '"
					+ table.getName() + "' [" + e.getMessage() + "]" , e );
		}
	}

	private static void appendOptions(Table table, StringBuilder createTable) {
		final String options = table.getOptions();
		if ( isNotBlank( options ) ) {
			createTable.append( " " ).append( options );
		}
	}

	private String appendCreateTable(Table table, String tableName, Metadata metadata, SqlStringGenerationContext context) {
		final var createTable = new StringBuilder();
		final var extra = new StringBuilder();

		createTable.append( tableCreateString( table ) )
				.append( ' ' )
				.append( tableName )
				.append( " (" );

		boolean isFirst = true;
		for ( var column : table.getColumns() ) {
			if ( isFirst ) {
				isFirst = false;
			}
			else {
				createTable.append( ", " );
			}
			appendColumn( createTable, column, table, metadata, dialect, context );

			extra.append( column.getValue().getExtraCreateTableInfo() );
		}
		if ( table.getRowId() != null ) {
			final String rowIdColumn = dialect.getRowIdSupport().columnDefinition( table.getRowId() );
			if ( rowIdColumn != null ) {
				createTable.append(", ").append( rowIdColumn );
			}
		}
		if ( table.hasPrimaryKey() ) {
			createTable.append( ", " ).append( primaryKeyString( table.getPrimaryKey() ) );
		}

		createTable.append( dialect.getUniqueDelegate().getTableCreationUniqueConstraintsFragment( table, context ) );

		applyTableCheck( table, createTable );

		if ( isNotEmpty( table.getExtraDeclarations() ) ) {
			createTable.append( ", " ).append( table.getExtraDeclarations() );
		}

		createTable.append( ')' );

		createTable.append( extra );

		if ( table.getComment() != null ) {
			final var commentSupport = dialect.getSchemaCommentSupport();
			if ( commentSupport.placement( CommentTarget.TABLE ) == CommentPlacement.INLINE ) {
				createTable.append( commentSupport.render( new CommentRequest(
						CommentTarget.TABLE,
						tableName,
						table.getComment()
				) ) );
			}
		}

		applyTableTypeString( createTable );

		appendOptions( table, createTable );

		return createTable.toString();
	}

	private String appendCreateView(Table table, String viewName) {
		final var createTable = new StringBuilder();

		final String viewQuery = table.getViewQuery();

		createTable.append("create view ").append( viewName );
		if ( dialect.getTableCreationSupport().requiresViewColumnList() ) {
			createTable.append(" (");
			var sortedColumns =
					table.getColumns().stream()
							.sorted( comparing( c -> viewQuery.indexOf( c.getQuotedName( dialect ) ) ) )
							.toList();
			boolean isFirst = true;
			for ( var column : sortedColumns ) {
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					createTable.append( ", " );
				}
				createTable.append( column.getQuotedName( dialect ) );
			}
			createTable.append(")");
		}
		createTable.append(" as ").append( viewQuery );

		appendOptions( table, createTable );

		return createTable.toString();
	}

	private void applyComments(Table table, String formattedTableName, List<String> sqlStrings) {
		final var support = dialect.getSchemaCommentSupport();
		final String comment = table.getComment();
		if ( comment != null && support.placement( CommentTarget.TABLE ) == CommentPlacement.STATEMENT ) {
			sqlStrings.add( support.render( new CommentRequest( CommentTarget.TABLE, formattedTableName, comment ) ) );
		}
		if ( support.placement( CommentTarget.TABLE_COLUMN ) == CommentPlacement.STATEMENT ) {
			for ( var column : table.getColumns() ) {
				final String columnComment = column.getComment();
				if ( columnComment != null ) {
					sqlStrings.add( support.render( new CommentRequest(
							CommentTarget.TABLE_COLUMN,
							formattedTableName + '.' + column.getQuotedName( dialect ),
							columnComment
					) ) );
				}
			}
		}
	}

	private void applyInitCommands(Table table, List<String> sqlStrings, SqlStringGenerationContext context) {
		for ( var initCommand : table.getInitCommands( context ) ) {
			addAll( sqlStrings, initCommand.initCommands() );
		}
	}

	private void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableCreationSupport().tableCreationOptions() );
	}

	private void applyTableCheck(Table table, StringBuilder buf) {
		final var support = dialect.getCheckConstraintSupport();
		if ( support.supports( CheckConstraintPlacement.TABLE ) ) {
			for ( var column : table.getColumns() ) {
				final var checkConstraints = column.getCheckConstraints();
				boolean hasAnonymousConstraints = false;
				if ( !support.supports( CheckConstraintPlacement.ANONYMOUS_COLUMN ) ) {
					for ( var constraint : checkConstraints ) {
						if ( constraint.isAnonymous() ) {
							if ( !hasAnonymousConstraints ) {
								buf.append( ", check (" );
								hasAnonymousConstraints = true;
							}
							else {
								buf.append( " and " );
							}
							buf.append( constraint.getConstraintInParens() );
						}
					}
					if ( hasAnonymousConstraints ) {
						buf.append( ')' );
					}
				}
				else {
					hasAnonymousConstraints = checkConstraints.stream().anyMatch( CheckConstraint::isAnonymous );
				}

				// Since some databases don't like when multiple check clauses appear for a colum definition,
				// named constraints need to be hoisted to the table definition.
				// Skip the first named constraint if the column has no anonymous constraints and the dialect
				// supports named column check constraints, because ColumnDefinitions will render the first check
				// constraint already.
				boolean skipNextNamedConstraint = !hasAnonymousConstraints
						&& support.supports( CheckConstraintPlacement.NAMED_COLUMN );
				for ( var constraint : checkConstraints ) {
					if ( constraint.isNamed() ) {
						if ( skipNextNamedConstraint ) {
							skipNextNamedConstraint = false;
						}
						else {
							buf.append( ", " ).append( renderCheck( constraint, CheckConstraintPlacement.TABLE ) );
						}
					}
				}
			}
			for ( var constraint : table.getChecks() ) {
				buf.append( ", " ).append( renderCheck( constraint, CheckConstraintPlacement.TABLE ) );
			}
			final var aggregateSupport = dialect.getAggregateSupport();
			if ( aggregateSupport != null && aggregateSupport.supportsComponentCheckConstraints() ) {
				for ( var column : table.getColumns() ) {
					if ( column instanceof AggregateColumn aggregateColumn ) {
						if ( !isArray( aggregateColumn ) ) {
							applyAggregateColumnCheck( buf, aggregateColumn );
						}
					}
				}
			}
		}
	}

	private String renderCheck(CheckConstraint constraint, CheckConstraintPlacement placement) {
		return dialect.getCheckConstraintSupport().render( new CheckConstraintRenderRequest(
				placement,
				constraint.getName(),
				constraint.getConstraint(),
				constraint.getOptions()
		) );
	}

	private boolean isArray(AggregateColumn aggregateColumn) {
		final var value = (BasicValue) aggregateColumn.getValue();
		return switch ( value.getResolution().getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.STRUCT_ARRAY, SqlTypes.STRUCT_TABLE, SqlTypes.JSON_ARRAY, SqlTypes.XML_ARRAY, SqlTypes.ARRAY
					-> true;
			default -> false;
		};
	}

	/// Append the standard aggregate-column check constraint.
	///
	/// Override without calling `super` to suppress this check for an aggregate
	/// representation unsupported by the database.
	protected void applyAggregateColumnCheck(StringBuilder buf, AggregateColumn aggregateColumn) {
		final var aggregateSupport = dialect.getAggregateSupport();
		final int checkStart = buf.length();
		buf.append( ", check (" );
		final int start = buf.length();

		// TODO: consider support for pg_jsonschema
		applyAggregateColumnCheck(
				buf,
				"",
				aggregateColumn,
				null,
				aggregateSupport,
				aggregateColumn.getComponent()
		);

		if ( buf.length() == start ) {
			buf.setLength( checkStart );
		}
		else {
			buf.append( ')' );
		}
	}

	private String applyAggregateColumnCheck(
			StringBuilder buf,
			String separator,
			AggregateColumn aggregateColumn,
			String aggregatePath,
			AggregateSupport aggregateSupport,
			Value value) {
		if ( value instanceof Component component ) {
			final var subAggregateColumn = component.getAggregateColumn();
			if ( subAggregateColumn != null && !isArray( subAggregateColumn )  ) {
				final String subAggregatePath =
						subAggregateColumn.getAggregateReadExpressionTemplate( dialect )
								.replace( Template.TEMPLATE + ".", "" );
				final int checkStart = buf.length();
				if ( subAggregateColumn.isNullable() ) {
					buf.append( subAggregatePath );
					buf.append( " is null or (" );
				}
				final int start = buf.length();
				separator = "";
				for ( var property : component.getProperties() ) {
					separator = applyAggregateColumnCheck(
							buf,
							separator,
							subAggregateColumn,
							subAggregatePath,
							aggregateSupport,
							property.getValue()
					);
				}

				if ( buf.length() == start ) {
					buf.setLength( checkStart );
				}
				else if ( aggregateColumn.isNullable() ) {
					buf.append( ')' );
				}
			}
		}
		else {
			for ( var subColumn : value.getColumns() ) {
				final String checkConstraint = getCheckConstraint( subColumn );
				if ( !subColumn.isNullable() || checkConstraint != null ) {
					final String subColumnName = subColumn.getQuotedName( dialect );
					final String columnExpression = aggregateSupport.aggregateComponentCustomReadExpression(
							new AggregateComponentReadRequest(
									subColumnName,
									subColumnName,
									aggregatePath,
									subColumnName,
									AggregateColumnDescriptorAdapter.effectiveSqlTypeCode( aggregateColumn ),
									AggregateColumnDescriptorAdapter.mapping( subColumn ),
									aggregateColumn.getComponent().getMetadata().getTypeConfiguration()
							)
					);
					if ( !subColumn.isNullable() ) {
						buf.append( separator );
						buf.append( columnExpression );
						buf.append( " is not null" );
						separator = " and ";
					}
					if ( checkConstraint != null ) {
						if ( subColumn.isNullable() ) {
							buf.append( separator );
							buf.append( '(' );
							buf.append( columnExpression );
							buf.append( " is null" );
							separator = " or ";
						}
						buf.append( separator );
						buf.append( checkConstraint.replace( subColumnName, columnExpression ) );
						if ( subColumn.isNullable() ) {
							buf.append( ')' );
						}
						separator = " and ";
					}
				}
			}
		}
		return separator;
	}

	private static String getCheckConstraint(Column subColumn) {
		final var checkConstraints = subColumn.getCheckConstraints();
		if ( checkConstraints.isEmpty() ) {
			return null;
		}
		else if ( checkConstraints.size() > 1 ) {
			throw new MappingException( "Multiple check constraints not supported for aggregate columns" );
		}
		else {
			return checkConstraints.get(0).getConstraint();
		}
	}

	private String tableCreateString(Table table) {
		final String createTableString = dialect.getTableCreationSupport().createTableCommand(
				table.hasPrimaryKey() ? TableCreationKind.STANDARD : TableCreationKind.MULTISET
		);
		final String type = table.getType();
		return isBlank( type )
				? createTableString
				: createTableString.replaceFirst( " (?i:table)",
						' ' + type + " table" );
	}

	/// Render the table's primary-key clause.
	///
	/// Override to change placement of an explicitly named ordering constraint.
	protected String primaryKeyString(PrimaryKey key) {
		final var constraint = new StringBuilder();
		final var orderingUniqueKey = key.getOrderingUniqueKey();
		if ( orderingUniqueKey != null && orderingUniqueKey.isNameExplicit() ) {
			constraint.append( "constraint " )
					.append( orderingUniqueKey.getName() ).append( ' ' );
		}
		constraint.append( "primary key (" );
		boolean first = true;
		for ( var column : key.getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				constraint.append(", ");
			}
			constraint.append( column.getQuotedName( dialect ) );
		}
		return constraint.append(')').toString();
	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {
		final var dropTable = new StringBuilder();
		if ( table.getViewQuery() == null ) {
			dropTable.append( "drop table " );
		}
		else {
			dropTable.append( "drop view " );
		}
		final var placement = dialect.getIfExistsSupport().dropTablePlacement();
		if ( placement == ExistenceCheckPlacement.BEFORE_NAME ) {
			dropTable.append( "if exists " );
		}
		dropTable.append( context.format( getTableName( table ) ) )
				.append( dialect.getSchemaDropSupport().cascadeConstraintsClause() );
		if ( placement == ExistenceCheckPlacement.AFTER_NAME ) {
			dropTable.append( " if exists" );
		}
		return new String[] { dropTable.toString() };
	}

	private static QualifiedName getTableName(Table table) {
		return new QualifiedNameParser.NameParts(
				toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		);
	}
}
