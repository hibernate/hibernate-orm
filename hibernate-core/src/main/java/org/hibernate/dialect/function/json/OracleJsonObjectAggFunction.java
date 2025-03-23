/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.function.CastFunction;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_objectagg function.
 */
public class OracleJsonObjectAggFunction extends JsonObjectAggFunction {

	private final CastTarget stringCastTarget;
	private CastFunction castFunction;

	public OracleJsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( " value ", false, typeConfiguration );
		this.stringCastTarget = new CastTarget( typeConfiguration.getBasicTypeForJavaType( String.class ) );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		if ( ExpressionTypeHelper.isNonNativeBoolean( arg ) ) {
			CastFunction castFunction = this.castFunction;
			if ( castFunction == null ) {
				castFunction = this.castFunction = (CastFunction) translator.getSessionFactory()
						.getQueryEngine()
						.getSqmFunctionRegistry()
						.findFunctionDescriptor( "cast" );
			}
			castFunction.render(
					sqlAppender,
					List.of( arg, stringCastTarget ),
					(ReturnableType<?>) stringCastTarget.getJdbcMapping(),
					translator
			);
			sqlAppender.appendSql( " format json" );
		}
		else {
			arg.accept( translator );
			final JdbcMappingContainer expressionType = arg.getExpressionType();
			if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson()
					&& !SqlTypes.isJsonType( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
				sqlAppender.appendSql( " format json" );
			}
		}
	}
}
