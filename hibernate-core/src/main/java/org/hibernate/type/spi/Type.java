/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;

/**
 * Base contract in the Hibernate "(mapping) type system".
 * <p/>
 * It is important to understand that a Type logically models the information
 * for a Java type and one or more SQL types.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface Type<T> extends org.hibernate.sqm.domain.Type, javax.persistence.metamodel.Type<T> {
	/**
	 * Enumerated values for the classification of the Type.
	 */
	enum Classification {
		/**
		 * Represents basic types (Strings, Integers, enums, etc).  Types classified as
		 * BASIC will be castable to {@link BasicType}
		 */
		BASIC( PersistenceType.BASIC ),
		/**
		 * Represents composite values (what JPA calls embedded/embeddable).  Types classified as
		 * COMPOSITE will be castable to {@link EmbeddableType}
		 */
		COMPOSITE( PersistenceType.EMBEDDABLE ),
		/**
		 * Represents reverse-discriminated values (where the discriminator is on the FK side of the association).
		 * Types classified as ANY will be castable to {@link AnyType}
		 */
		ANY( null ),
		/**
		 * Represents an entity value (either as a root, one-to-one or many-to-one).  Types classified
		 * as ENTITY will be castable to {@link EntityType}
		 */
		ENTITY( PersistenceType.ENTITY ),
		MAPPED_SUPERCLASS( PersistenceType.MAPPED_SUPERCLASS ),
		/**
		 * Represents a plural attribute, including the FK.   Types classified as COLLECTION
		 * will be castable to {@link CollectionType}
		 */
		COLLECTION( null );

		private final PersistenceType jpaPersistenceType;

		Classification(PersistenceType jpaPersistenceType) {
			this.jpaPersistenceType = jpaPersistenceType;
		}

		public PersistenceType getJpaPersistenceType() {
			return jpaPersistenceType;
		}

		public static Classification fromJpaPersistenceType(PersistenceType jpaPersistenceType) {
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

	/**
	 * Return the classification of this Type.
	 *
	 * @return The Type's classification/categorization
	 */
	Classification getClassification();

	@Override
	default PersistenceType getPersistenceType() {
		return this.getClassification().getJpaPersistenceType();
	}

	/**
	 * Returns the abbreviated name of the Type.  Mostly used historically for short-name
	 * referencing of the Type in {@code hbm.xml} mappings.
	 *
	 * @return The Type name
	 */
	String getName();

	/**
	 * Obtain a descriptor for the Java side of a value mapping.
	 *
	 * @return The Java type descriptor.
	 */
	JavaTypeDescriptor getJavaTypeDescriptor();

	/**
	 * The mutability of this type.  Generally follows
	 * {@link #getJavaTypeDescriptor()} -> {@link JavaTypeDescriptor#getMutabilityPlan()}
	 *
	 * @return The type's mutability
	 */
	MutabilityPlan getMutabilityPlan();

	/**
	 * The comparator for this type.  Generally follows
	 * {@link #getJavaTypeDescriptor()} -> {@link JavaTypeDescriptor#getComparator()}
	 *
	 * @return The type's comparator
	 */
	Comparator getComparator();

	/**
	 * Generate a representation of the value for logging purposes.
	 *
	 * @param value The value to be logged
	 * @param factory The session factory
	 *
	 * @return The loggable representation
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	String toLoggableString(Object value, SessionFactoryImplementor factory);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// "read" and "write" contracts...

	/**
	 * Extract a value from the JDBC result set.  This is useful for 2-phase property initialization - the second
	 * phase is a call to {@link #resolve}
	 * This hydrated value will be either:<ul>
	 *     <li>in the case of an entity or collection type, the key</li>
	 *     <li>otherwise, the value itself</li>
	 * </ul>
	 *
	 * @param rs The JDBC result set
	 * @param names the column names making up this type value (use to read from result set)
	 * @param session The originating session
	 * @param owner the parent entity
	 *
	 * @return An entity or collection key, or an actual value.
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 *
	 * @see #resolve
	 */
	Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException;

	/**
	 * The second phase of 2-phase loading.  Only really pertinent for entities and collections.  Here we resolve the
	 * identifier to an entity or collection instance
	 *
	 * @param value an identifier or value returned by <tt>hydrate()</tt>
	 * @param owner the parent entity
	 * @param session the session
	 *
	 * @return the given value, or the value associated with the identifier
	 *
	 * @throws HibernateException An error from Hibernate
	 *
	 * @see #hydrate
	 */
	Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException;

	/**
	 * Given a hydrated, but unresolved value, return a value that may be used to reconstruct property-ref
	 * associations.
	 *
	 * @param value The unresolved, hydrated value
	 * @param session THe originating session
	 * @param owner The value owner
	 *
	 * @return The semi-resolved value
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	Object semiResolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException;

	/**
	 * As part of 2-phase loading, when we perform resolving what is the resolved type for this type?  Generally
	 * speaking the type and its semi-resolved type will be the same.  The main deviation from this is in the
	 * case of an entity where the type would be the entity type and semi-resolved type would be its identifier type
	 *
	 * @param factory The session factory
	 *
	 * @return The semi-resolved type
	 */
	Type getSemiResolvedType(SessionFactoryImplementor factory);

	/**
	 * How many columns are used to persist this type.
	 * *
	 * @return The number of columns
	 *
	 */
	int getColumnSpan();
}
