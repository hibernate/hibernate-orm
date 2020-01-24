/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmJdbcFunctionEscapeWrapper;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Acts as a wrapper to another SqmFunctionTemplate - upon rendering uses the
 * standard JDBC escape sequence (i.e. `{fn blah}`) when rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class JdbcEscapeFunctionDescriptor
		extends AbstractSqmFunctionDescriptor {
	private final SqmFunctionDescriptor wrapped;

	public JdbcEscapeFunctionDescriptor(String name, SqmFunctionDescriptor wrapped) {
		super(name);
		this.wrapped = wrapped;
	}

	@Override
	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SqmJdbcFunctionEscapeWrapper<>(
				this,
				wrapped.generateSqmExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				),
				queryEngine.getCriteriaBuilder()
		);
	}
}
