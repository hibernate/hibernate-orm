/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.sql.ast.spi.AbstractSqlAstTranslator.getCastTypeName;

/**
 * HANA json_value function.
 */
public class HANAJsonValueFunction extends JsonValueFunction {

	public HANAJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
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
			sqlAppender.append( "case " );
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
			sqlAppender.append( " when 'true' then " );
			jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, trueValue, dialect, wrapperOptions );
			sqlAppender.append( " when 'false' then " );
			jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, falseValue, dialect, wrapperOptions );
			sqlAppender.append( " end" );
		}
	}

	public static String jsonValueReturningType(SqlTypedMapping column) {
		final String columnDefinition = column.getColumnDefinition();
		assert columnDefinition != null;
		return jsonValueReturningType( columnDefinition );
	}

	public static String jsonValueReturningType(String columnDefinition) {
		final int parenthesisIndex = columnDefinition.indexOf( '(' );
		final String baseName = parenthesisIndex == -1
				? columnDefinition
				: columnDefinition.substring( 0, parenthesisIndex );
		return switch ( baseName ) {
			case "real", "float", "double", "decimal" -> "decimal";
			case "tinyint", "smallint" -> "integer";
			case "clob" -> "varchar(5000)";
			case "nclob" -> "nvarchar(5000)";
			default -> columnDefinition;
		};
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		// No return type for booleans, this is handled via decode
		if ( arguments.returningType() != null && !isEncodedBoolean( arguments.returningType().getJdbcMapping() ) ) {
			sqlAppender.appendSql( " returning " );
			sqlAppender.appendSql( jsonValueReturningType(
					getCastTypeName( arguments.returningType(), walker.getSessionFactory().getTypeConfiguration() )
			) );
		}
	}

	static boolean isEncodedBoolean(JdbcMapping type) {
		return type.getJdbcType().isBoolean();
	}
}
