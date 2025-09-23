/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.sql.Template;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.SqlTypes;

import static java.util.Collections.addAll;
import static java.util.Comparator.comparing;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.appendColumn;

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
		final var tableName = getTableName( table );

		try {
			final String formattedTableName = context.format( tableName );

			final var createTable = new StringBuilder();

			final String viewQuery = table.getViewQuery();
			if ( viewQuery != null ) {
				createTable.append("create view ")
						.append( formattedTableName );
				if ( dialect.requiresColumnListInCreateView() ) {
					createTable.append(" (");
					var sortedColumns =
							table.getColumns().stream()
									.sorted( comparing( c -> viewQuery.indexOf( c.getQuotedName( dialect ) ) ) )
									.toList();
					boolean isFirst = true;
					for ( Column column : sortedColumns ) {
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
				createTable.append(" as ")
						.append( viewQuery );
			}
			else {
				final var extra = new StringBuilder();

				createTable.append( tableCreateString( table.hasPrimaryKey() ) )
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

					extra.append( column.getValue().getExtraCreateTableInfo() );
				}
				if ( table.getRowId() != null ) {
					final String rowIdColumn = dialect.getRowIdColumnString( table.getRowId() );
					if ( rowIdColumn != null ) {
						createTable.append(", ").append( rowIdColumn );
					}
				}
				if ( table.hasPrimaryKey() ) {
					createTable.append( ", " ).append( primaryKeyString( table.getPrimaryKey() ) );
				}

				createTable.append( dialect.getUniqueDelegate().getTableCreationUniqueConstraintsFragment( table, context ) );

				applyTableCheck( table, createTable );

				createTable.append( ')' );

				createTable.append( extra );

				if ( table.getComment() != null ) {
					createTable.append( dialect.getTableComment( table.getComment() ) );
				}

				applyTableTypeString( createTable );
			}

			if ( isNotEmpty( table.getOptions() ) ) {
				createTable.append( " " );
				createTable.append( table.getOptions() );
			}

			final List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( createTable.toString() );
			applyComments( table, formattedTableName, sqlStrings );
			applyInitCommands( table, sqlStrings, context );
			return sqlStrings.toArray( EMPTY_STRINGS );
		}
		catch (Exception e) {
			throw new MappingException( "Error creating SQL 'create' commands for table '"
					+ table.getName() + "' [" + e.getMessage() + "]" , e );
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
			final String comment = table.getComment();
			if ( comment != null && dialect.getTableComment( "" ).isEmpty() ) {
				sqlStrings.add( "comment on table " + formattedTableName + " is '" + comment + "'" );
			}
			if ( dialect.getColumnComment( "" ).isEmpty() ){
				for ( var column : table.getColumns() ) {
					final String columnComment = column.getComment();
					if ( columnComment != null ) {
						sqlStrings.add(
								"comment on column " + formattedTableName + '.' + column.getQuotedName( dialect )
										+ " is '" + columnComment + "'"
						);
					}
				}
			}
		}
	}

	protected void applyInitCommands(Table table, List<String> sqlStrings, SqlStringGenerationContext context) {
		for ( var initCommand : table.getInitCommands( context ) ) {
			addAll( sqlStrings, initCommand.initCommands() );
		}
	}

	protected void applyTableTypeString(StringBuilder buf) {
		buf.append( dialect.getTableTypeString() );
	}

	protected void applyTableCheck(Table table, StringBuilder buf) {
		if ( dialect.supportsTableCheck() ) {
			for ( var column : table.getColumns() ) {
				final var checkConstraints = column.getCheckConstraints();
				boolean hasAnonymousConstraints = false;
				if ( !dialect.supportsColumnCheck() ) {
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
				boolean skipNextNamedConstraint = !hasAnonymousConstraints && dialect.supportsNamedColumnCheck();
				for ( var constraint : checkConstraints ) {
					if ( constraint.isNamed() ) {
						if ( skipNextNamedConstraint ) {
							skipNextNamedConstraint = false;
						}
						else {
							buf.append( ',' ).append( constraint.constraintString( dialect ) );
						}
					}
				}
			}
			for ( var constraint : table.getChecks() ) {
				buf.append( "," ).append( constraint.constraintString( dialect ) );
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

	private boolean isArray(AggregateColumn aggregateColumn) {
		final var value = (BasicValue) aggregateColumn.getValue();
		return switch ( value.getResolution().getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.STRUCT_ARRAY, SqlTypes.STRUCT_TABLE, SqlTypes.JSON_ARRAY, SqlTypes.XML_ARRAY, SqlTypes.ARRAY
					-> true;
			default -> false;
		};
	}

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
							subColumnName,
							subColumnName,
							aggregatePath,
							subColumnName,
							aggregateColumn,
							subColumn
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
						buf.append(
								checkConstraint.replace(
										subColumnName,
										columnExpression
								)
						);
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

	protected String tableCreateString(boolean hasPrimaryKey) {
		return hasPrimaryKey ? dialect.getCreateTableString() : dialect.getCreateMultisetTableString();

	}

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
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			dropTable.append( "if exists " );
		}
		dropTable.append( context.format( getTableName( table ) ) )
				.append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
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
