/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.AbstractSqmFunction;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T> extends AbstractSqmFunction<T> implements SqlAstFunctionProducer<T> {
	private final SelfRenderingFunctionSupport renderingSupport;
	private final List<SqmExpression> sqmArguments;

	public SelfRenderingSqmFunction(
			SelfRenderingFunctionSupport renderingSupport,
			List<SqmExpression> sqmArguments,
			AllowableFunctionReturnType<T> impliedResultType,
			NodeBuilder nodeBuilder) {
		super( impliedResultType, nodeBuilder );
		this.renderingSupport = null;
		this.sqmArguments = sqmArguments;
	}

	public SelfRenderingSqmFunction(
			List<SqmExpression> sqmArguments,
			AllowableFunctionReturnType<T> impliedResultType,
			NodeBuilder nodeBuilder) {
		super( impliedResultType, nodeBuilder );
		this.renderingSupport = null;
		this.sqmArguments = sqmArguments;
	}

	public SelfRenderingFunctionSupport getRenderingSupport() {
		return renderingSupport;
	}

	public List<SqmExpression> getSqmArguments() {
		return sqmArguments;
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		return new SelfRenderingFunctionSqlAstExpression( this, walker );
	}
}
