/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.type.BindingContext;
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

/**
 * Standard xmlforest function.
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
									BindingContext bindingContext) {
								for ( int i = 0; i < arguments.size(); i++ ) {
									if ( !( arguments.get( i ) instanceof SqmNamedExpression<?> namedExpression ) ) {
										throw new FunctionArgumentException(
												String.format(
														"Parameter %d of function 'xmlforest()' is not named",
														i
												)
										);
									}
									if ( !XmlHelper.isValidXmlName( namedExpression.getName() ) ) {
										throw new FunctionArgumentException(
												String.format(
														"Invalid XML element name passed to 'xmlforest()': %s",
														namedExpression.getName()
												)
										);
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
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmlforest" );
		char separator = '(';
		for ( var sqlAstArgument : sqlAstArguments ) {
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
