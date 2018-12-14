/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaParameterExpression;

/**
 * Defines a parameter specification, or the information about a parameter (where it occurs, what is
 * its type, etc).
 *
 * @author Steve Ebersole
 */
public class ParameterExpression<T>
		extends AbstractExpression<T>
		implements JpaParameterExpression<T> {
	private final String name;

	public ParameterExpression(String name, Class<T> javaType, CriteriaNodeBuilder builder) {
		super( javaType, builder );
		this.name = name;
	}

	public ParameterExpression(Class<T> javaType, CriteriaNodeBuilder builder) {
		this( null, javaType, builder );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getParameterType() {
		return (Class) getJavaType();
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitParameter( this );
	}
}
