/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.XmlTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableValueColumnDefinition;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server xmltable function.
 */
public class SQLServerXmlTableFunction extends XmlTableFunction {

	public SQLServerXmlTableFunction(TypeConfiguration typeConfiguration) {
		super( false, typeConfiguration );
	}

	@Override
	protected void renderXmlTable(SqlAppender sqlAppender, XmlTableArguments arguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "(select" );
		renderColumns( sqlAppender, arguments.columnsClause(), walker );
		sqlAppender.appendSql( " from (select " );
		if ( !arguments.isXmlType() ) {
			sqlAppender.appendSql( "cast(" );
		}
		arguments.xmlDocument().accept( walker );
		if ( !arguments.isXmlType() ) {
			sqlAppender.appendSql( " as xml)" );
		}
		sqlAppender.appendSql( ") t0_(d) cross apply t0_.d.nodes(" );
		walker.render( arguments.xpath(), SqlAstNodeRenderingMode.INLINE_PARAMETERS );
		sqlAppender.appendSql( ") t1_(d))" );
	}

	@Override
	protected void renderColumns(SqlAppender sqlAppender, XmlTableColumnsClause xmlTableColumnsClause, SqlAstTranslator<?> walker) {
		char separator = ' ';
		for ( XmlTableColumnDefinition columnDefinition : xmlTableColumnsClause.getColumnDefinitions() ) {
			sqlAppender.appendSql( separator );
			if ( columnDefinition instanceof XmlTableQueryColumnDefinition definition ) {
				renderXmlQueryColumnDefinition( sqlAppender, definition, walker );
			}
			else if ( columnDefinition instanceof XmlTableValueColumnDefinition definition ) {
				renderXmlValueColumnDefinition( sqlAppender, definition, walker );
			}
			else {
				renderXmlOrdinalityColumnDefinition(
						sqlAppender,
						(XmlTableOrdinalityColumnDefinition) columnDefinition,
						walker
				);
			}
			separator = ',';
		}
	}

	@Override
	protected void renderXmlOrdinalityColumnDefinition(SqlAppender sqlAppender, XmlTableOrdinalityColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "row_number() over (order by (select 1)) " );
		sqlAppender.appendSql( definition.name() );
	}

	@Override
	protected void renderXmlValueColumnDefinition(SqlAppender sqlAppender, XmlTableValueColumnDefinition definition, SqlAstTranslator<?> walker) {
		if ( definition.defaultExpression() != null ) {
			sqlAppender.appendSql( "coalesce(" );
		}
		sqlAppender.appendSql( "t1_.d.value('(" );
		sqlAppender.appendSql( definition.xpath() == null ? definition.name() : definition.xpath() );
		sqlAppender.appendSql( ")[1]'," );
		sqlAppender.appendSingleQuoteEscapedString( determineColumnType( definition.type(), walker ) );
		sqlAppender.appendSql( ')' );

		if ( definition.defaultExpression() != null ) {
			sqlAppender.appendSql( ',' );
			definition.defaultExpression().accept( walker );
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}

	@Override
	protected void renderXmlQueryColumnDefinition(SqlAppender sqlAppender, XmlTableQueryColumnDefinition definition, SqlAstTranslator<?> walker) {
		if ( definition.defaultExpression() != null ) {
			sqlAppender.appendSql( "coalesce(" );
		}
		sqlAppender.appendSql( "t1_.d.query('(" );
		sqlAppender.appendSql( definition.xpath() == null ? definition.name() : definition.xpath() );
		sqlAppender.appendSql( ")[1]')" );

		if ( definition.defaultExpression() != null ) {
			sqlAppender.appendSql( ',' );
			definition.defaultExpression().accept( walker );
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( definition.name() );
	}
}
