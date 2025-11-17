/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.QueryException;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * SQL Server json_table function.
 */
public class SQLServerJsonTableFunction extends JsonTableFunction {

	public SQLServerJsonTableFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderJsonTable(SqlAppender sqlAppender, JsonTableArguments arguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "(select" );
		renderColumnSelects( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( " from openjson(" );
		arguments.jsonDocument().accept( walker );
		if ( arguments.jsonPath() != null ) {
			sqlAppender.appendSql( ',' );
			final String rawJsonPath;
			if ( arguments.passingClause() != null ) {
				rawJsonPath = JsonPathHelper.inlinedJsonPathIncludingPassingClause(
						arguments.jsonPath(),
						arguments.passingClause(),
						walker
				);
			}
			else {
				rawJsonPath = walker.getLiteralValue( arguments.jsonPath() );
			}
			final String jsonPath;
			if ( arguments.errorBehavior() == JsonTableErrorBehavior.ERROR ) {
				// Default behavior is NULL ON ERROR
				jsonPath = "strict " + rawJsonPath;
			}
			else {
				jsonPath = rawJsonPath;
			}
			sqlAppender.appendSingleQuoteEscapedString(
					// openjson unwraps arrays automatically and doesn't support this syntax, so remove it
					jsonPath.endsWith( "[*]" )
							? jsonPath.substring( 0, jsonPath.length() - 3 )
							: jsonPath
			);
		}
		else if ( arguments.errorBehavior() == JsonTableErrorBehavior.ERROR ) {
			// Default behavior is NULL ON ERROR
			sqlAppender.appendSql( ",'strict $'" );
		}
		sqlAppender.appendSql( ")" );
		renderColumnDefinitions( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( " t0" );
		renderNestedColumnJoins( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( ')' );
	}

	protected int renderNestedColumnJoins(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		int currentClauseLevel = clauseLevel;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				final int nextClauseLevel = currentClauseLevel + 1;
				// For every nested column, we create a lateral join that selects all the columns nested within it
				sqlAppender.appendSql( " cross apply (select" );
				renderColumnSelects( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, walker );
				// The previous table alias will have a special column ready for this join to use for openjson
				sqlAppender.appendSql( " from openjson(t" );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( ".nested_" );
				sqlAppender.appendSql( nextClauseLevel );
				sqlAppender.appendSql( "_)" );
				renderColumnDefinitions( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, walker );
				sqlAppender.appendSql( " t" );
				sqlAppender.appendSql( nextClauseLevel );
				sqlAppender.appendSql( ") t" );
				sqlAppender.appendSql( nextClauseLevel );
				currentClauseLevel = renderNestedColumnJoins( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, walker );
			}
		}
		return currentClauseLevel;
	}

	protected void renderColumnSelects(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		int currentClauseLevel = clauseLevel;
		char separator = ' ';
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			sqlAppender.appendSql( separator );
			if ( columnDefinition instanceof JsonTableValueColumnDefinition valueColumnDefinition ) {
				if ( valueColumnDefinition.errorBehavior() != null && valueColumnDefinition.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
					throw new QueryException( "Can't emulate on error clause for value within json_table() on SQL server" );
				}
				sqlAppender.appendSql( 't' );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( '.' );
				sqlAppender.appendSql( valueColumnDefinition.name() );
				// todo: empty behavior?
			}
			else if ( columnDefinition instanceof JsonTableQueryColumnDefinition queryColumnDefinition ) {
				if ( queryColumnDefinition.errorBehavior() != null && queryColumnDefinition.errorBehavior() != JsonQueryErrorBehavior.ERROR ) {
					throw new QueryException( "Can't emulate on error clause for query within json_table() on SQL server" );
				}
				if ( queryColumnDefinition.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_ARRAY
						|| queryColumnDefinition.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
					sqlAppender.appendSql( "coalesce(" );
				}
				// SQL Server only supports no wildcard in JSON paths, so wrapper can be added statically
				if ( queryColumnDefinition.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
					sqlAppender.appendSql( "'['+" );
				}
				sqlAppender.appendSql( 't' );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( '.' );
				sqlAppender.appendSql( queryColumnDefinition.name() );
				if ( queryColumnDefinition.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
					sqlAppender.appendSql( "+']'" );
				}
				if ( queryColumnDefinition.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_ARRAY ) {
					sqlAppender.appendSql( ",'[]')" );
				}
				else if ( queryColumnDefinition.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
					sqlAppender.appendSql( ",'{}')" );
				}
				sqlAppender.appendSql( ' ' );
				sqlAppender.appendSql( queryColumnDefinition.name() );
			}
			else if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition ordinalityColumnDefinition ) {
				sqlAppender.appendSql( "row_number() over (order by (select null)) " );
				sqlAppender.appendSql( ordinalityColumnDefinition.name() );
			}
			else if ( columnDefinition instanceof JsonTableExistsColumnDefinition existsColumnDefinition ) {
				if ( existsColumnDefinition.errorBehavior() == JsonExistsErrorBehavior.FALSE ) {
					throw new QueryException( "Can't emulate exists false on error for json_table() on SQL Server" );
				}
				final String jsonPath = existsColumnDefinition.jsonPath() == null
						? "$." + existsColumnDefinition.name()
						: existsColumnDefinition.jsonPath();
				final List<JsonPathHelper.JsonPathElement> pathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
				final JsonPathHelper.JsonPathElement lastPathElement = pathElements.get( pathElements.size() - 1 );
				final String prefix = JsonPathHelper.toJsonPath( pathElements, 0, pathElements.size() - 1 );
				final String terminalKey;
				if ( lastPathElement instanceof JsonPathHelper.JsonIndexAccess indexAccess ) {
					terminalKey = String.valueOf( indexAccess.index() );
				}
				else if (lastPathElement instanceof JsonPathHelper.JsonAttribute attribute) {
					terminalKey = attribute.attribute();
				}
				else {
					throw new AssertionFailure( "Unrecognized json path element: " + lastPathElement );
				}

				sqlAppender.appendSql( "coalesce((select 1 from openjson(t" );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( "." );
				sqlAppender.appendSql( existsColumnDefinition.name() );
				sqlAppender.appendSql( ',' );
				sqlAppender.appendSingleQuoteEscapedString( prefix );
				sqlAppender.appendSql( ") t where t.[key]=" );
				sqlAppender.appendSingleQuoteEscapedString( terminalKey );
				sqlAppender.appendSql( "),0) " );
				sqlAppender.appendSql( existsColumnDefinition.name() );
			}
			else {
				final JsonTableNestedColumnDefinition nestedColumnDefinition = (JsonTableNestedColumnDefinition) columnDefinition;
				// Select all the columns of a directly nested column definition
				sqlAppender.appendSql( "t" );
				sqlAppender.appendSql( currentClauseLevel + 1 );
				sqlAppender.appendSql( ".*" );
				currentClauseLevel += 1 + countNestedColumnDefinitions( nestedColumnDefinition.columns() );
			}
			separator = ',';
		}
	}

	protected void renderColumnDefinitions(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		int currentClauseLevel = clauseLevel;
		String separator = " with (";
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition ) {
				// This is implemented differently, so don't render or change the separator
				continue;
			}
			sqlAppender.appendSql( separator );
			if ( columnDefinition instanceof JsonTableQueryColumnDefinition definition ) {
				renderJsonQueryColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else if ( columnDefinition instanceof JsonTableValueColumnDefinition definition ) {
				renderJsonValueColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else if ( columnDefinition instanceof JsonTableExistsColumnDefinition definition ) {
				renderJsonExistsColumnDefinition( sqlAppender, definition, clauseLevel, walker );
			}
			else {
				final JsonTableNestedColumnDefinition nestedColumnDefinition = (JsonTableNestedColumnDefinition) columnDefinition;
				currentClauseLevel = renderJsonNestedColumnDefinition( sqlAppender, nestedColumnDefinition, currentClauseLevel + 1, walker );
			}
			separator = ",";
		}
		if ( ",".equals( separator ) ) {
			sqlAppender.appendSql( ')' );
		}
	}

	@Override
	protected void renderColumnPath(String name, @Nullable String jsonPath, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			sqlAppender.appendSql( ' ' );
			sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		}
	}

	@Override
	protected int renderJsonNestedColumnDefinition(SqlAppender sqlAppender, JsonTableNestedColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "nested_" );
		sqlAppender.appendSql( clauseLevel );
		sqlAppender.appendSql( "_ nvarchar(max) " );
		// Strip off the array spread operator since SQL Server doesn't support that
		final String jsonPath = definition.jsonPath().endsWith( "[*]" )
				? definition.jsonPath().substring( 0, definition.jsonPath().length() - 3 )
				: definition.jsonPath();
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		sqlAppender.appendSql( " as json" );
		return clauseLevel + countNestedColumnDefinitions( definition.columns() );
	}

	@Override
	protected void renderJsonQueryColumnDefinition(SqlAppender sqlAppender, JsonTableQueryColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " nvarchar(max)" );
		renderColumnPath( definition.name(), definition.jsonPath(), sqlAppender, walker );
		sqlAppender.appendSql( " as json" );
	}

	@Override
	protected void renderJsonValueColumnDefinition(SqlAppender sqlAppender, JsonTableValueColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( definition.type(), walker ) );
		renderColumnPath( definition.name(), definition.jsonPath(), sqlAppender, walker );
	}

	@Override
	protected void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " nvarchar(max) '$' as json" );
	}
}
