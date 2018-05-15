/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmSubQuery implements SqmExpression {
	private final SqmQuerySpec querySpec;
	private final ExpressableType expressableType;

	public SqmSubQuery(SqmQuerySpec querySpec, ExpressableType expressableType) {
		this.querySpec = querySpec;
		this.expressableType = expressableType;
	}

	@Override
	public ExpressableType getExpressableType() {
		return expressableType;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	public SqmQuerySpec getQuerySpec() {
		return querySpec;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSubQueryExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<subquery>";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return expressableType.getJavaTypeDescriptor();
	}

//	@Override
//	public QueryResult createDomainResult(
//			Expression expression, String resultVariable, QueryResultCreationContext creationContext) {
//		throw new TreeException( "Selecting a SubQuery type is not allowed.");
//	}
}
