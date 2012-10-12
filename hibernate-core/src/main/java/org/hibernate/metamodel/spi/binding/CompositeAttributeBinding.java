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
package org.hibernate.metamodel.spi.binding;

/**
 * A specialized binding contract for a singular attribute binding that
 * contains other attribute bindings.
 *
 * @author Gail Badner
 */
public interface CompositeAttributeBinding
		extends SingularNonAssociationAttributeBinding, AttributeBindingContainer {

	/**
	 * Can the composite attribute be mapped to a single entity
	 * attribute by means of an actual component class that aggregates
	 * the tuple values?
	 *
	 * If {@code true} is returned, this instance can safely be cast
	 * to an {@link AggregatedCompositeAttributeBinding}.
	 *
	 * If {@code false} is returned, this instance can safely be cast
	 * to a {@link NonAggregatedCompositeAttributeBinding}.
	 *
	 * @return true, if the attribute can be mapped to a single entity
	 * attribute by means of an actual component class that aggregates
	 * the tuple values; false, otherwise.
	 */
	boolean isAggregated();
}
