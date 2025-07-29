/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

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
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * @author Christian Beikov
 */
public class ArrayToStringFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public ArrayToStringFunction(TypeConfiguration typeConfiguration) {
		super(
				"array_to_string",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
					new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 3 ), ANY, STRING, STRING )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.composite(
						new ArrayAndElementArgumentTypeResolver( 0, 2 ),
						StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, ANY, STRING, STRING )
				)
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression separatorExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression defaultExpression = sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) arrayExpression.getExpressionType().getSingleJdbcMapping();
		final int ddlTypeCode = pluralType.getElementType().getJdbcType().getDdlTypeCode();
		if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
			// For some reason, PostgreSQL turns true/false to t/f in this function, so unnest this manually
			sqlAppender.append( "case when " );
			arrayExpression.accept( walker );
			sqlAppender.append( " is not null then coalesce((select string_agg(" );
			if ( defaultExpression != null ) {
				sqlAppender.append( "coalesce(" );
			}
			sqlAppender.append( "cast(t.v as varchar)" );
			if ( defaultExpression != null ) {
				sqlAppender.append( "," );
				defaultExpression.accept( walker );
				sqlAppender.append( ")" );
			}
			sqlAppender.appendSql( ',' );
			separatorExpression.accept( walker );
			sqlAppender.append( " order by t.i) from unnest(");
			arrayExpression.accept( walker );
			sqlAppender.append(") with ordinality t(v,i)),'') end" );
		}
		else {
			sqlAppender.appendSql( "array_to_string(" );
			arrayExpression.accept( walker );
			sqlAppender.appendSql( ',' );
			separatorExpression.accept( walker );
			if ( defaultExpression != null ) {
				sqlAppender.appendSql( ',' );
				defaultExpression.accept( walker );
			}
			sqlAppender.appendSql( ')' );
		}
	}

}
