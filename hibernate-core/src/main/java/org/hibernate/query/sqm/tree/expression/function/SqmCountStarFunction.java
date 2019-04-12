/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmCountStarFunction<T> extends AbstractSqmAggregateFunction<T> {
	public SqmCountStarFunction(AllowableFunctionReturnType<T> resultType, NodeBuilder nodeBuilder) {
		super( STAR, resultType, nodeBuilder );
	}

	@Override
	public String getFunctionName() {
		return SqmCountFunction.NAME;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCountStarFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "COUNT(*)";
	}

	@SuppressWarnings("unchecked")
	public static SqmExpression STAR = new AbstractSqmExpression( null, null ) {
		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		public BasicValuedExpressableType getExpressableType() {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		protected void internalApplyInferableType(ExpressableType newType) {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		public Object accept(SemanticQueryWalker walker) {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		public String asLoggableText() {
			return "*";
		}
	};
}
