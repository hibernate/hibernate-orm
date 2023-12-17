/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Custom {@link TruncFunction} for SQL Server versions < 16 which uses the custom {@link DateTruncConvertEmulation}
 *
 * @author Marco Belladelli
 */
public class SqlServerConvertTruncFunction extends TruncFunction {
	private final DateTruncConvertEmulation dateTruncEmulation;

	public SqlServerConvertTruncFunction(TypeConfiguration typeConfiguration) {
		super(
				"round(?1,0,1)",
				"round(?1,?2,1)",
				null,
				null,
				typeConfiguration
		);
		this.dateTruncEmulation = new DateTruncConvertEmulation( typeConfiguration );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
			// datetime truncation
			return dateTruncEmulation.generateSqmExpression(
					arguments,
					impliedResultType,
					queryEngine
			);
		}
		// numeric truncation
		return new SelfRenderingSqmFunction<>(
				this,
				numericRenderingSupport,
				args,
				impliedResultType,
				TruncArgumentsValidator.NUMERIC_VALIDATOR,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	/**
	 * Custom {@link DateTruncEmulation} that handles rendering when using the convert function to parse datetime strings
	 *
	 * @author Marco Belladelli
	 */
	private static class DateTruncConvertEmulation extends DateTruncEmulation {
		public DateTruncConvertEmulation(TypeConfiguration typeConfiguration) {
			super( "convert", typeConfiguration );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			sqlAppender.appendSql( toDateFunction );
			sqlAppender.append( '(' );
			sqlAppender.append( "datetime," );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ')' );
		}
	}
}
