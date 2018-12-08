/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStandardFunction<T> extends AbstractExpression<T> implements StandardFunction<T> {
	private final String functionName;

	public AbstractStandardFunction(
			String functionName,
			Class<T> javaType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
		this.functionName = functionName;
	}

	public AbstractStandardFunction(
			String functionName,
			JavaTypeDescriptor<T> javaTypeDescriptor,
			CriteriaNodeBuilder criteriaBuilder) {
		super( javaTypeDescriptor, criteriaBuilder );
		this.functionName = functionName;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public boolean isAggregator() {
		return false;
	}
}
