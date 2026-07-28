/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.aggregate.OracleAggregateSupport;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_object function.
 */
public class OracleJsonObjectFunction extends JsonObjectFunction {

	private final CastTarget stringCastTarget;
	private CastFunction castFunction;

	public OracleJsonObjectFunction(boolean colonSyntax, TypeConfiguration typeConfiguration) {
		super( typeConfiguration, colonSyntax );
		this.stringCastTarget = new CastTarget( typeConfiguration.getBasicTypeForJavaType( String.class ) );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		final SessionFactoryImplementor sessionFactory = walker.getSessionFactory();
		if ( ExpressionTypeHelper.isNonNativeBoolean( value ) ) {
			CastFunction castFunction = this.castFunction;
			if ( castFunction == null ) {
				castFunction = this.castFunction = (CastFunction) sessionFactory
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
			final JdbcMapping jdbcMapping = ( (Expression) value ).getExpressionType().getSingleJdbcMapping();
			((OracleAggregateSupport) OracleAggregateSupport.valueOf( sessionFactory.getJdbcServices().getDialect() )).appendJsonWriteExpression(
					sqlAppender,
					() -> value.accept( walker ),
					jdbcMapping,
					sessionFactory.getTypeConfiguration()
			);
				if ( jdbcMapping.getJdbcType().isJson()
					&& !SqlTypes.isJsonType( jdbcMapping.getJdbcType().getDdlTypeCode() ) ) {
					sqlAppender.appendSql( " format json" );
				}
		}
	}
}
