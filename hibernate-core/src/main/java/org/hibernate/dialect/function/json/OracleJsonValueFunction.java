/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_value function.
 */
public class OracleJsonValueFunction extends JsonValueFunction {

	public OracleJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final boolean encodedBoolean = arguments.returningType() != null
				&& isEncodedBoolean( arguments.returningType().getJdbcMapping() );
		if ( encodedBoolean ) {
			sqlAppender.append( "decode(" );
		}
		super.render( sqlAppender, arguments, returnType, walker );
		if ( encodedBoolean ) {
			final JdbcMapping type = arguments.returningType().getJdbcMapping();
			//noinspection unchecked
			final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = type.getJdbcLiteralFormatter();
			final SessionFactoryImplementor sessionFactory = walker.getSessionFactory();
			final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
			final WrapperOptions wrapperOptions = sessionFactory.getWrapperOptions();
			final Object trueValue = type.convertToRelationalValue( true );
			final Object falseValue = type.convertToRelationalValue( false );
			sqlAppender.append( ",'true'," );
			jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, trueValue, dialect, wrapperOptions );
			sqlAppender.append( ",'false'," );
			jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, falseValue, dialect, wrapperOptions );
			sqlAppender.append( ')' );
		}
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		if ( arguments.returningType() != null && isEncodedBoolean( arguments.returningType().getJdbcMapping() ) ) {
			sqlAppender.appendSql( " returning varchar2(5)" );
		}
		else {
			super.renderReturningClause( sqlAppender, arguments, walker );
		}
	}

	public static boolean isEncodedBoolean(JdbcMapping type) {
		return type.getJdbcType().isBoolean() && type.getJdbcType().getDdlTypeCode() != SqlTypes.BOOLEAN;
	}
}
