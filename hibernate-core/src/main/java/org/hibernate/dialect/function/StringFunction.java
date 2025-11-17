/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

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

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ENUM;


/**
 * The HQL {@code string()} function returns the string value of an enum
 * <p>
 * For enum fields mapped as STRING or ENUM it's a synonym for {@code cast(x as String)}. Same as {@link CastStrEmulation}.
 * For enum fields mapped as ORDINAL it's a case statement that returns the bane if enum.
 *
 * @author Luca Molteni, Cedomir Igaly
 */
public class StringFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	public StringFunction(TypeConfiguration typeConfiguration) {
		super(
				"string",
				new ArgumentTypesValidator( null, ENUM ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
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

		if ( argumentType.isString() || argumentType.getDefaultSqlTypeCode() == SqlTypes.ENUM ) {
			singleExpression.accept( walker );
		}
		else if ( argumentType.isInteger() ) {
			EnumJavaType<?> enumJavaType = (EnumJavaType<?>) singleJdbcMapping.getMappedJavaType();
			Object[] enumConstants = enumJavaType.getJavaTypeClass().getEnumConstants();

			sqlAppender.appendSql( "case " );
			singleExpression.accept( walker );
			for ( Object e : enumConstants ) {
				Enum<?> enumValue = (Enum<?>) e;
				sqlAppender.appendSql( " when " );
				sqlAppender.appendSql( enumValue.ordinal() );
				sqlAppender.appendSql( " then " );
				sqlAppender.appendSingleQuoteEscapedString( enumValue.name() );
			}
			sqlAppender.appendSql( " end" );
		}
		else {
			throw new QueryException( "Unsupported enum type passed to 'string()' function: " + argumentType );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(ENUM arg)";
	}
}
