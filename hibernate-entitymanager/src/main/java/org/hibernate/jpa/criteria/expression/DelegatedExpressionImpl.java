/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.criteria.expression;

import java.util.List;
import javax.persistence.criteria.Selection;

import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.ValueHandlerFactory;

/**
 * Implementation of {@link javax.persistence.criteria.Expression} wraps another Expression and delegates most of its
 * functionality to that wrapped Expression
 *
 * @author Steve Ebersole
 */
public abstract class DelegatedExpressionImpl<T> extends ExpressionImpl<T> {
	private final ExpressionImpl<T> wrapped;

	public DelegatedExpressionImpl(ExpressionImpl<T> wrapped) {
		super( wrapped.criteriaBuilder(), wrapped.getJavaType() );
		this.wrapped = wrapped;
	}

	public ExpressionImpl<T> getWrapped() {
		return wrapped;
	}


	// delegations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void registerParameters(ParameterRegistry registry) {
		wrapped.registerParameters( registry );
	}

	@Override
	public Selection<T> alias(String alias) {
		wrapped.alias( alias );
		return this;
	}

	@Override
	public boolean isCompoundSelection() {
		return wrapped.isCompoundSelection();
	}

	@Override
	public List<ValueHandlerFactory.ValueHandler> getValueHandlers() {
		return wrapped.getValueHandlers();
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return wrapped.getCompoundSelectionItems();
	}

	@Override
	public Class<T> getJavaType() {
		return wrapped.getJavaType();
	}

	@Override
	protected void resetJavaType(Class targetType) {
		wrapped.resetJavaType( targetType );
	}

	@Override
	protected void forceConversion(ValueHandlerFactory.ValueHandler<T> tValueHandler) {
		wrapped.forceConversion( tValueHandler );
	}

	@Override
	public ValueHandlerFactory.ValueHandler<T> getValueHandler() {
		return wrapped.getValueHandler();
	}

	@Override
	public String getAlias() {
		return wrapped.getAlias();
	}

	@Override
	protected void setAlias(String alias) {
		wrapped.setAlias( alias );
	}
}
