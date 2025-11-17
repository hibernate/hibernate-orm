/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ENUM;


/**
 * The HQL {@code ordinal()} function returns the ordinal value of an enum
 * <p>
 * For enum fields mapped as ORDINAL it's a synonym for {@code cast(x as Integer)}. Same as {@link CastStrEmulation} but for Integer.
 * For enum fields mapped as STRING or ENUM it's a case statement that returns the ordinal value.
 *
 * @author Luca Molteni
 */
public class OrdinalFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	public OrdinalFunction(TypeConfiguration typeConfiguration) {
		super(
				"ordinal",
				new ArgumentTypesValidator( null, ENUM ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				),
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		Expression singleExpression = (Expression) arguments.get( 0 );

		JdbcMapping singleJdbcMapping = singleExpression.getExpressionType().getSingleJdbcMapping();
		JdbcType argumentType = singleJdbcMapping.getJdbcType();

		if ( argumentType.isInteger() ) {
			singleExpression.accept( walker );
		}
		else if ( argumentType.isString() || argumentType.getDefaultSqlTypeCode() == SqlTypes.ENUM ) {

			EnumJavaType<?> enumJavaType = (EnumJavaType<?>) singleJdbcMapping.getMappedJavaType();
			Object[] enumConstants = enumJavaType.getJavaTypeClass().getEnumConstants();

			sqlAppender.appendSql( "case " );
			singleExpression.accept( walker );
			for ( Object e : enumConstants ) {
				Enum<?> enumValue = (Enum<?>) e;
				sqlAppender.appendSql( " when " );
				sqlAppender.appendSingleQuoteEscapedString( (String) singleJdbcMapping.convertToRelationalValue(
						enumValue.toString() ) );
				sqlAppender.appendSql( " then " );
				sqlAppender.appendSql( enumValue.ordinal() );
			}
			sqlAppender.appendSql( " end" );
		}
		else {
			throw new QueryException( "Unsupported enum type passed to 'ordinal()' function: " + argumentType );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(ENUM arg)";
	}
}
