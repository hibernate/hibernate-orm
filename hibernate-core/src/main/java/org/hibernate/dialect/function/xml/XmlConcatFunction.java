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
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.XML;

/**
 * Standard xmlconcat function.
 */
public class XmlConcatFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public XmlConcatFunction(TypeConfiguration typeConfiguration) {
		super(
				"xmlconcat",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( StandardArgumentsValidators.min( 2 ), XML )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML )
				),
				StandardFunctionArgumentTypeResolvers.impliedOrInvariant( typeConfiguration, XML )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmlconcat" );
		char separator = '(';
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			sqlAppender.appendSql( separator );
			sqlAstArgument.accept( walker );
			separator = ',';
		}
		sqlAppender.appendSql( ')' );
	}
}
