/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MySQL json_table function.
 */
public class MySQLJsonTableFunction extends JsonTableFunction {

	public MySQLJsonTableFunction(TypeConfiguration typeConfiguration) {
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
			throw new QueryException( "Can't emulate null on error clause on MySQL" );
		}
		sqlAppender.appendSql( "json_table(" );
		arguments.jsonDocument().accept( walker );
		if ( arguments.jsonPath() == null ) {
			sqlAppender.appendSql( ",'$'" );
		}
		else {
			sqlAppender.appendSql( ',' );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause != null ) {
				JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
						sqlAppender,
						"",
						arguments.jsonPath(),
						passingClause,
						walker
				);
			}
			else {
				arguments.jsonPath().accept( walker );
			}
		}
		renderColumns( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected int renderJsonNestedColumnDefinition(SqlAppender sqlAppender, JsonTableNestedColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// MySQL docs way that "path" is optional, but it isn't...
		sqlAppender.appendSql( "nested path " );
		sqlAppender.appendSingleQuoteEscapedString( definition.jsonPath() );
		return renderColumns( sqlAppender, definition.columns(), clauseLevel, walker );
	}

	@Override
	protected void renderJsonValueColumnDefinition(SqlAppender sqlAppender, JsonTableValueColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql(determineColumnType( definition.type(), walker ) );

		if ( definition.jsonPath() != null ) {
			sqlAppender.appendSql( " path " );
			sqlAppender.appendSingleQuoteEscapedString( definition.jsonPath() );
		}
		else {
			sqlAppender.appendSql( " path '$." );
			sqlAppender.appendSql( definition.name() );
			sqlAppender.appendSql( "'" );
		}

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
		// todo: mismatch clause?
	}

	@Override
	protected void renderJsonQueryColumnDefinition(SqlAppender sqlAppender, JsonTableQueryColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// Conditional wrapper is the default behavior on MySQL
		if ( definition.wrapMode() != null && definition.wrapMode() != JsonQueryWrapMode.WITH_CONDITIONAL_WRAPPER ) {
			throw new QueryException( "Can't emulate wrapper clause on MySQL" );
		}
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( DdlTypeHelper.getTypeName( definition.type(), walker.getSessionFactory().getTypeConfiguration() ) );
		if ( definition.type().getJdbcType().getDdlTypeCode() != SqlTypes.JSON ) {
			sqlAppender.appendSql( " format json" );
		}

		if ( definition.jsonPath() != null ) {
			sqlAppender.appendSql( " path " );
			sqlAppender.appendSingleQuoteEscapedString( definition.jsonPath() );
		}
		else {
			sqlAppender.appendSql( " path '$." );
			sqlAppender.appendSql( definition.name() );
			sqlAppender.appendSql( "'" );
		}

		if ( definition.errorBehavior() != null ) {
			switch ( definition.errorBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on error" );
				case NULL -> sqlAppender.appendSql( " null on error" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " default '{}' on error" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " default '[]' on error" );
			}
		}

		if ( definition.emptyBehavior() != null ) {
			switch ( definition.emptyBehavior() ) {
				case ERROR -> sqlAppender.appendSql( " error on empty" );
				case NULL -> sqlAppender.appendSql( " null on empty" );
				case EMPTY_OBJECT -> sqlAppender.appendSql( " default '{}' on empty" );
				case EMPTY_ARRAY -> sqlAppender.appendSql( " default '[]' on empty" );
			}
		}
	}

	@Override
	protected void renderJsonExistsColumnDefinition(SqlAppender sqlAppender, JsonTableExistsColumnDefinition definition, int clauseLevel, SqlAstTranslator<?> walker) {
		// jsonb_path_exists errors by default
		if ( definition.errorBehavior() != null && definition.errorBehavior() != JsonExistsErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on MySQL" );
		}
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( DdlTypeHelper.getTypeName( definition.type(), walker.getSessionFactory().getTypeConfiguration() ) );

		sqlAppender.appendSql( " exists" );
		if ( definition.jsonPath() != null ) {
			sqlAppender.appendSql( " path " );
			sqlAppender.appendSingleQuoteEscapedString( definition.jsonPath() );
		}
		else {
			sqlAppender.appendSql( " path '$." );
			sqlAppender.appendSql( definition.name() );
			sqlAppender.appendSql( "'" );
		}
	}
}
