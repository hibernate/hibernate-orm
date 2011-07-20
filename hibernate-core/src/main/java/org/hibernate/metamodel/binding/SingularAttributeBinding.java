/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.relational.Value;

/**
 * Specialized binding contract for singular (non-collection) attributes
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeBinding extends AttributeBinding {
	/**
	 * Obtain the value bound here.  This could potentially be a {@link org.hibernate.metamodel.relational.Tuple}
	 * indicating multiple database values are bound, in which case access to the individual values can be achieved by
	 * either casting this return to {@link org.hibernate.metamodel.relational.Tuple} and using its
	 * {@link org.hibernate.metamodel.relational.Tuple#values()} method or using the {@link #getSimpleValueBindings()}
	 * method here and accessing each bindings {@link SimpleValueBinding#getSimpleValue simple value}
	 *
	 * @return The bound value
	 */
	public Value getValue();

	/**
	 * Returns the number of {@link SimpleValueBinding} objects that will be returned by
	 * {@link #getSimpleValueBindings()}
	 *
	 * @return the number of {@link SimpleValueBinding simple value bindings}
	 *
	 * @see #getSimpleValueBindings()
	 */
	public int getSimpleValueSpan();

	public Iterable<SimpleValueBinding> getSimpleValueBindings();

	public void setSimpleValueBindings(Iterable<SimpleValueBinding> simpleValueBindings);

	/**
	 * Convenience method to determine if any {@link SimpleValueBinding simple value bindings} are derived values
	 * (formula mappings).
	 *
	 * @return {@code true} indicates that the binding contains a derived value; {@code false} indicates it does not.
	 */
	public boolean hasDerivedValue();

	/**
	 * Convenience method to determine if all {@link SimpleValueBinding simple value bindings} allow nulls.
	 *
	 * @return {@code true} indicates that all values allow {@code null}; {@code false} indicates one or more do not
	 */
	public boolean isNullable();

	/**
	 * Obtain the generation strategy for this attribute/value.
	 *
	 * @return The generation strategy
	 */
	public PropertyGeneration getGeneration();
}
