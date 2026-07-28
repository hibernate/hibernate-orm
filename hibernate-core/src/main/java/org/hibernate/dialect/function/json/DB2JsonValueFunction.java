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
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.dialect.function.array.DdlTypeHelper.getCastTypeName;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * DB2 json_value function.
 */
public class DB2JsonValueFunction extends JsonValueFunction {

	public DB2JsonValueFunction(TypeConfiguration typeConfiguration) {
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
				sqlAppender.append( "decode(" );
				super.render( sqlAppender, arguments, returnType, walker );

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
				break;
			case UUID:
				sqlAppender.append( "hextoraw(replace(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ",'-',''))" );
				break;
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				sqlAppender.append( "hextoraw(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ')' );
				break;
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
				sqlAppender.append( "cast(trim(trailing 'Z' from " );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ") as " );
				sqlAppender.append( getCastTypeName( arguments.returningType(), walker.getSessionFactory().getTypeConfiguration() ) );
				sqlAppender.append( ')' );
				break;
			default:
				super.render( sqlAppender, arguments, returnType, walker );
		}
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		if ( arguments.returningType() != null ) {
			final JdbcMapping jdbcMapping = arguments.returningType().getJdbcMapping();
			final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
			// No return type for booleans (handled via decode), UUID, binary types (handled via hextoraw),
			// or timestamp types (handled via special casting)
			if ( requiresSpecialExtraction( sqlTypeCode ) ) {
				if ( sqlTypeCode == TIMESTAMP_WITH_TIMEZONE || sqlTypeCode == TIMESTAMP_UTC ) {
					// Need to return a varchar instead of a clob (default for json_value) to avoid issues with the
					// trim function not accepting clob
					sqlAppender.append( " returning varchar(35)" );
				}
			}
			else {
				super.renderReturningClause( sqlAppender, arguments, walker );
			}
		}
	}

	private static boolean requiresSpecialExtraction(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN, UUID, BINARY, VARBINARY, LONG32VARBINARY, BLOB, TIMESTAMP_WITH_TIMEZONE, TIMESTAMP_UTC -> true;
			default -> false;
		};
	}
}
