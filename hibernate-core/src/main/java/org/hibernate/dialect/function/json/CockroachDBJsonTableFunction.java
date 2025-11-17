/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * CockroachDB json_table function.
 */
public class CockroachDBJsonTableFunction extends PostgreSQLJsonTableFunction {

	public CockroachDBJsonTableFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			JsonTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() == JsonTableErrorBehavior.NULL ) {
			throw new QueryException( "Can't emulate null on error clause on CockroachDB" );
		}
		final SqlAstNode jsonPathExpression = arguments.jsonPath();
		final List<JsonPathHelper.JsonPathElement> jsonPathElements;
		final boolean isArray;
		if ( jsonPathExpression == null ) {
			jsonPathElements = Collections.emptyList();
			// Assume array by default
			isArray = true;
		}
		else {
			final String jsonPath;
			try {
				jsonPath = walker.getLiteralValue( arguments.jsonPath() );
			}
			catch (Exception ex) {
				throw new QueryException( "CockroachDB json_table only support literal json paths, but got " + arguments.jsonPath() );
			}
			isArray = jsonPath.endsWith( "[*]" );
			if ( isArray ) {
				jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath.substring( 0, jsonPath.length() - 3 ) );
			}
			else {
				jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
			}
		}

		sqlAppender.appendSql( "(select" );

		renderColumns( sqlAppender, arguments.columnsClause(), 0, walker );

		sqlAppender.appendSql( " from " );
		if ( isArray ) {
			sqlAppender.appendSql( "jsonb_array_elements(" );
		}
		else {
			sqlAppender.appendSql( "(values (" );
		}
		CockroachDBJsonQueryFunction.appendJsonQuery(
				sqlAppender,
				arguments.jsonDocument(),
				jsonPathElements,
				arguments.isJsonType(),
				arguments.passingClause(),
				walker
		);
		if ( isArray ) {
			sqlAppender.appendSql( ") with ordinality t0(d,i)" );
		}
		else {
			sqlAppender.appendSql( ",1)) t0(d,i)" );
		}
		renderNestedColumnJoins( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected int renderNestedColumnJoins(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		int nextClauseLevel = clauseLevel;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				sqlAppender.appendSql( " left join lateral " );
				final boolean isArray = nestedColumnDefinition.jsonPath().endsWith( "[*]" );
				final String jsonPath;
				if ( isArray ) {
					jsonPath = nestedColumnDefinition.jsonPath().substring( 0, nestedColumnDefinition.jsonPath().length() - 3 );
					sqlAppender.appendSql( "jsonb_array_elements(" );
				}
				else {
					jsonPath = nestedColumnDefinition.jsonPath();
					sqlAppender.appendSql( "(values (" );
				}
				CockroachDBJsonQueryFunction.appendJsonQuery(
						sqlAppender,
						new ClauseLevelDocumentExpression( clauseLevel ),
						JsonPathHelper.parseJsonPathElements( jsonPath ),
						true,
						null,
						walker
				);
				if ( isArray ) {
					sqlAppender.appendSql( ") with ordinality t" );
				}
				else {
					sqlAppender.appendSql( ",1)) t" );
				}
				sqlAppender.appendSql( clauseLevel + 1 );
				sqlAppender.appendSql( "(d,i) on true" );
				nextClauseLevel = renderNestedColumnJoins( sqlAppender, nestedColumnDefinition.columns(), clauseLevel + 1, walker );
			}
		}
		return nextClauseLevel;
	}

	@Override
	protected void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// jsonb_path_exists errors by default
		if ( definition.errorBehavior() != null && definition.errorBehavior() != JsonExistsErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on CockroachDB" );
		}
		final String jsonPath = definition.jsonPath() == null
				? "$." + definition.name()
				: definition.jsonPath();
		CockroachDBJsonExistsFunction.appendJsonExists(
				sqlAppender,
				new ClauseLevelDocumentExpression( clauseLevel ),
				JsonPathHelper.parseJsonPathElements( jsonPath ),
				true,
				null,
				walker
		);
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}

	@Override
	protected void renderJsonQueryColumnDefinition(SqlAppender sqlAppender, JsonTableQueryColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// jsonb_path_query functions error by default
		if ( definition.errorBehavior() != null && definition.errorBehavior() != JsonQueryErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on CockroachDB" );
		}
		if ( definition.emptyBehavior() != null && definition.emptyBehavior() != JsonQueryEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on CockroachDB" );
		}
		final JsonQueryWrapMode wrapMode = definition.wrapMode();

		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "jsonb_build_array(" );
		}
		final String jsonPath = definition.jsonPath() == null
				? "$." + definition.name()
				: definition.jsonPath();
		CockroachDBJsonQueryFunction.appendJsonQuery(
				sqlAppender,
				new ClauseLevelDocumentExpression( clauseLevel ),
				JsonPathHelper.parseJsonPathElements( jsonPath ),
				true,
				null,
				walker
		);
		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( ")" );
		}
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}

	@Override
	protected void renderJsonValueColumnDefinition(SqlAppender sqlAppender, JsonTableValueColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// jsonb_path_query_first errors by default
		if ( definition.errorBehavior() != null && definition.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on CockroachDB" );
		}
		if ( definition.emptyBehavior() != null && definition.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on CockroachDB" );
		}
		final String jsonPath = definition.jsonPath() == null
				? "$." + definition.name()
				: definition.jsonPath();
		CockroachDBJsonValueFunction.appendJsonValue(
				sqlAppender,
				new ClauseLevelDocumentExpression( clauseLevel ),
				JsonPathHelper.parseJsonPathElements( jsonPath ),
				true,
				null,
				definition.type(),
				walker
		);
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}
}
