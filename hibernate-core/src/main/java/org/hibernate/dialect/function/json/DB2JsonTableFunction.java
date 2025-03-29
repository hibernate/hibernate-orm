/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.CteGenerateSeriesFunction;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * DB2 json_table function.
 * This implementation/emulation goes to great lengths to ensure Hibernate ORM can provide the same {@code json_table()}
 * experience that other dialects provide also on DB2.
 * The most notable limitation of the DB2 function is that it doesn't support JSON arrays,
 * so this emulation uses a series CTE called {@code max_series} with 10_000 rows to join
 * each array element queried with {@code json_query()} at the respective index via {@code json_table()} separately.
 * Another notable limitation of the DB2 function is that it doesn't support nested column paths,
 * which requires emulation by joining each nesting with a separate {@code json_table()}.
 */
public class DB2JsonTableFunction extends JsonTableFunction {

	private final int maximumSeriesSize;

	public DB2JsonTableFunction(int maximumSeriesSize, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.maximumSeriesSize = maximumSeriesSize;
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(List<? extends SqmTypedNode<?>> sqmArguments, QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmJsonTableFunction<>(
				this,
				this,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				(SqmExpression<?>) sqmArguments.get( 0 ),
				sqmArguments.size() > 1 ? (SqmExpression<String>) sqmArguments.get( 1 ) : null
		) {
			@Override
			public TableGroup convertToSqlAst(NavigablePath navigablePath, String identifierVariable, boolean lateral, boolean canUseInnerJoins, boolean withOrdinality, SqmToSqlAstConverter walker) {
				final FunctionTableGroup tableGroup = (FunctionTableGroup) super.convertToSqlAst( navigablePath, identifierVariable, lateral, canUseInnerJoins, withOrdinality, walker );
				final JsonTableArguments arguments = JsonTableArguments.extract( tableGroup.getPrimaryTableReference().getFunctionExpression().getArguments() );
				final Expression jsonPath = arguments.jsonPath();
				final boolean isArray = !(jsonPath instanceof Literal literal)
						|| isArrayAccess( (String) literal.getLiteralValue() );
				if ( isArray || hasNestedArray( arguments.columnsClause() ) ) {
					walker.registerQueryTransformer( new SeriesQueryTransformer( maximumSeriesSize ) );
				}
				return tableGroup;
			}
		};
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			JsonTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() == JsonTableErrorBehavior.NULL ) {
			throw new QueryException( "Can't emulate null on error clause on DB2" );
		}
		final Expression jsonDocument = arguments.jsonDocument();
		final Expression jsonPath = arguments.jsonPath();
		final boolean isArray = isArrayAccess( jsonPath, walker );
		sqlAppender.appendSql( "lateral(select" );
		renderColumnSelects( sqlAppender, arguments.columnsClause(), 0, isArray );
		sqlAppender.appendSql( " from " );

		if ( isArray ) {
			sqlAppender.appendSql( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.NAME );
			sqlAppender.appendSql( " i join " );
		}
		sqlAppender.appendSql( "json_table(" );
		// DB2 json functions only work when passing object documents,
		// which is why an array element query result is packed in shell object `{"a":...}`
		if ( isArray ) {
			sqlAppender.appendSql( "'{\"a\":'||" );
		}
		appendJsonDocument( sqlAppender, jsonPath, jsonDocument, arguments.passingClause(), isArray, walker );
		if ( isArray ) {
			sqlAppender.appendSql( "||'}'" );
		}
		sqlAppender.appendSql( ",'strict $'" );
		renderColumns( sqlAppender, arguments.columnsClause(), 0, isArray ? "$.a" : null, walker );
		sqlAppender.appendSql( " error on error) t0" );
		if ( isArray ) {
			sqlAppender.appendSql( " on json_exists('{\"a\":'||" );
			if ( jsonPath != null ) {
				final String jsonPathString;
				if ( arguments.passingClause() != null ) {
					jsonPathString = JsonPathHelper.inlinedJsonPathIncludingPassingClause( jsonPath, arguments.passingClause(), walker );
				}
				else {
					jsonPathString = walker.getLiteralValue( jsonPath );
				}
				if ( jsonPathString.endsWith( "[*]" ) ) {
					jsonDocument.accept( walker );
					sqlAppender.appendSql( "||'}'," );
					final String adaptedJsonPath = jsonPathString.substring( 0, jsonPathString.length() - 3 );
					sqlAppender.appendSingleQuoteEscapedString( adaptedJsonPath.replace( "$", "$.a" ) );
					sqlAppender.appendSql( "||'['||(i.i-1)||']')" );
				}
				else {
					sqlAppender.appendSql( "json_query('{\"a\":'||" );
					jsonDocument.accept( walker );
					sqlAppender.appendSql( "||'}'," );
					sqlAppender.appendSingleQuoteEscapedString( jsonPathString.replace( "$", "$.a" ) );
					sqlAppender.appendSql( " with wrapper)||'}','$.a['||(i.i-1)||']')" );
				}
			}
			else {
				jsonDocument.accept( walker );
				sqlAppender.appendSql( "||'}','$.a['||(i.i-1)||']')" );
			}
		}
		renderNestedColumnJoins( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( ')' );
	}

	private static void appendJsonDocument(SqlAppender sqlAppender, Expression jsonPath, Expression jsonDocument, JsonPathPassingClause passingClause, boolean isArray, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			sqlAppender.appendSql( "json_query(" );
			if ( isArray ) {
				final String jsonPathString;
				if ( passingClause != null ) {
					jsonPathString = JsonPathHelper.inlinedJsonPathIncludingPassingClause( jsonPath, passingClause, walker );
				}
				else {
					jsonPathString = walker.getLiteralValue( jsonPath );
				}
				if ( jsonPathString.endsWith( "[*]" ) ) {
					sqlAppender.appendSql( "'{\"a\":'||" );
					jsonDocument.accept( walker );
					sqlAppender.appendSql( "||'}'," );
					final String adaptedJsonPath = jsonPathString.substring( 0, jsonPathString.length() - 3 );
					sqlAppender.appendSingleQuoteEscapedString( adaptedJsonPath.replace( "$", "$.a" ) );
					sqlAppender.appendSql( "||'['||(i.i-1)||']'" );
				}
				else {
					sqlAppender.appendSql( "'{\"a\":'||" );
					sqlAppender.appendSql( "json_query('{\"a\":'||" );
					jsonDocument.accept( walker );
					sqlAppender.appendSql( "||'}'," );
					sqlAppender.appendSingleQuoteEscapedString( jsonPathString.replace( "$", "$.a" ) );
					sqlAppender.appendSql( " with wrapper)||'}','$.a['||(i.i-1)||']'" );
				}
			}
			else {
				jsonDocument.accept( walker );
				sqlAppender.appendSql( ',' );
				if ( passingClause != null ) {
					JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
							sqlAppender,
							"",
							jsonPath,
							passingClause,
							walker
					);
				}
				else {
					jsonPath.accept( walker );
				}
			}
			sqlAppender.appendSql( ')' );
		}
		else {
			if ( isArray ) {
				sqlAppender.appendSql( "json_query('{\"a\":'||" );
			}
			jsonDocument.accept( walker );
			if ( isArray ) {
				sqlAppender.appendSql( "||'}','$.a['||(i.i-1)||']')" );
			}
		}
	}

	private boolean isArrayAccess(@Nullable Expression jsonPath, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			try {
				return isArrayAccess( walker.getLiteralValue( jsonPath ) );
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		// Assume array by default
		return true;
	}


	private boolean isArrayAccess(String jsonPath) {
		return jsonPath.endsWith( "[*]" );
	}

	private boolean hasNestedArray(JsonTableColumnsClause jsonTableColumnsClause) {
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				if ( isArrayAccess( nestedColumnDefinition.jsonPath() )
						|| hasNestedArray( nestedColumnDefinition.columns() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private int renderNestedColumnJoins(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		int currentClauseLevel = clauseLevel;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				// DB2 doesn't support the nested path syntax, so emulate it by lateral joining json_table()
				final int nextClauseLevel = currentClauseLevel + 1;
				final boolean isArray = isArrayAccess( nestedColumnDefinition.jsonPath() );

				sqlAppender.appendSql( " left join lateral (select" );
				renderColumnSelects( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, isArray );
				sqlAppender.appendSql( " from " );

				if ( isArray ) {
					// When the JSON path indicates that the document is an array,
					// join the `max_series` CTE to be able to use the respective array element in json_table().
					// DB2 json functions only work when passing object documents,
					// which is why results are packed in shell object `{"a":...}`
					sqlAppender.appendSql( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.NAME );
					sqlAppender.appendSql( " i join json_table('{\"a\":'||json_query('{\"a\":'||t" );
					sqlAppender.appendSql( clauseLevel );
					sqlAppender.appendSql( ".nested_" );
					sqlAppender.appendSql( nextClauseLevel );
					sqlAppender.appendSql( "_||'}','$.a['||(i.i-1)||']')||'}','strict $'" );
					// Since the query results are packed in a shell object `{"a":...}`,
					// the JSON path for columns need to be prefixed with `$.a`
					renderColumns( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, "$.a", walker );
					sqlAppender.appendSql( " error on error) t" );
					sqlAppender.appendSql( nextClauseLevel );
					// Emulation of arrays via `max_series` sequence requires a join condition to check if an array element exists
					sqlAppender.appendSql( " on json_exists('{\"a\":'||t" );
					sqlAppender.appendSql( clauseLevel );
					sqlAppender.appendSql( ".nested_" );
					sqlAppender.appendSql( nextClauseLevel );
					sqlAppender.appendSql( "_||'}','$.a['||(i.i-1)||']')" );
				}
				else {
					sqlAppender.appendSql( "json_table(t" );
					sqlAppender.appendSql( clauseLevel );
					sqlAppender.appendSql( ".nested_" );
					sqlAppender.appendSql( nextClauseLevel );
					sqlAppender.appendSql( "_,'strict $'" );
					renderColumns( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, null, walker );
					sqlAppender.appendSql( " error on error) t" );
					sqlAppender.appendSql( nextClauseLevel );
				}
				sqlAppender.appendSql( ") t" );
				sqlAppender.appendSql( nextClauseLevel );
				sqlAppender.appendSql( " on 1=1" );
				currentClauseLevel = renderNestedColumnJoins( sqlAppender, nestedColumnDefinition.columns(), nextClauseLevel, walker );
			}
		}
		return currentClauseLevel;
	}

	private void renderColumnSelects(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, boolean isArray) {
		int currentClauseLevel = clauseLevel;
		char separator = ' ';
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			sqlAppender.appendSql( separator );
			if ( columnDefinition instanceof JsonTableExistsColumnDefinition existsColumnDefinition ) {
				// DB2 doesn't support the exists syntax in json_table(),
				// so emulate it by selecting the json_exists() result
				sqlAppender.appendSql( "json_exists(t" );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( "." );
				sqlAppender.appendSql( existsColumnDefinition.name() );
				sqlAppender.appendSql( ',' );
				final String jsonPath = existsColumnDefinition.jsonPath() == null
						? "$." + existsColumnDefinition.name()
						: existsColumnDefinition.jsonPath();
				sqlAppender.appendSingleQuoteEscapedString( jsonPath );
				final JsonExistsErrorBehavior errorBehavior = existsColumnDefinition.errorBehavior();
				if ( errorBehavior != null && errorBehavior != JsonExistsErrorBehavior.FALSE ) {
					if ( errorBehavior == JsonExistsErrorBehavior.TRUE ) {
						sqlAppender.appendSql( " true on error" );
					}
					else {
						sqlAppender.appendSql( " error on error" );
					}
				}
				sqlAppender.appendSql( ") " );
				sqlAppender.appendSql( existsColumnDefinition.name() );
			}
			else if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition ordinalityColumnDefinition ) {
				// DB2 doesn't support the for ordinality syntax in json_table() since it has no support for array either
				if ( isArray ) {
					// If the document is an array, a series table with alias `i` is joined to emulate array support.
					sqlAppender.appendSql( "i.i " );
				}
				else {
					// The ordinality for non-array documents always is trivially 1
					sqlAppender.appendSql( "1 " );
				}
				sqlAppender.appendSql( ordinalityColumnDefinition.name() );
			}
			else if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				// A join is created in #renderNestedColumnJoins under the alias `t<currentClauseLevel + 1>`
				// which holds all nested columns, so just select that directly
				sqlAppender.appendSql( 't' );
				sqlAppender.appendSql( currentClauseLevel + 1 );
				sqlAppender.appendSql( ".*" );
				currentClauseLevel += 1 + countNestedColumnDefinitions( nestedColumnDefinition.columns() );
			}
			else if ( columnDefinition instanceof JsonTableValueColumnDefinition valueColumnDefinition ) {
				// Just pass-through value columns in the select clause
				sqlAppender.appendSql( 't' );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( '.' );
				sqlAppender.appendSql( valueColumnDefinition.name() );
			}
			else {
				// Just pass-through query columns in the select clause
				final JsonTableQueryColumnDefinition queryColumnDefinition = (JsonTableQueryColumnDefinition) columnDefinition;
				sqlAppender.appendSql( 't' );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( '.' );
				sqlAppender.appendSql( queryColumnDefinition.name() );
			}
			separator = ',';
		}
	}

	private int renderColumns(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, @Nullable String jsonPathPrefix, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( " columns" );
		int nextClauseLevel = clauseLevel + 1;
		char separator = '(';
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			sqlAppender.appendSql( separator );
			if ( columnDefinition instanceof JsonTableExistsColumnDefinition definition ) {
				renderJsonExistsColumnDefinition( sqlAppender, definition );
			}
			else if ( columnDefinition instanceof JsonTableQueryColumnDefinition definition ) {
				renderJsonQueryColumnDefinition( sqlAppender, definition, jsonPathPrefix, walker );
			}
			else if ( columnDefinition instanceof JsonTableValueColumnDefinition definition ) {
				renderJsonValueColumnDefinition( sqlAppender, definition, jsonPathPrefix, walker );
			}
			else if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition definition ) {
				renderJsonOrdinalityColumnDefinition( sqlAppender, definition );
			}
			else {
				nextClauseLevel = renderJsonNestedColumnDefinition( sqlAppender, (JsonTableNestedColumnDefinition) columnDefinition, nextClauseLevel );
			}
			separator = ',';
		}
		sqlAppender.appendSql( ')' );
		return nextClauseLevel;
	}

	private void renderColumnPath(String name, @Nullable String jsonPath, @Nullable String jsonPathPrefix, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		if ( jsonPath != null ) {
			super.renderColumnPath(
					name,
					jsonPathPrefix != null
							? jsonPathPrefix + jsonPath.substring( 1 )
							: jsonPath,
					sqlAppender,
					walker
			);
		}
		else {
			// We can either double quote the column name to make it case-sensitive, or use an explicit JSON path.
			// Using an explicit JSON path is easier though since we don't know where the column is going to be used
			sqlAppender.appendSql( " path '" );
			if ( jsonPathPrefix == null ) {
				sqlAppender.appendSql( '$' );
			}
			else {
				sqlAppender.appendSql( jsonPathPrefix );
			}
			sqlAppender.appendSql( '.' );
			sqlAppender.appendSql( name );
			sqlAppender.appendSql( '\'' );
		}
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		final String columnType = super.determineColumnType( castTarget, walker );
		switch ( columnType ) {
			// Boolean not supported in json_table()
			case "boolean":
				return "smallint";
		}
		return columnType;
	}

	private void renderJsonQueryColumnDefinition(SqlAppender sqlAppender, JsonTableQueryColumnDefinition definition, @Nullable String jsonPathPrefix, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( new CastTarget( definition.type() ), walker ) );
		if ( definition.type().getJdbcType().getDdlTypeCode() != SqlTypes.JSON ) {
			sqlAppender.appendSql( " format json" );
		}

		if ( definition.wrapMode() != null ) {
			switch ( definition.wrapMode() ) {
				case WITH_WRAPPER -> sqlAppender.appendSql( " with wrapper" );
				case WITHOUT_WRAPPER -> sqlAppender.appendSql( " without wrapper" );
				case WITH_CONDITIONAL_WRAPPER -> sqlAppender.appendSql( " with conditional wrapper" );
			}
		}

		// Custom implementation of query rendering to pass through our path prefix
		renderColumnPath( definition.name(), definition.jsonPath(), jsonPathPrefix, sqlAppender, walker );

		if ( definition.errorBehavior() != null ) {
			switch ( definition.errorBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on error" );
				case NULL -> sqlAppender.appendSql( " null on error" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " empty object on error" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " empty array on error" );
			}
		}

		if ( definition.emptyBehavior() != null ) {
			switch ( definition.emptyBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on empty" );
				case NULL -> sqlAppender.appendSql( " null on empty" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " empty object on empty" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " empty array on empty" );
			}
		}
	}

	private void renderJsonValueColumnDefinition(SqlAppender sqlAppender, JsonTableValueColumnDefinition definition, @Nullable String jsonPathPrefix, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( definition.type(), walker ) );

		// Custom implementation of value rendering to pass through our path prefix
		renderColumnPath( definition.name(), definition.jsonPath(), jsonPathPrefix, sqlAppender, walker );

		if ( definition.errorBehavior() != null ) {
			if ( definition.errorBehavior() == JsonValueErrorBehavior.ERROR ) {
				sqlAppender.appendSql( " error on error" );
			}
			else if ( definition.errorBehavior() != JsonValueErrorBehavior.NULL ) {
				final Expression defaultExpression = definition.errorBehavior().getDefaultExpression();
				assert defaultExpression != null;
				sqlAppender.appendSql( " default " );
				defaultExpression.accept( walker );
				sqlAppender.appendSql( " on error" );
			}
		}
		if ( definition.emptyBehavior() != null ) {
			if ( definition.emptyBehavior() == JsonValueEmptyBehavior.ERROR ) {
				sqlAppender.appendSql( " error on empty" );
			}
			else if ( definition.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
				final Expression defaultExpression = definition.emptyBehavior().getDefaultExpression();
				assert defaultExpression != null;
				sqlAppender.appendSql( " default " );
				defaultExpression.accept( walker );
				sqlAppender.appendSql( " on empty" );
			}
		}
	}

	private void renderJsonOrdinalityColumnDefinition(SqlAppender sqlAppender, JsonTableOrdinalityColumnDefinition definition) {
		// DB2 doesn't support for ordinality syntax since it also doesn't support arrays
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " clob format json path '$'" );
	}

	private int renderJsonNestedColumnDefinition(SqlAppender sqlAppender, JsonTableNestedColumnDefinition definition, int clauseLevel) {
		// DB2 doesn't support nested path syntax, so just select the nested path as json clob and join that later
		sqlAppender.appendSql( "nested_" );
		sqlAppender.appendSql( clauseLevel );
		sqlAppender.appendSql( "_ clob format json path " );
		// Strip off array element access from JSON path to select the array as a whole for later processing
		final String jsonPath = isArrayAccess( definition.jsonPath() )
				? definition.jsonPath().substring( 0, definition.jsonPath().length() - 3 )
				: definition.jsonPath();
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		return clauseLevel + countNestedColumnDefinitions( definition.columns() );
	}

	private void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition) {
		// DB2 doesn't support exists syntax, so select the whole document against which an exists check
		// is made through the json_exists() function in the select clause
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " clob format json path '$'" );
	}

	public static class SeriesQueryTransformer implements QueryTransformer {

		private final int maxSeriesSize;

		public SeriesQueryTransformer(int maxSeriesSize) {
			this.maxSeriesSize = maxSeriesSize;
		}

		@Override
		public QuerySpec transform(CteContainer cteContainer, QuerySpec querySpec, SqmToSqlAstConverter converter) {
			if ( cteContainer.getCteStatement( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.NAME ) == null ) {
				cteContainer.addCteStatement( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.createSeriesCte( maxSeriesSize, converter ) );
			}
			return querySpec;
		}
	}
}
