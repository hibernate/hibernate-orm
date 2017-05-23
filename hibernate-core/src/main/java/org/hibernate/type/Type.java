/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 *
 * @author Steve Ebersole
 */
public interface Type<T> {
	/**
	 * Return the classification of this Type.
	 *
	 * @return The Type's classification/categorization
	 */
	Classification getClassification();

	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	/**
	 * Get the Java type handled by this Hibernate mapping Type.  May return {@code null}
	 * in the case of non-basic types in dynamic domain models.
	 */
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	/**
	 * Compare two instances of the class mapped by this type for
	 * persistence "equality" (equality of persistent state).
	 * <p/>
	 * This should always equate to some form of comparison of the value's internal state.  As an example, for
	 * something like a date the comparison should be based on its internal "time" state based on the specific portion
	 * it is meant to represent (timestamp, date, time).
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean areEqual(T x, T y) throws HibernateException;

	/**
	 * Return a String representation of the given value for use in Hibernate logging.
	 */
	default String toLoggableString(Object value) {
		return value == null ? "<null>" : value.toString();
	}

	/**
	 * Enumerated values for the classification of the Type.
	 */
	enum Classification {
		/**
		 * Indicates that this type represents basic values (Strings, Integers, enums, etc).
		 * <p/>
		 * Corresponds to the JPA {@link javax.persistence.metamodel.Type.PersistenceType#BASIC} classification
		 */
		BASIC( javax.persistence.metamodel.Type.PersistenceType.BASIC ),

		/**
		 * Represents composite values (what JPA calls embedded/embeddable).
		 * <p/>
		 * Corresponds to the JPA {@link javax.persistence.metamodel.Type.PersistenceType#EMBEDDABLE} classification
		 */
		COMPOSITE( javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE ),

		/**
		 * Represents reverse-discriminated values (where the discriminator is on the FK side of the association).
		 * <p/>
		 * Has no corresponding JPA classification.  JPA simply has no such concept.
		 */
		ANY( null ),

		/**
		 * Represents an entity value (either as a root, one-to-one or many-to-one).
		 * <p/>
		 * Corresponds to the JPA {@link javax.persistence.metamodel.Type.PersistenceType#ENTITY} classification
		 */
		ENTITY( javax.persistence.metamodel.Type.PersistenceType.ENTITY ),

		/**
		 * Generally an abstract idea, represents a "mapped superclass" in the inheritance hierarchy.
		 * <p/>
		 * Corresponds to the JPA {@link javax.persistence.metamodel.Type.PersistenceType#MAPPED_SUPERCLASS} classification
		 */
		MAPPED_SUPERCLASS( javax.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS ),

		/**
		 * Represents a plural attribute, including the FK.
		 * <p/>
		 * Has no corresponding JPA classification.  JPA handles this via PluralAttribute and the
		 * fact that support for Collection types in JPA is extremely narrow.
		 */
		COLLECTION( null );

		private final javax.persistence.metamodel.Type.PersistenceType jpaPersistenceType;

		Classification(javax.persistence.metamodel.Type.PersistenceType jpaPersistenceType) {
			this.jpaPersistenceType = jpaPersistenceType;
		}

		public javax.persistence.metamodel.Type.PersistenceType getJpaPersistenceType() {
			return jpaPersistenceType;
		}

		public static Classification fromJpaPersistenceType(javax.persistence.metamodel.Type.PersistenceType jpaPersistenceType) {
			switch ( jpaPersistenceType ) {
				case BASIC: {
					return BASIC;
				}
				case MAPPED_SUPERCLASS: {
					return MAPPED_SUPERCLASS;
				}
				case EMBEDDABLE: {
					return COMPOSITE;
				}
				case ENTITY: {
					return ENTITY;
				}
				default: {
					return null;
				}
			}
		}
	}
}
