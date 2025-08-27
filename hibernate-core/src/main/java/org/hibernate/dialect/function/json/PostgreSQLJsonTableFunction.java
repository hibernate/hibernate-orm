/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
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
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Map;

/**
 * PostgreSQL json_table function.
 */
public class PostgreSQLJsonTableFunction extends JsonTableFunction {

	public PostgreSQLJsonTableFunction(TypeConfiguration typeConfiguration) {
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
			throw new QueryException( "Can't emulate null on error clause on PostgreSQL" );
		}
		sqlAppender.appendSql( "(select" );

		renderColumns( sqlAppender, arguments.columnsClause(), 0, walker );

		sqlAppender.appendSql( " from jsonb_path_query(" );

		final boolean needsCast = !arguments.isJsonType() && AbstractSqlAstTranslator.isParameter( arguments.jsonDocument() );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		arguments.jsonDocument().accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		final SqlAstNode jsonPath = arguments.jsonPath();
		if ( jsonPath != null ) {
			sqlAppender.appendSql( ',' );
			if ( jsonPath instanceof Literal ) {
				jsonPath.accept( walker );
			}
			else {
				sqlAppender.appendSql( "cast(" );
				jsonPath.accept( walker );
				sqlAppender.appendSql( " as jsonpath)" );
			}
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause != null ) {
				sqlAppender.append( ",jsonb_build_object" );
				char separator = '(';
				for ( Map.Entry<String, Expression> entry : passingClause.getPassingExpressions().entrySet() ) {
					sqlAppender.append( separator );
					sqlAppender.appendSingleQuoteEscapedString( entry.getKey() );
					sqlAppender.append( ',' );
					entry.getValue().accept( walker );
					separator = ',';
				}
				sqlAppender.append( ')' );
			}
		}
		else {
			sqlAppender.appendSql( ",'$[*]'" );
		}
		sqlAppender.appendSql( ") with ordinality t0(d,i)" );
		renderNestedColumnJoins( sqlAppender, arguments.columnsClause(), 0, walker );

		sqlAppender.appendSql( ')' );
	}

	protected int renderNestedColumnJoins(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		int nextClauseLevel = clauseLevel;
		for ( JsonTableColumnDefinition columnDefinition : jsonTableColumnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableNestedColumnDefinition nestedColumnDefinition ) {
				sqlAppender.appendSql( " left join lateral jsonb_path_query(t" );
				sqlAppender.appendSql( clauseLevel );
				sqlAppender.appendSql( ".d," );
				sqlAppender.appendSingleQuoteEscapedString( nestedColumnDefinition.jsonPath() );
				sqlAppender.appendSql( ") with ordinality t" );
				sqlAppender.appendSql( clauseLevel + 1 );
				sqlAppender.appendSql( "(d,i) on true" );
				nextClauseLevel = renderNestedColumnJoins( sqlAppender, nestedColumnDefinition.columns(), clauseLevel + 1, walker );
			}
		}
		return nextClauseLevel;
	}

	@Override
	protected int renderColumns(SqlAppender sqlAppender, JsonTableColumnsClause jsonTableColumnsClause, int clauseLevel, SqlAstTranslator<?> walker) {
		return renderColumnDefinitions( sqlAppender, jsonTableColumnsClause, ' ', clauseLevel, walker );
	}

	@Override
	protected int renderJsonNestedColumnDefinition(SqlAppender sqlAppender, JsonTableNestedColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		return renderColumns( sqlAppender, definition.columns(), clauseLevel, walker );
	}

	@Override
	protected void renderJsonOrdinalityColumnDefinition(SqlAppender sqlAppender, JsonTableOrdinalityColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( 't' );
		sqlAppender.appendSql( clauseLevel );
		sqlAppender.appendSql( ".i " );
		sqlAppender.appendSql( definition.name() );
	}

	@Override
	protected void renderJsonValueColumnDefinition(SqlAppender sqlAppender, JsonTableValueColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// jsonb_path_query_first errors by default
		if ( definition.errorBehavior() != null && definition.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on PostgreSQL" );
		}
		if ( definition.emptyBehavior() != null && definition.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on PostgreSQL" );
		}
		final String jsonPath = definition.jsonPath() == null
				? "$." + definition.name()
				: definition.jsonPath();
		PostgreSQLJsonValueFunction.appendJsonValue(
				sqlAppender,
				new ClauseLevelDocumentExpression( clauseLevel ),
				new QueryLiteral<>(
						jsonPath,
						walker.getSessionFactory().getTypeConfiguration().getBasicTypeForJavaType( String.class )
				),
				true,
				definition.type(),
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
			throw new QueryException( "Can't emulate on error clause on PostgreSQL" );
		}
		if ( definition.emptyBehavior() != null && definition.emptyBehavior() != JsonQueryEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on PostgreSQL" );
		}
		final String jsonPath = definition.jsonPath() == null
				? "$." + definition.name()
				: definition.jsonPath();
		PostgreSQLJsonQueryFunction.appendJsonQuery(
				sqlAppender,
				new ClauseLevelDocumentExpression( clauseLevel ),
				new QueryLiteral<>(
						jsonPath,
						walker.getSessionFactory().getTypeConfiguration().getBasicTypeForJavaType( String.class )
				),
				true,
				definition.wrapMode(),
				null,
				walker
		);
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}

	@Override
	protected void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// jsonb_path_exists errors by default
		if ( definition.errorBehavior() != null && definition.errorBehavior() != JsonExistsErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on PostgreSQL" );
		}
		final String jsonPath = definition.jsonPath() == null
				? "$." + definition.name()
				: definition.jsonPath();
		PostgreSQLJsonExistsFunction.appendJsonExists(
				sqlAppender,
				walker,
				new ClauseLevelDocumentExpression( clauseLevel ),
				new QueryLiteral<>(
						jsonPath,
						walker.getSessionFactory().getTypeConfiguration().getBasicTypeForJavaType( String.class )
				),
				null
		);
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}

	protected static class ClauseLevelDocumentExpression implements SelfRenderingExpression {
		private final int clauseLevel;

		public ClauseLevelDocumentExpression(int clauseLevel) {
			this.clauseLevel = clauseLevel;
		}

		@Override
		public void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
			sqlAppender.appendSql( 't' );
			sqlAppender.appendSql( clauseLevel );
			sqlAppender.appendSql( ".d" );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return null;
		}
	}
}
