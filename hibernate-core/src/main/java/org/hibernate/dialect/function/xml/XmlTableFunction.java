/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmXmlTableFunction;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.XmlTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableValueColumnDefinition;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.XML;

/**
 * Standard xmltable function.
 */
public class XmlTableFunction extends AbstractSqmSelfRenderingSetReturningFunctionDescriptor {

	protected final boolean supportsParametersInDefault;

	public XmlTableFunction(boolean supportsParametersInDefault, TypeConfiguration typeConfiguration) {
		this(
				supportsParametersInDefault,
				new XmlTableSetReturningFunctionTypeResolver(),
				typeConfiguration
		);
	}

	protected XmlTableFunction(boolean supportsParametersInDefault, SetReturningFunctionTypeResolver setReturningFunctionTypeResolver, TypeConfiguration typeConfiguration) {
		super(
				"xmltable",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 2 ),
						FunctionParameterType.STRING,
						FunctionParameterType.IMPLICIT_XML
				),
				setReturningFunctionTypeResolver,
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, XML )
		);
		this.supportsParametersInDefault = supportsParametersInDefault;
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(List<? extends SqmTypedNode<?>> arguments, QueryEngine queryEngine) {
		//noinspection unchecked
		return new SqmXmlTableFunction<>(
				this,
				this,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				(SqmExpression<String>) arguments.get( 0 ),
				(SqmExpression<?>) arguments.get( 1 )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		renderXmlTable( sqlAppender, XmlTableArguments.extract( sqlAstArguments ), tupleType, tableIdentifierVariable, walker );
	}

	protected void renderXmlTable(
			SqlAppender sqlAppender,
			XmlTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmltable(" );
		arguments.xpath().accept( walker );
		sqlAppender.appendSql( " passing " );
		if ( !arguments.isXmlType() ) {
			sqlAppender.appendSql( "xmlparse(document " );
		}
		arguments.xmlDocument().accept( walker );
		if ( !arguments.isXmlType() ) {
			sqlAppender.appendSql( ')' );
		}
		renderColumns( sqlAppender, arguments.columnsClause(), walker );
		sqlAppender.appendSql( ')' );
	}

	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		return determineColumnType( castTarget, walker.getSessionFactory().getTypeConfiguration() );
	}

	protected static String determineColumnType(CastTarget castTarget, TypeConfiguration typeConfiguration) {
		final String columnDefinition = castTarget.getColumnDefinition();
		if ( columnDefinition != null ) {
			return columnDefinition;
		}
		else {
			final String typeName = DdlTypeHelper.getTypeName(
					castTarget.getJdbcMapping(),
					castTarget.toSize(),
					typeConfiguration
			);
			final int parenthesisIndex = typeName.indexOf( '(' );
			if ( parenthesisIndex != -1 && typeName.charAt( parenthesisIndex + 1 ) == '$' ) {
				// Remove length/precision and scale arguments if it contains unresolved variables
				return typeName.substring( 0, parenthesisIndex );
			}
			else {
				return typeName;
			}
		}
	}

	protected void renderColumns(SqlAppender sqlAppender, XmlTableColumnsClause xmlTableColumnsClause, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( " columns" );
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

	protected void renderXmlOrdinalityColumnDefinition(SqlAppender sqlAppender, XmlTableOrdinalityColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " for ordinality" );
	}

	protected void renderXmlValueColumnDefinition(SqlAppender sqlAppender, XmlTableValueColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( definition.type(), walker ) );

		renderColumnPath( definition.name(), definition.xpath(), sqlAppender, walker );
		renderDefaultExpression( definition.defaultExpression(), sqlAppender, walker );
	}

	protected void renderColumnPath(String name, @Nullable String xpath, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		if ( xpath != null ) {
			sqlAppender.appendSql( " path " );
			sqlAppender.appendSingleQuoteEscapedString( xpath );
		}
		else {
			// To avoid case sensitivity issues, just pass the path always
			sqlAppender.appendSql( " path " );
			sqlAppender.appendSingleQuoteEscapedString( name );
		}
	}

	protected void renderDefaultExpression(@Nullable Expression expression, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		if ( expression != null ) {
			sqlAppender.appendSql( " default " );
			if ( supportsParametersInDefault ) {
				expression.accept( walker );
			}
			else {
				walker.render( expression, SqlAstNodeRenderingMode.INLINE_PARAMETERS );
			}
		}
	}

	protected void renderXmlQueryColumnDefinition(SqlAppender sqlAppender, XmlTableQueryColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( new CastTarget( definition.type() ), walker ) );

		renderColumnPath( definition.name(), definition.xpath(), sqlAppender, walker );
		renderDefaultExpression( definition.defaultExpression(), sqlAppender, walker );
	}

	protected record XmlTableArguments(
			Expression xpath,
			Expression xmlDocument,
			boolean isXmlType,
			XmlTableColumnsClause columnsClause
	){
		public static XmlTableArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			final Expression xpath = (Expression) sqlAstArguments.get( 0 );
			final Expression xmlDocument = (Expression) sqlAstArguments.get( 1 );
			XmlTableColumnsClause columnsClause = null;
			int nextIndex = 2;
			if ( nextIndex < sqlAstArguments.size() ) {
				if ( sqlAstArguments.get( nextIndex ) instanceof XmlTableColumnsClause tableColumnsClause ) {
					columnsClause = tableColumnsClause;
				}
			}
			return new XmlTableArguments(
					xpath,
					xmlDocument,
					xmlDocument.getExpressionType() != null
							&& xmlDocument.getExpressionType().getSingleJdbcMapping().getJdbcType().isXml(),
					columnsClause
			);
		}
	}
}
