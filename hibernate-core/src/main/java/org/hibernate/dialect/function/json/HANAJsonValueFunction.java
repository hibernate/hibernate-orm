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

import static org.hibernate.dialect.function.array.DdlTypeHelper.getCastTypeName;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_UTC;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * HANA json_value function.
 */
public class HANAJsonValueFunction extends JsonValueFunction {

	// The memory limit for a data type on HANA
	private static final int MEMORY_LIMIT = 1024 * 1024 * 64;

	public HANAJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final JdbcMapping jdbcMapping = arguments.returningType() != null
				? arguments.returningType().getJdbcMapping()
				: null;
		final int sqlTypeCode = jdbcMapping != null
				? jdbcMapping.getJdbcType().getDefaultSqlTypeCode()
				: VARCHAR;

		// Handle UUID and binary types with special wrapping
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				sqlAppender.append( "case " );
				super.render( sqlAppender, arguments, returnType, walker );
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
				break;
			case UUID:
				sqlAppender.append( "hextobin(replace(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ",'-',''))" );
				break;
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				sqlAppender.append( "hextobin(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ')' );
				break;
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
			case TIME:
			case TIME_UTC:
				sqlAppender.append( "cast(trim(trailing 'Z' from " );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ") as " );
				sqlAppender.append( getCastTypeName( arguments.returningType(), walker.getSessionFactory().getTypeConfiguration() ) );
				sqlAppender.append( ')' );
				break;
			default:
				super.render( sqlAppender, arguments, returnType, walker );
				break;
		}
	}

	public static String jsonValueReturningType(SqlTypedMapping column, TypeConfiguration typeConfiguration) {
		return jsonValueReturningType( getCastTypeName( column, typeConfiguration ) );
	}

	public static String jsonValueReturningType(String columnDefinition) {
		final int parenthesisIndex = columnDefinition.indexOf( '(' );
		final String baseName = parenthesisIndex == -1
				? columnDefinition
				: columnDefinition.substring( 0, parenthesisIndex );
		return switch ( baseName ) {
			case "real", "float", "double", "decimal" -> "decimal";
			case "tinyint", "smallint" -> "integer";
			// Clobs are also not supported, so use the biggest varchar/nvarchar possible
			case "clob" -> "varchar(" + MEMORY_LIMIT + ")";
			case "nclob" -> "nvarchar(" + MEMORY_LIMIT + ")";
			default -> columnDefinition;
		};
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		// No return type for booleans, this is handled via decode
		if ( arguments.returningType() != null && !requiresSpecialExtraction( arguments.returningType().getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) ) {
			sqlAppender.appendSql( " returning " );
			sqlAppender.appendSql( jsonValueReturningType(
					getCastTypeName( arguments.returningType(), walker.getSessionFactory().getTypeConfiguration() )
			) );
		}
	}

	static boolean isEncodedBoolean(JdbcMapping type) {
		return type.getJdbcType().isBoolean();
	}

	private static boolean requiresSpecialExtraction(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN, UUID, BINARY, VARBINARY, LONG32VARBINARY, BLOB, TIMESTAMP, TIMESTAMP_WITH_TIMEZONE, TIMESTAMP_UTC, TIME, TIME_UTC -> true;
			default -> false;
		};
	}
}
