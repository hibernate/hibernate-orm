/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

import java.util.Comparator;q
import javax.persistence.metamodel.BasicType;

/**
 * Abstract adapter for JavaTypeDescriptor implementations describing a "basic type" as defined
 * by JPA (as per {@link BasicType}).
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTypeDescriptorBasicImpl<T>
		extends AbstractTypeDescriptor<T>
		implements JavaTypeDescriptorBasicImplementor<T> {

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 *
	 * @see #AbstractTypeDescriptorBasicImpl(Class, MutabilityPlan)
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptorBasicImpl(Class<T> type) {
		this( type, (MutabilityPlan<T>) ImmutableMutabilityPlan.INSTANCE );
	}

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptorBasicImpl(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan );
	}

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptorBasicImpl(Class<T> type, MutabilityPlan<T> mutabilityPlan, Comparator comparator) {
		super( type, mutabilityPlan, comparator );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public String extractLoggableRepresentation(T value) {
		return (value == null) ? "null" : value.toString();
	}
}
