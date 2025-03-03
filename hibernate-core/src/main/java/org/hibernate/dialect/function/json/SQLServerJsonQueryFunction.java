/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_query function.
 */
public class SQLServerJsonQueryFunction extends JsonQueryFunction {

	public SQLServerJsonQueryFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// openjson errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonQueryErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on SQL server" );
		}
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements(
				walker.getLiteralValue( arguments.jsonPath() )
		);
		if ( arguments.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_ARRAY
				|| arguments.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
			sqlAppender.appendSql( "coalesce(" );
		}
		render( sqlAppender, arguments, jsonPathElements, jsonPathElements.size() - 1, walker );
		if ( arguments.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_ARRAY ) {
			sqlAppender.appendSql( ",'[]')" );
		}
		else if ( arguments.emptyBehavior() == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
			sqlAppender.appendSql( ",'{}')" );
		}
	}

	private void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			List<JsonPathHelper.JsonPathElement> jsonPathElements,
			int index,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "(select " );
		final boolean aggregate = index == jsonPathElements.size() - 1 && (
				arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER
						|| arguments.wrapMode() == JsonQueryWrapMode.WITH_CONDITIONAL_WRAPPER
		);
		if ( aggregate ) {
			if ( arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
				sqlAppender.appendSql( "'['+" );
			}
			else {
				sqlAppender.appendSql( "case when count(*)>1 then '[' else '' end+" );
			}
			sqlAppender.appendSql( "string_agg(t.v,',')" );
			if ( arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
				sqlAppender.appendSql( "+']'" );
			}
			else {
				sqlAppender.appendSql( "+case when count(*)>1 then ']' else '' end" );
			}

			// openjson unquotes values, so we have to quote them again
			sqlAppender.appendSql( " from (select " );
			// type 0 is a null literal
			sqlAppender.appendSql( "case t.type when 0 then 'null' when 1 then ");
			// type 1 is a string literal. to quote it, we use for json path and trim the string down to just the value
			sqlAppender.appendSql(
					"(select substring(a.v,6,len(a.v)-6) from (select t.value a for json path,without_array_wrapper) a(v))" );
			sqlAppender.appendSql( " else t.value end v");

		}
		else {
			sqlAppender.appendSql( "t.value" );
		}
		sqlAppender.appendSql( " from openjson(" );
		if ( index == 0 ) {
			arguments.jsonDocument().accept( walker );
		}
		else {
			render( sqlAppender, arguments, jsonPathElements, index - 1, walker );
		}
		sqlAppender.appendSql( ')' );
		if ( arguments.emptyBehavior() == JsonQueryEmptyBehavior.ERROR ) {
			sqlAppender.appendSql( " with (value nvarchar(max) " );
			final JsonPathHelper.JsonPathElement jsonPathElement = jsonPathElements.get( index );
			if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSql( "'strict $." );
				for ( int i = 0; i < attribute.attribute().length(); i++ ) {
					final char c = attribute.attribute().charAt( i );
					if ( c == '\'' ) {
						sqlAppender.append( '\'' );
					}
					sqlAppender.append( c );
				}
				sqlAppender.append( '\'' );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonIndexAccess indexAccess ) {
				sqlAppender.appendSql( "'strict $[" );
				sqlAppender.appendSql( indexAccess.index() );
				sqlAppender.appendSql( "]'" );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonParameterIndexAccess indexAccess ) {
				final JsonPathPassingClause passingClause = arguments.passingClause();
				assert passingClause != null;
				final Object literalValue = walker.getLiteralValue(
						passingClause.getPassingExpressions().get( indexAccess.parameterName() )
				);
				sqlAppender.appendSql( "'strict $[" );
				sqlAppender.appendSql( literalValue.toString() );
				sqlAppender.appendSql( "]'" );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported JSON path expression: " + jsonPathElement );
			}
			sqlAppender.appendSql( " as json) t" );
		}
		else {
			sqlAppender.appendSql( " t where " );
			final JsonPathHelper.JsonPathElement jsonPathElement = jsonPathElements.get( index );
			if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSql( "t.[key]=" );
				sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonIndexAccess indexAccess ) {
				sqlAppender.appendSql( "t.[key]=" );
				sqlAppender.appendSql( indexAccess.index() );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonParameterIndexAccess indexAccess ) {
				final JsonPathPassingClause passingClause = arguments.passingClause();
				assert passingClause != null;
				sqlAppender.appendSql( "t.[key]=" );
				passingClause.getPassingExpressions().get( indexAccess.parameterName() ).accept( walker );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported JSON path expression: " + jsonPathElement );
			}
		}
		if ( aggregate ) {
			sqlAppender.appendSql( ") t" );
		}
		sqlAppender.appendSql( ")" );
	}
}
