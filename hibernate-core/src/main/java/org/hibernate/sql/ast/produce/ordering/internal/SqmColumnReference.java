/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.results.spi.Selectable;

/**
 * @author Steve Ebersole
 */
public class SqmColumnReference implements SqmExpression {
	private final SqmFrom sqmFromBase;

	public SqmColumnReference(SqmFrom sqmFromBase) {
		this.sqmFromBase = sqmFromBase;
	}

	@Override
	public ExpressableType getExpressionType() {
		return null;
	}

	@Override
	public ExpressableType getInferableType() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return (T) new ExplicitColumnReference();
	}

	@Override
	public String asLoggableText() {
		return null;
	}

	private class ExplicitColumnReference implements SelfRenderingExpression {
		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			// todo (6.0) - need to be able to resolve the table name to use as placeholder
			//
			// 		^^based on assumption that we will render the reference using the pattern:
			//				"{table_name}" + column_name
			//
			//		which allows us to be better about injecting the aliases - currently
			//			the referenced column must be from the "driving table".  The idea
			//			here is to handle that for any table in the "group"
		}

		@Override
		public ExpressableType getType() {
			return null;
		}

		@Override
		public Selectable getSelectable() {
			return null;
		}
	}
}
