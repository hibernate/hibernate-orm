/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL json_mergepatch function.
 */
public class PostgreSQLJsonMergepatchFunction extends AbstractJsonMergepatchFunction {

	public PostgreSQLJsonMergepatchFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		// Introduce a CTE named "args" which will provide easy access to the arguments in the following
		sqlAppender.appendSql( "(with recursive args" );
		char separator = '(';
		for ( int i = 0; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( separator );
			sqlAppender.appendSql( 'd' );
			sqlAppender.appendSql( i );
			separator = ',';
		}

		sqlAppender.appendSql( ") as(select" );
		separator = ' ';
		for ( int i = 0; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( separator );
			renderJsonDocumentExpression( sqlAppender, translator, (Expression) arguments.get( i ) );
			separator = ',';
		}
		sqlAppender.appendSql( ")," );
		// Render CTEs that explode JSON into key-value pairs for each parent prefix
		for ( int i = 0; i < arguments.size(); i++ ) {
			renderKeyValueCte( "val" + i, "d" + i, sqlAppender);
		}
		// Compute the resulting JSON recursively
		sqlAppender.appendSql( "res(v,p,l) as(" );
		sqlAppender.appendSql( "select" );
		// Aggregate key-value pairs, preferring the last value
		sqlAppender.appendSql( " jsonb_object_agg(coalesce(" );
		renderColumnList(sqlAppender, "k", arguments.size());
		sqlAppender.appendSql( "),coalesce(" );
		renderColumnList(sqlAppender, "v", arguments.size());
		sqlAppender.appendSql( "))" );
		// The parent path
		sqlAppender.appendSql( ",coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( ")" );
		// The level within the object tree
		sqlAppender.appendSql( ",cardinality(coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( "))" );
		// Full join the two key-value pair tables based on parent prefix and key
		sqlAppender.appendSql( " from val0 v0" );
		for ( int i = 1; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( " full join val" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( " v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( " on v0.p=v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( ".p and v0.k=v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( ".k" );
		}
		// start at the bottom
		sqlAppender.appendSql( " where cardinality(coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( "))=" );
		sqlAppender.appendSql( "(select cardinality(v.p) from val0 v" );
		for ( int i = 1; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( " union select cardinality(v.p) from val" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( " v" );
		}
		sqlAppender.appendSql( " order by 1 desc limit 1)" );
		// filter rows where the new value is a json null i.e. should be removed
		sqlAppender.appendSql( " and jsonb_typeof(coalesce(" );
		renderColumnList(sqlAppender, "v", arguments.size(), 1);
		sqlAppender.appendSql( ")) is distinct from 'null'" );
		sqlAppender.appendSql( " group by" );
		sqlAppender.appendSql( " coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( ")" );
		sqlAppender.appendSql( ",cardinality(coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( "))" );

		sqlAppender.appendSql( " union all " );

		sqlAppender.appendSql( "select" );
		// Use strict aggregation to ensure a SQL null does not end up as JSON null in the result
		sqlAppender.appendSql( " jsonb_object_agg(coalesce(" );
		renderColumnList(sqlAppender, "k", arguments.size());
		sqlAppender.appendSql( "),coalesce(case when coalesce(" );
		renderColumnList(sqlAppender, "k", arguments.size());
		sqlAppender.appendSql( ")=r.p[cardinality(r.p)] then r.v end," );
		renderColumnList(sqlAppender, "v", arguments.size());
		sqlAppender.appendSql( ")) filter (where coalesce(case when coalesce(" );
		renderColumnList(sqlAppender, "k", arguments.size());
		sqlAppender.appendSql( ")=r.p[cardinality(r.p)] then r.v end," );
		renderColumnList(sqlAppender, "v", arguments.size());
		sqlAppender.appendSql( ") is not null)" );
		// The parent path
		sqlAppender.appendSql( ",coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( ")" );
		// The level within the object tree
		sqlAppender.appendSql( ",r.l-1" );
		// Full join the two key-value pair tables based on parent prefix and key
		sqlAppender.appendSql( " from val0 v0" );
		for ( int i = 1; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( " full join val" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( " v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( " on v0.p=v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( ".p and v0.k=v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( ".k" );
		}
		// Recurse against the previously processed rows with lowest "level" to walk up the tree
		sqlAppender.appendSql( " join (select * from res r order by r.l fetch first 1 rows with ties) r" );
		sqlAppender.appendSql( " on cardinality(coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( "))=r.l-1" );
		// filter rows where the new value is a json null i.e. should be removed
		sqlAppender.appendSql( " and jsonb_typeof(coalesce(" );
		renderColumnList(sqlAppender, "v", arguments.size(), 1);
		sqlAppender.appendSql( ")) is distinct from 'null'" );
		// Stop at the last/root level
		sqlAppender.appendSql( " and r.l<>0" );
		sqlAppender.appendSql( " group by" );
		sqlAppender.appendSql( " coalesce(" );
		renderColumnList(sqlAppender, "p", arguments.size());
		sqlAppender.appendSql( ")" );
		sqlAppender.appendSql( ",r.l-1" );
		sqlAppender.appendSql( ") " );
		// Select the last/root level object
		sqlAppender.appendSql( "select r.v from res r where r.l=0)" );
	}

	private void renderColumnList(SqlAppender sqlAppender, String column, int size) {
		renderColumnList( sqlAppender, column, size, 0 );
	}

	private void renderColumnList(SqlAppender sqlAppender, String column, int size, int end) {
		sqlAppender.appendSql( "v" );
		sqlAppender.appendSql( size - 1 );
		sqlAppender.appendSql( '.' );
		sqlAppender.appendSql( column );
		for ( int i = size - 2; i >= end; i-- ) {
			sqlAppender.appendSql( ",v" );
			sqlAppender.appendSql( i );
			sqlAppender.appendSql( '.' );
			sqlAppender.appendSql( column );
		}
	}

	private void renderKeyValueCte(String cteName, String columnName, SqlAppender sqlAppender) {
		sqlAppender.appendSql( cteName );
		sqlAppender.appendSql( "(p,k,v) as (");
		sqlAppender.appendSql( "select '{}'::text[],s.k,t." );
		sqlAppender.appendSql( columnName );
		sqlAppender.appendSql( "->s.k from args t join lateral jsonb_object_keys(t." );
		sqlAppender.appendSql( columnName );
		sqlAppender.appendSql( ") s(k) on 1=1 union " );
		sqlAppender.appendSql( "select v.p||v.k,s.k,v.v->s.k from " );
		sqlAppender.appendSql( cteName );
		sqlAppender.appendSql( " v" );
		sqlAppender.appendSql( " join lateral jsonb_object_keys(v.v) s(k)" );
		sqlAppender.appendSql( " on jsonb_typeof(v.v)='object'" );
		sqlAppender.appendSql( ")," );
	}

	private void renderJsonDocumentExpression(SqlAppender sqlAppender, SqlAstTranslator<?> translator, Expression json) {
		final boolean needsCast = !isJsonType( json );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		json.accept( translator );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
	}

	private boolean isJsonType(Expression expression) {
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		return expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson();
	}
}
