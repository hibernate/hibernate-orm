/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.dialect.function.json.JsonValueFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_value function.
 */
public class SingleStoreJsonValueFunction extends JsonValueFunction {

	public SingleStoreJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.NULL ) {
			throw new QueryException( "Can't emulate on error clause on SingleStore" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on SingleStore" );
		}
		if ( arguments.returningType() != null ) {
			if ( arguments.returningType().getJdbcMapping().getJdbcType().isBoolean() ) {
				sqlAppender.append( "case " );
			}
			else {
				sqlAppender.append( "cast(" );
			}
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "SingleStore json_value only support literal json paths, but got " + arguments.jsonPath() );
		}
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
		sqlAppender.appendSql( "json_extract_string(" );
		arguments.jsonDocument().accept( walker );
		for ( JsonPathHelper.JsonPathElement pathElement : jsonPathElements ) {
			sqlAppender.appendSql( ',' );
			if ( pathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
			}
			else if ( pathElement instanceof JsonPathHelper.JsonParameterIndexAccess indexParameter) {
				final String parameterName = indexParameter.parameterName();
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			else {
				sqlAppender.appendSql( '\'' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) pathElement ).index() );
				sqlAppender.appendSql( '\'' );
			}
		}
		sqlAppender.appendSql( ')' );
		if ( arguments.returningType() != null ) {
			if ( arguments.returningType().getJdbcMapping().getJdbcType().isBoolean() ) {
				sqlAppender.append( " when 'true' then true when 'false' then false end " );
			}
			else {
				sqlAppender.appendSql( " as " );
				arguments.returningType().accept( walker );
				sqlAppender.appendSql( ')' );
			}
		}
	}
}
