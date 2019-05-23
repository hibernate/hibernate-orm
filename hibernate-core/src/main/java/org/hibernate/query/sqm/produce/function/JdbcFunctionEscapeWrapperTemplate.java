/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.function.SqmJdbcFunctionEscapeWrapper;

/**
 * Acts as a wrapper to another SqmFunctionTemplate - upon rendering uses the
 * standard JDBC escape sequence (i.e. `{fn blah}`) when rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class JdbcFunctionEscapeWrapperTemplate
		extends AbstractSqmFunctionTemplate {
	private final SqmFunctionTemplate wrapped;

	public JdbcFunctionEscapeWrapperTemplate(SqmFunctionTemplate wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new SqmJdbcFunctionEscapeWrapper<>(
				wrapped.makeSqmFunctionExpression(
						arguments,
						impliedResultType,
						queryEngine
				),
				queryEngine.getCriteriaBuilder()
		);
	}
}
