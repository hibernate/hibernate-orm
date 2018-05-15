/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class NonSelectSqlExpressionResolver extends PerQuerySpecSqlExpressionResolver {
	private final StandardSqlExpressionResolver rootResolver;

	public NonSelectSqlExpressionResolver(
			SessionFactoryImplementor sessionFactory,
			Supplier<QuerySpec> querySpecSupplier,
			Function<Expression, Expression> normalizer,
			BiConsumer<Expression, SqlSelection> selectionConsumer) {
		super( sessionFactory, querySpecSupplier, normalizer, selectionConsumer );
		this.rootResolver = new StandardSqlExpressionResolver(
				querySpecSupplier,
				normalizer,
				selectionConsumer
		);
	}

	@Override
	protected StandardSqlExpressionResolver determineSubResolver(QuerySpec querySpec) {
		if ( querySpec == null ) {
			return rootResolver;
		}
		return super.determineSubResolver( querySpec );
	}
}
