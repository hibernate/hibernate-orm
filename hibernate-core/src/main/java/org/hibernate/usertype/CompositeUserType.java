/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * A <tt>UserType</tt> that may be dereferenced in a query.
 * This interface allows a custom type to define "properties".
 * These need not necessarily correspond to physical JavaBeans
 * style properties.<br>
 * <br>
 * A <tt>CompositeUserType</tt> may be used in almost every way
 * that a component may be used. It may even contain many-to-one
 * associations.<br>
 * <br>
 * Implementors must be immutable and must declare a public
 * default constructor.<br>
 * <br>
 * Unlike <tt>UserType</tt>, cacheability does not depend upon
 * serializability. Instead, <tt>assemble()</tt> and
 * <tt>disassemble</tt> provide conversion to/from a cacheable
 * representation.
 * <br>
 * Properties are ordered by the order of their names
 * i.e. they are alphabetically ordered, such that
 * <code>properties[i].name &lt; properties[i + 1].name</code>
 * for all <code>i &gt;= 0</code>.
 */
@Incubating
public interface CompositeUserType<J> extends EmbeddableInstantiator {

	/**
	 * Get the value of a property.
	 *
	 * @param component an instance of class mapped by this "type"
	 * @param property the property index
	 * @return the property value
	 * @throws HibernateException
	 */
	Object getPropertyValue(J component, int property) throws HibernateException;

	@Override
	J instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory);

	/**
	 * The class that represents the embeddable mapping of the type.
	 *
	 * @return Class
	 */
	Class<?> embeddable();

	/**
	 * The class returned by {@code instantiate()}.
	 *
	 * @return Class
	 */
	Class<J> returnedClass();

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality".
	 * Equality of the persistent state.
	 */
	boolean equals(J x, J y);

	/**
	 * Get a hashcode for the instance, consistent with persistence "equality"
	 */
	int hashCode(J x);

	/**
	 * Return a deep copy of the persistent state, stopping at entities and at
	 * collections. It is not necessary to copy immutable objects, or null
	 * values, in which case it is safe to simply return the argument.
	 *
	 * @param value the object to be cloned, which may be null
	 * @return Object a copy
	 */
	J deepCopy(J value);

	/**
	 * Are objects of this type mutable?
	 *
	 * @return boolean
	 */
	boolean isMutable();

	/**
	 * Transform the object into its cacheable representation. At the very least this
	 * method should perform a deep copy if the type is mutable. That may not be enough
	 * for some implementations, however; for example, associations must be cached as
	 * identifier values. (optional operation)
	 *
	 * @param value the object to be cached
	 * @return a cacheable representation of the object
	 */
	Serializable disassemble(J value);

	/**
	 * Reconstruct an object from the cacheable representation. At the very least this
	 * method should perform a deep copy if the type is mutable. (optional operation)
	 *
	 * @param cached the object to be cached
	 * @param owner the owner of the cached object
	 * @return a reconstructed object from the cacheable representation
	 */
	J assemble(Object cached, Object owner);

	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param detached the value from the detached entity being merged
	 * @param managed the value in the managed entity
	 *
	 * @return the value to be merged
	 */
	J replace(J detached, J managed, Object owner);

	@Override
	default boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return returnedClass().isInstance( object );
	}

	@Override
	default boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return object.getClass().equals( returnedClass() );
	}
}
