/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.dialect.Dialect;

/**
 * Models a value expression.  It is the result of a <tt>formula</tt> mapping.
 *
 * @author Steve Ebersole
 */
public class DerivedValue extends AbstractValue {
	private final String expression;

	public DerivedValue(int position, String expression) {
		super( position );
		this.expression = expression;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.DERIVED_VALUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toLoggableString() {
		return "{derived-column}";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAlias(Dialect dialect, TableSpecification tableSpecification) {
		return "formula" + Integer.toString( getPosition() ) + '_';
	}

	/**
	 * Get the value expression.
	 * @return the value expression
	 */
	public String getExpression() {
		return expression;
	}
}
