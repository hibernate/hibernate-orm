/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.json.JsonObjectFunction;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * GaussDB json_object function.
 * @author chenzhida
 *
 * Notes: Original code of this class is based on PostgreSQLJsonObjectFunction.
 */
public class GaussDBJsonObjectFunction extends JsonObjectFunction {

	// Hold the Dialect reference to query isMMode() at render time (mode is detected by GaussDBDialect
	// from datcompatibility). Keeping the reference rather than a boolean preserves a single source of
	// truth for the mode — same pattern as GaussDBExtractFunction.
	private final Dialect dialect;

	public GaussDBJsonObjectFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
		this.dialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		sqlAppender.appendSql( "json_build_object" );
		char separator = '(';
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( separator );
		}
		else {
			final JsonNullBehavior nullBehavior;
			final int argumentsCount;
			if ( ( sqlAstArguments.size() & 1 ) == 1 ) {
				nullBehavior = (JsonNullBehavior) sqlAstArguments.get( sqlAstArguments.size() - 1 );
				argumentsCount = sqlAstArguments.size() - 1;
			}
			else {
				nullBehavior = JsonNullBehavior.NULL;
				argumentsCount = sqlAstArguments.size();
			}
			sqlAppender.appendSql('(');
			separator = ' ';
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				final SqlAstNode key = sqlAstArguments.get( i );
				Expression valueNode = (Expression) sqlAstArguments.get( i+1 );
				if ( nullBehavior == JsonNullBehavior.ABSENT && walker.getLiteralValue( valueNode ) == null) {
					continue;
				}
				if (separator != ' ') {
					sqlAppender.appendSql(separator);
				}
				else {
					separator = ',';
				}
				key.accept( walker );
				sqlAppender.appendSql( ',' );
				renderValue( sqlAppender, valueNode, walker );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		final JdbcMappingContainer expressionType = ((Expression) value).getExpressionType();
		if ( expressionType.getSingleJdbcMapping().getJdbcType().isBinary() ) {
			// GaussDB json_build_object renders bytea/varbinary as the PG bytea text "\x01020304", but the
			// test (HHH-20522) expects the plain hex string "01020304". A mode stores binary as bytea and
			// uses PG encode(bytea,'hex'); M mode stores binary as varbinary (encode rejects varbinary)
			// and uses MySQL hex(varbinary) instead.
			if ( dialect instanceof GaussDBDialect g && g.isMMode() ) {
				sqlAppender.appendSql( "hex(" );
				value.accept( walker );
				sqlAppender.appendSql( ")" );
			}
			else {
				sqlAppender.appendSql( "encode(" );
				value.accept( walker );
				sqlAppender.appendSql( ",'hex')" );
			}
		}
		else if ( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.TIMESTAMP ) {
			// GaussDB json_build_object renders timestamp with a space separator and (in M mode)
			// truncates to seconds, but the test expects ISO "2024-01-15T10:30:45.123" (T separator).
			// A mode (Oracle-compatible) has PG to_char(datetime); M mode (MySQL-compatible) lacks it
			// and uses MySQL date_format instead.
			if ( dialect instanceof GaussDBDialect g && g.isMMode() ) {
				sqlAppender.appendSql( "date_format(" );
				value.accept( walker );
				sqlAppender.appendSql( ", '%Y-%m-%dT%H:%i:%s.%f')" );
			}
			else {
				sqlAppender.appendSql( "to_char(" );
				value.accept( walker );
				sqlAppender.appendSql( ", 'YYYY-MM-DD\"T\"HH24:MI:SS.US')" );
			}
		}
		else {
			value.accept( walker );
		}
	}

}
