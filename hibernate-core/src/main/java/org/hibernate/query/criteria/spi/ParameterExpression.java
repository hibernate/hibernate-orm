/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.named.spi.ParameterMemento;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * Defines a parameter specification, or the information about a parameter (where it occurs, what is
 * its type, etc).
 *
 * @author Steve Ebersole
 */
public class ParameterExpression<T>
		extends AbstractExpression<T>
		implements JpaParameterExpression<T>, QueryParameterImplementor<T> {
	private final String name;
	private boolean allowsMultiValuedBinding;

	private AllowableParameterType explicitType;

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

	public AllowableParameterType getExplicitType() {
		return explicitType;
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
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitParameter( this );
	}

	@Override
	public void disallowMultiValuedBinding() {
		setAllowsMultiValuedBinding( true );
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowsMultiValuedBinding;
	}

	public void setAllowsMultiValuedBinding(boolean allowsMultiValuedBinding) {
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}


	@Override
	public ParameterMemento toMemento() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AllowableParameterType<T> getHibernateType() {
		return null;
	}
}
