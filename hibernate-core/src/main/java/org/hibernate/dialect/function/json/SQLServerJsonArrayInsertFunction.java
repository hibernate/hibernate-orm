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
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_array_insert function.
 */
public class SQLServerJsonArrayInsertFunction extends AbstractJsonArrayInsertFunction {

	public SQLServerJsonArrayInsertFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final String jsonPath = translator.<String>getLiteralValue( (Expression) arguments.get( 1 ) ).trim();
		final int bracketEndIndex = jsonPath.lastIndexOf( ']' );
		final int bracketStartIndex = jsonPath.lastIndexOf( '[' );
		if ( jsonPath.isEmpty()
				|| bracketEndIndex != jsonPath.length() - 1
				|| bracketStartIndex == -1 ) {
			throw new QueryException( "JSON path does not end with an array index: " + jsonPath );
		}
		final int index;
		try {
			index = Integer.parseInt( jsonPath.substring( bracketStartIndex + 1, bracketEndIndex ) );
		}
		catch ( NumberFormatException e ) {
			throw new QueryException( "JSON path does not point to a valid array index: " + jsonPath );
		}
		final Expression json = (Expression) arguments.get( 0 );
		final SqlAstNode value = arguments.get( 2 );
		// Only replace data if this is an array
		sqlAppender.appendSql( "(select case when left(json_query(x.d,x.p),1)='[' then " );
		// Replace the array
		sqlAppender.appendSql( "json_modify(x.d,x.p,json_query((" );
		// Aggregate a new JSON array based on element rows
		sqlAppender.appendSql( "select '['+string_agg(t.v,',') within group (order by t.k)+']' from (" );

		sqlAppender.appendSql( "select x.i k,x.v v union all " );
		sqlAppender.appendSql( "select case when cast(t.[key] as int)>=x.i then cast(t.[key] as int)+1 " );
		sqlAppender.appendSql( "else cast(t.[key] as int) end," );
		// type 0 is a null literal
		sqlAppender.appendSql( "case t.type when 0 then 'null' when 1 then ");
		// type 1 is a string literal. to quote it, we use for json path and trim the string down to just the value
		sqlAppender.appendSql(
				"(select substring(a.v,6,len(a.v)-6) from (select t.value a for json path,without_array_wrapper) a(v))" );
		sqlAppender.appendSql( " else t.value end from openjson(x.d,x.p) t) t))) " );
		sqlAppender.appendSql( " else x.d end " );
		// Push args into a values clause since we are going to refer to them multiple times
		sqlAppender.appendSql( "from (values(" );
		json.accept( translator );
		sqlAppender.append( ',' );
		sqlAppender.appendSingleQuoteEscapedString( jsonPath.substring( 0, bracketStartIndex ) );
		sqlAppender.append( ',' );
		sqlAppender.appendSql( index );
		sqlAppender.append( ',' );
		value.accept( translator );
		sqlAppender.append( ")) x(d,p,i,v))" );
	}

	protected void renderArgument(
			SqlAppender sqlAppender,
			SqlAstNode arg,
			SqlAstTranslator<?> translator) {
		sqlAppender.appendSql( "substring(json_modify('[]','append $'," );
		arg.accept( translator );
		sqlAppender.appendSql( "),2,len(json_modify('[]','append $'," );
		arg.accept( translator );
		sqlAppender.appendSql( "))-2)" );
	}
}
