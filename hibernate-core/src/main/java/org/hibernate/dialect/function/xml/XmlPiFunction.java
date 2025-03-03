/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Standard xmlpi function.
 */
public class XmlPiFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public XmlPiFunction(TypeConfiguration typeConfiguration) {
		super(
				"xmlpi",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( StandardArgumentsValidators.between( 1, 2 ), STRING, STRING )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, ANY, STRING )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmlpi(name " );
		final Literal literal = (Literal) sqlAstArguments.get( 0 );
		sqlAppender.appendDoubleQuoteEscapedString( (String) literal.getLiteralValue() );
		if ( sqlAstArguments.size() > 1 ) {
			sqlAppender.appendSql( ',' );
			sqlAstArguments.get( 1 ).accept( walker );
		}
		sqlAppender.appendSql( ')' );
	}
}
