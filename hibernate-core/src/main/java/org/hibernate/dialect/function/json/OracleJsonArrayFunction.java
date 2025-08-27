/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_array function.
 */
public class OracleJsonArrayFunction extends JsonArrayFunction {

	private final CastTarget stringCastTarget;
	private final @Nullable String returningType;
	private CastFunction castFunction;

	public OracleJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.stringCastTarget = new CastTarget( typeConfiguration.getBasicTypeForJavaType( String.class ) );
		final BasicType<String> jsonType = typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON );
		final String jsonTypeName = typeConfiguration.getDdlTypeRegistry().getTypeName(
				jsonType.getJdbcType().getDdlTypeCode(),
				Size.nil(),
				jsonType
		);
		this.returningType = jsonTypeName;
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( ExpressionTypeHelper.isNonNativeBoolean( value ) ) {
			CastFunction castFunction = this.castFunction;
			if ( castFunction == null ) {
				castFunction = this.castFunction = (CastFunction) walker.getSessionFactory()
						.getQueryEngine()
						.getSqmFunctionRegistry()
						.findFunctionDescriptor( "cast" );
			}
			castFunction.render(
					sqlAppender,
					List.of( value, stringCastTarget ),
					(ReturnableType<?>) stringCastTarget.getJdbcMapping(),
					walker
			);
			sqlAppender.appendSql( " format json" );
		}
		else {
			value.accept( walker );
			final JdbcMappingContainer expressionType = ( (Expression) value ).getExpressionType();
			if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson()
					&& !SqlTypes.isJsonType( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
				sqlAppender.appendSql( " format json" );
			}
		}
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( " returning " );
		sqlAppender.appendSql( returningType );
	}
}
