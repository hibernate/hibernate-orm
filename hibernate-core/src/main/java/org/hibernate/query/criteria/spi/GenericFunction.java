/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

import org.hibernate.query.criteria.JpaSelection;

/**
 * Models a generic (non-standard) function call
 *
 * @author Steve Ebersole
 */
public class GenericFunction<X>
		extends AbstractExpression<X>
		implements JpaFunctionImplementor<X> {

	private final String functionName;
	private final List<? extends ExpressionImplementor<?>> arguments;

	public GenericFunction(
			String functionName,
			List<? extends ExpressionImplementor<?>> arguments,
			Class<X> javaType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
		this.functionName = functionName;
		this.arguments = arguments;
	}

	public String getFunctionName() {
		return functionName;
	}

	public boolean isAggregator() {
		return false;
	}

	public List<? extends ExpressionImplementor<?>> getFunctionArguments() {
		return arguments;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		// todo (6.0) : is this correct?
		return getFunctionArguments();
	}

	@Override
	public <T> T accept(CriteriaVisitor visitor) {
		return visitor.visitGenericFunction( this );
	}
}
