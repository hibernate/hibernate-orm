/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;
import java.util.Map;

import org.hibernate.query.ReturnableType;
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
import org.hibernate.query.sqm.tree.expression.SqmExpression;
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

import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
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
									TypeConfiguration typeConfiguration) {
								//noinspection unchecked
								final String elementName = ( (SqmLiteral<String>) arguments.get( 0 ) ).getLiteralValue();
								if ( !isValidXmlName( elementName ) ) {
									throw new FunctionArgumentException(
											String.format(
													"Invalid XML element name passed to 'xmlelement()': %s",
													elementName
											)
									);
								}
								if ( arguments.size() > 1
										&& arguments.get( 1 ) instanceof SqmXmlAttributesExpression attributesExpression ) {
									final Map<String, SqmExpression<?>> attributes = attributesExpression.getAttributes();
									for ( Map.Entry<String, SqmExpression<?>> entry : attributes.entrySet() ) {
										if ( !isValidXmlName( entry.getKey() ) ) {
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

							private static boolean isValidXmlName(String name) {
								if ( name.isEmpty()
										|| !isValidXmlNameStart( name.charAt( 0 ) )
										|| name.regionMatches( true, 0, "xml", 0, 3 ) ) {
									return false;
								}
								for ( int i = 1; i < name.length(); i++ ) {
									if ( !isValidXmlNameChar( name.charAt( i ) ) ) {
										return false;
									}
								}
								return true;
							}

							private static boolean isValidXmlNameStart(char c) {
								return isLetter( c ) || c == '_' || c == ':';
							}

							private static boolean isValidXmlNameChar(char c) {
								return isLetterOrDigit( c ) || c == '_' || c == ':' || c == '-' || c == '.';
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
			for ( Map.Entry<String, Expression> entry : arguments.attributes().getAttributes().entrySet() ) {
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
			final Literal elementName = (Literal) arguments.get( 0 );
			final XmlAttributes attributes;
			final List<Expression> content;

			int index = 1;
			if ( arguments.size() > index && arguments.get( index ) instanceof XmlAttributes ) {
				attributes = (XmlAttributes) arguments.get( index );
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
