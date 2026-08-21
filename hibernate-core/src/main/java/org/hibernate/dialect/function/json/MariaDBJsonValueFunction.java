/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
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
 * MariaDB json_value function.
 */
public class MariaDBJsonValueFunction extends JsonValueFunction {

	public MariaDBJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.NULL ) {
			// MariaDB reports the error 4038 as warning and simply returns null
			throw new QueryException( "Can't emulate on error clause on MariaDB" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on MariaDB" );
		}
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
							sqlAppender.append( "unhex(replace(json_unquote(" );
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
		sqlAppender.appendSql( "),'null'))" );
		if ( jdbcType != null ) {
			switch ( jdbcType.getDefaultSqlTypeCode() ) {
				case BOOLEAN:
					sqlAppender.append( " when 'true' then true when 'false' then false end" );
					break;
				case BINARY:
				case VARBINARY:
				case LONG32VARBINARY:
					sqlAppender.append( "))" );
					break;
				case UUID:
					if ( jdbcType.isBinary() ) {
						sqlAppender.append( "),'-',''))" );
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
