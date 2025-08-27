/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.List;

import org.hibernate.dialect.function.json.ExpressionTypeHelper;
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
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.IMPLICIT_XML;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.XML;

/**
 * Standard xmlquery function.
 */
public class XmlQueryFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean returningContent;

	public XmlQueryFunction(boolean returningContent, TypeConfiguration typeConfiguration) {
		super(
				"xmlquery",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( null, STRING, IMPLICIT_XML )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, XML )
		);
		this.returningContent = returningContent;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression xmlDocument = (Expression) sqlAstArguments.get( 1 );
		final boolean needsCast = !ExpressionTypeHelper.isXml( xmlDocument );
		sqlAppender.appendSql( "xmlquery(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( " passing " );
		if ( needsCast ) {
			sqlAppender.appendSql( "xmlparse(document " );
		}
		sqlAstArguments.get( 1 ).accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( ')' );
		}
		if ( returningContent ) {
			sqlAppender.appendSql( " returning content" );
		}
		sqlAppender.appendSql( ')' );
	}
}
