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

import static org.hibernate.dialect.function.array.DdlTypeHelper.getTypeName;
import static org.hibernate.type.SqlTypes.*;


/**
 * Oracle json_value function.
 */
public class OracleJsonValueFunction extends JsonValueFunction {

	private static final int JSON_VALUE_MAX_BINARY_HEX_LENGTH = 32767;

	public OracleJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false, false );
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

		// Handle special types with custom extraction
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				if ( isEncodedBoolean( jdbcMapping ) ) {
					sqlAppender.append( "decode(" );
					super.render( sqlAppender, arguments, returnType, walker );
					//noinspection unchecked
					final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = jdbcMapping.getJdbcLiteralFormatter();
					final SessionFactoryImplementor sessionFactory = walker.getSessionFactory();
					final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
					final WrapperOptions wrapperOptions = sessionFactory.getWrapperOptions();
					final Object trueValue = jdbcMapping.convertToRelationalValue( true );
					final Object falseValue = jdbcMapping.convertToRelationalValue( false );
					sqlAppender.append( ",'true'," );
					jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, trueValue, dialect, wrapperOptions );
					sqlAppender.append( ",'false'," );
					jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, falseValue, dialect, wrapperOptions );
					sqlAppender.append( ')' );
				}
				else {
					super.render( sqlAppender, arguments, returnType, walker );
				}
				break;

			case DATE:
				if ( supportsOson( walker ) ) {
					// Oracle OSON extension is used, value is not stored as string
					super.render( sqlAppender, arguments, returnType, walker );
				}
				else {
					sqlAppender.append( "to_date(substr(" );
					super.render( sqlAppender, arguments, returnType, walker );
					sqlAppender.append( ",1,10),'YYYY-MM-DD')" );
				}
				break;

			case TIME:
				sqlAppender.append( "to_timestamp(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ",'hh24:mi:ss')" );
				break;
			case TIMESTAMP:
				if ( supportsOson( walker ) ) {
					super.render( sqlAppender, arguments, returnType, walker );
				}
				else {
					sqlAppender.append( "to_timestamp(" );
					super.render( sqlAppender, arguments, returnType, walker );
					sqlAppender.append( ",'YYYY-MM-DD\"T\"hh24:mi:ss.FF9')" );
				}
				break;
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
				if ( supportsOson( walker ) ) {
					// Oracle OSON extension is used, value is not stored as string
					super.render( sqlAppender, arguments, returnType, walker );
				}
				else {
					sqlAppender.append( "to_timestamp_tz(" );
					super.render( sqlAppender, arguments, returnType, walker );
					sqlAppender.append( ",'YYYY-MM-DD\"T\"hh24:mi:ss.FF9TZH:TZM')" );
				}
				break;
			case UUID:
				sqlAppender.append( "hextoraw(replace(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ",'-',''))" );
				break;
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				sqlAppender.append( "hextoraw(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( ')' );
				break;
			case BLOB:
				sqlAppender.append( "to_blob(hextoraw(" );
				super.render( sqlAppender, arguments, returnType, walker );
				sqlAppender.append( "))" );
			default:
				super.render( sqlAppender, arguments, returnType, walker );
				break;
		}
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
		if ( arguments.returningType() != null ) {
			final JdbcMapping jdbcMapping = arguments.returningType().getJdbcMapping();
			final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();

			// No return type for encoded booleans, UUID, or binary types (handled via decode/hextoraw)
			if ( requiresSpecialExtraction( sqlTypeCode, jdbcMapping ) ) {
				if ( isEncodedBoolean( jdbcMapping ) ) {
					sqlAppender.appendSql( " returning varchar2(5)" );
				}
				else {
					switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
						case TINYINT:
						case SMALLINT:
						case INTEGER:
						case BIGINT:
						case CLOB:
						case NCLOB:
							// use getTypeName (not getCastTypeName) so that CLOB/NCLOB
							// columns render as 'returning clob/nclob' — Oracle accepts
							// those here, unlike in cast() targets, and truncating to
							// varchar2 would lose content for large strings
							sqlAppender.appendSql( " returning " );
							sqlAppender.appendSql( getTypeName( arguments.returningType(),
									walker.getSessionFactory().getTypeConfiguration() ) );
							break;

						case DATE:
							if ( supportsOson( walker ) ) {
								// Oracle OSON extension is used, value is not stored as string
								sqlAppender.appendSql( " returning date" );
							}
							break;

						case TIMESTAMP:
							if ( supportsOson( walker ) ) {
								sqlAppender.appendSql( " returning timestamp(9)" );
							}
							break;
						case TIMESTAMP_WITH_TIMEZONE:
						case TIMESTAMP_UTC:
							if ( supportsOson( walker ) ) {
								// Oracle OSON extension is used, value is not stored as string
								sqlAppender.appendSql( " returning timestamp(9) with time zone" );
							}
							break;
						case BINARY:
						case VARBINARY:
						case LONG32VARBINARY:
							// We encode binary data as hex, so we have to decode here
							final Long binaryLength = arguments.returningType().getLength();
							if ( binaryLength != null
									? binaryLength * 2 < 4000L
									: !jdbcMapping.getJdbcType().isLobOrLong() ) {
								break;
							}
							// Fall-through intended
						case BLOB:
							// We encode binary data as hex, so we have to decode here
							// Oracle accepts large VARCHAR2 sizes in JSON_VALUE return clauses, even
							// when general SQL VARCHAR2 casts are still capped lower, so decode the
							// hex string through a wide scalar value and then materialize a BLOB.
							sqlAppender.appendSql( " returning varchar2(" );
							sqlAppender.appendSql( JSON_VALUE_MAX_BINARY_HEX_LENGTH );
							sqlAppender.appendSql( ")" );
							break;
					}
				}
				// UUID and binary don't need returning clause, handled by hextoraw wrapper
				return;
			}
		}
		super.renderReturningClause( sqlAppender, arguments, walker );
	}

	private boolean supportsOson(SqlAstTranslator<?> walker) {
		return walker.getSessionFactory().getJdbcServices().getDialect().getVersion().isSameOrAfter( 23 );
	}

	private static boolean requiresSpecialExtraction(int sqlTypeCode, JdbcMapping jdbcMapping) {
		return switch ( sqlTypeCode ) {
			case TINYINT, SMALLINT, INTEGER, BIGINT,
				CLOB, NCLOB, UUID,
				DATE, TIME, TIMESTAMP, TIMESTAMP_WITH_TIMEZONE, TIMESTAMP_UTC,
				BINARY, VARBINARY, LONG32VARBINARY, BLOB -> true;
			case BOOLEAN -> isEncodedBoolean( jdbcMapping );
			default -> false;
		};
	}

	public static boolean isEncodedBoolean(JdbcMapping type) {
		return type.getJdbcType().isBoolean() && type.getJdbcType().getDdlTypeCode() != BOOLEAN;
	}
}
