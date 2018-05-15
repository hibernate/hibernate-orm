/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.util.Comparator;
import javax.persistence.metamodel.BasicType;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Abstract adapter for JavaTypeDescriptor implementations describing a "basic type" as defined
 * by JPA (as per {@link BasicType}).
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBasicJavaDescriptor<T>
		extends AbstractJavaDescriptor<T>
		implements BasicJavaDescriptor<T> {

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 *
	 * @see #AbstractBasicJavaDescriptor(Class, MutabilityPlan)
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractBasicJavaDescriptor(Class<? extends T> type) {
		this(
				type,
				(MutabilityPlan<T>) ImmutableMutabilityPlan.INSTANCE,
				Comparable.class.isAssignableFrom( type )
						? (Comparator<T>) ComparableComparator.INSTANCE
						: null
		);
	}

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractBasicJavaDescriptor(Class<? extends T> type, MutabilityPlan<T> mutabilityPlan) {
		this(
				type,
				mutabilityPlan,
				Comparable.class.isAssignableFrom( type )
						? (Comparator<T>) ComparableComparator.INSTANCE
						: null
		);
	}

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractBasicJavaDescriptor(Class<? extends T> type, MutabilityPlan<T> mutabilityPlan, Comparator comparator) {
		super( type.getName(), type, mutabilityPlan, comparator );
	}

	@Override
	public String extractLoggableRepresentation(T value) {
		return (value == null) ? "null" : value.toString();
	}

	@Override
	public Class<T> getJavaType() {
		return super.getJavaType();
	}

	@Override
	public String getTypeName() {
		return getJavaType().getName();
	}

	protected HibernateException unknownUnwrap(Class conversionType) {
		throw new HibernateException(
				"Unknown unwrap conversion requested: " + getJavaType().getName() + " to " + conversionType.getName()
		);
	}

	protected HibernateException unknownWrap(Class conversionType) {
		throw new HibernateException(
				"Unknown wrap conversion requested: " + conversionType.getName() + " to " + getJavaType().getName()
		);
	}
}
