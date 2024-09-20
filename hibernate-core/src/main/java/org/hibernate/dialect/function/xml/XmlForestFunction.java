/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmNamedExpression;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;

/**
 * Standard XmlForestFunction function.
 */
public class XmlForestFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public XmlForestFunction(TypeConfiguration typeConfiguration) {
		super(
				"xmlforest",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.min( 1 ),
						new ArgumentsValidator() {
							@Override
							public void validate(
									List<? extends SqmTypedNode<?>> arguments,
									String functionName,
									TypeConfiguration typeConfiguration) {
								for ( int i = 0; i < arguments.size(); i++ ) {
									SqmTypedNode<?> argument = arguments.get( i );
									if ( !( argument instanceof SqmNamedExpression<?> namedExpression ) ) {
										throw new FunctionArgumentException(
												String.format(
														"Parameter %d of function 'xmlforest()' is not named",
														i
												)
										);
									}
									if ( !isValidXmlName( namedExpression.getName() ) ) {
										throw new FunctionArgumentException(
												String.format(
														"Invalid XML element name passed to 'xmlforest()': %s",
														namedExpression.getName()
												)
										);
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
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmlforest" );
		char separator = '(';
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			sqlAppender.appendSql( separator );
			if ( sqlAstArgument instanceof AliasedExpression aliasedExpression ) {
				aliasedExpression.getExpression().accept( walker );
				sqlAppender.appendSql( " as " );
				sqlAppender.appendDoubleQuoteEscapedString( aliasedExpression.getAlias() );
			}
			else {
				sqlAstArgument.accept( walker );
			}
			separator = ',';
		}
		sqlAppender.appendSql( ')' );
	}
}
