/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.aggregate.MySQLAggregateSupport;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * MySQL json_value function.
 */
public class MySQLJsonValueFunction extends JsonValueFunction {

	public MySQLJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// json_extract errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR
				|| arguments.emptyBehavior() == JsonValueEmptyBehavior.ERROR
				// Can't emulate DEFAULT ON EMPTY since we can't differentiate between a NULL value and EMPTY
				|| arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			super.render( sqlAppender, arguments, returnType, walker );
		}
		else {
			final JdbcType jdbcType = arguments.returningType() == null
					? null
					: arguments.returningType().getJdbcMapping().getJdbcType();
			if ( jdbcType != null ) {
				switch ( jdbcType.getDefaultSqlTypeCode() ) {
					case BOOLEAN:
						sqlAppender.append( "case " );
						break;
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex, so we have to decode here
						sqlAppender.append( "unhex(json_unquote(" );
						break;
					case UUID:
						if ( jdbcType.isBinary() ) {
							if ( supportsUuidFunctions( walker ) ) {
								sqlAppender.append( "uuid_to_bin(json_unquote(" );
							}
							else {
								sqlAppender.append( "unhex(replace(json_unquote(" );
							}
							break;
						}
						// Fall-through intended
					default:
						sqlAppender.append( "cast(" );
						break;
				}
			}
			sqlAppender.appendSql( "json_unquote(nullif(json_extract(" );
			arguments.jsonDocument().accept( walker );
			sqlAppender.appendSql( "," );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause == null ) {
				arguments.jsonPath().accept( walker );
			}
			else {
				JsonPathHelper.appendJsonPathConcatPassingClause(
						sqlAppender,
						arguments.jsonPath(),
						passingClause, walker
				);
			}
			sqlAppender.appendSql( "),cast('null' as json)))" );
			if ( jdbcType != null ) {
				switch ( jdbcType.getDefaultSqlTypeCode() ) {
					case BOOLEAN:
						sqlAppender.append( supportsJsonType( walker )
								? " when cast('true' as json) then true when cast('false' as json) then false end"
								: " when 'true' then true when 'false' then false end" );
						break;
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						sqlAppender.append( "))" );
						break;
					case UUID:
						if ( jdbcType.isBinary() ) {
							if ( supportsUuidFunctions( walker ) ) {
								sqlAppender.append( "))" );
							}
							else {
								sqlAppender.append( "),'-',''))" );
							}
							break;
						}
						// Fall-through intended
					default:
						sqlAppender.appendSql( " as " );
						arguments.returningType().accept( walker );
						sqlAppender.appendSql( ')' );
						break;
				}
			}
		}
	}

	private static boolean supportsUuidFunctions(SqlAstTranslator<?> walker) {
		final Dialect dialect = walker.getSessionFactory().getJdbcServices().getDialect();
		return dialect.getAggregateSupport() == MySQLAggregateSupport.forTiDB( dialect )
				|| dialect instanceof MySQLDialect mySQLDialect && mySQLDialect.getMySQLVersion().isSameOrAfter( 8 );
	}

	private static boolean supportsJsonType(SqlAstTranslator<?> walker) {
		final Dialect dialect = walker.getSessionFactory().getJdbcServices().getDialect();
		return dialect.getAggregateSupport() != MySQLAggregateSupport.forMariaDB( dialect );
	}
}
