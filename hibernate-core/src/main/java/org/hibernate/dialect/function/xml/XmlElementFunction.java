/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmXmlAttributesExpression;
import org.hibernate.query.sqm.tree.expression.SqmXmlElementExpression;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.XmlAttributes;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Standard xmlelement function.
 */
public class XmlElementFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public XmlElementFunction(TypeConfiguration typeConfiguration) {
		super(
				"xmlelement",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( StandardArgumentsValidators.min( 1 ), STRING ),
						new ArgumentsValidator() {
							@Override
							public void validate(
									List<? extends SqmTypedNode<?>> arguments,
									String functionName,
									BindingContext bindingContext) {
								//noinspection unchecked
								final var literal = (SqmLiteral<String>) arguments.get( 0 );
								final String elementName = literal.getLiteralValue();
								if ( !XmlHelper.isValidXmlName( elementName ) ) {
									throw new FunctionArgumentException(
											String.format(
													"Invalid XML element name passed to 'xmlelement()': %s",
													elementName
											)
									);
								}
								if ( arguments.size() > 1
										&& arguments.get( 1 ) instanceof SqmXmlAttributesExpression attributesExpression ) {
									for ( var entry : attributesExpression.getAttributes().entrySet() ) {
										if ( !XmlHelper.isValidXmlName( entry.getKey() ) ) {
											throw new FunctionArgumentException(
													String.format(
															"Invalid XML attribute name passed to 'xmlattributes()': %s",
															entry.getKey()
													)
											);
										}
									}
								}
							}
						}
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML )
				),
				null
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		//noinspection unchecked
		return (SelfRenderingSqmFunction<T>) new SqmXmlElementExpression(
				this,
				this,
				arguments,
				(ReturnableType<String>) impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, XmlElementArguments.extract( sqlAstArguments ), returnType, walker );
	}

	protected void render(
			SqlAppender sqlAppender,
			XmlElementArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmlelement(name " );
		sqlAppender.appendDoubleQuoteEscapedString( arguments.elementName() );
		if ( arguments.attributes() != null ) {
			sqlAppender.appendSql( ",xmlattributes" );
			char separator = '(';
			for ( var entry : arguments.attributes().getAttributes().entrySet() ) {
				sqlAppender.appendSql( separator );
				entry.getValue().accept( walker );
				sqlAppender.appendSql( " as " );
				sqlAppender.appendDoubleQuoteEscapedString( entry.getKey() );
				separator = ',';
			}
			sqlAppender.appendSql( ')' );
		}
		if ( !arguments.content().isEmpty() ) {
			for ( Expression expression : arguments.content() ) {
				sqlAppender.appendSql( ',' );
				expression.accept( walker );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	protected record XmlElementArguments(
			String elementName,
			@Nullable XmlAttributes attributes,
			List<Expression> content) {
		static XmlElementArguments extract(List<? extends SqlAstNode> arguments) {
			final var elementName = (Literal) arguments.get( 0 );
			final XmlAttributes attributes;
			final List<Expression> content;
			int index = 1;
			if ( arguments.size() > index
					&& arguments.get( index ) instanceof XmlAttributes xmlAttributes ) {
				attributes = xmlAttributes;
				index++;
			}
			else {
				attributes = null;
			}
			//noinspection unchecked
			content = (List<Expression>) arguments.subList( index, arguments.size() );
			return new XmlElementArguments( (String) elementName.getLiteralValue(), attributes, content );
		}
	}
}
