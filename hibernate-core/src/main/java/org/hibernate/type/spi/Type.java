/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sqm.domain.type.SqmDomainType;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;

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
public interface Type<T> extends SqmDomainType {
	/**
	 * Enumerated values for the classification of the Type.
	 */
	enum Classification {
		/**
		 * Represents basic types (Strings, Integers, enums, etc).  Types classified as
		 * BASIC will be castable to {@link BasicType}.
		 * <p/>
		 * Corresponds to the JPA {@link PersistenceType#BASIC} classification
		 */
		BASIC( PersistenceType.BASIC ),

		/**
		 * Represents composite values (what JPA calls embedded/embeddable).  Types classified as
		 * COMPOSITE will be castable to {@link EmbeddedType}
		 * <p/>
		 * Corresponds to the JPA {@link PersistenceType#EMBEDDABLE} classification
		 */
		COMPOSITE( PersistenceType.EMBEDDABLE ),

		/**
		 * Represents reverse-discriminated values (where the discriminator is on the FK side of the association).
		 * Types classified as ANY will be castable to {@link AnyType}
		 * <p/>
		 * Has no corresponding JPA classification.  JPA simply has no such concept.
		 */
		ANY( null ),

		/**
		 * Represents an entity value (either as a root, one-to-one or many-to-one).  Types classified
		 * as ENTITY will be castable to {@link EntityType}
		 * <p/>
		 * Corresponds to the JPA {@link PersistenceType#ENTITY} classification
		 */
		ENTITY( PersistenceType.ENTITY ),

		/**
		 * Generally an abstract idea, represents a "mapped superclass" in the inheritance hierarchy.
		 * <p/>
		 * Corresponds to the JPA {@link PersistenceType#MAPPED_SUPERCLASS} classification
		 */
		MAPPED_SUPERCLASS( PersistenceType.MAPPED_SUPERCLASS ),

		/**
		 * Represents a plural attribute, including the FK.   Types classified as COLLECTION
		 * will be castable to {@link CollectionType}
		 * <p/>
		 * Has no corresponding JPA classification.  JPA handles this via PluralAttribute and the
		 * fact that support for Collection types in JPA is extremely narrow.
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


	/**
	 * Returns the abbreviated name of the Type.  Mostly used historically for short-name
	 * referencing of the Type in {@code hbm.xml} mappings.
	 *
	 * @return The Type name
	 */
	default String getName() {
		return String.format( Locale.ROOT, "%s[%s]", getClassification().name(), getJavaTypeDescriptor().getTypeName() );
	}

	/**
	 * Describes the columns mapping for this Type.
	 *
	 * @return The column mapping for this Type
	 */
	ColumnMapping[] getColumnMappings();

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

	/**
	 * If values of this type can be rendered as literals into SQL, access
	 * the formatter capable of doing that rendering.
	 *
	 * @return The JDBC/SQL literal formatter, or {@code null} if values
	 * of this type cannot be rendered as a literal.
	 */
	JdbcLiteralFormatter getJdbcLiteralFormatter();




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// "read" and "write" contracts...
	//
	//		- for now we simply use the method sigs from the legacy Type contract,
	//			especially in regards to name-based reads rather than position-based.
	//			Eventually we will transition that over to position based reads,
	//			but for now, in the interest of hopefully getting to a compilable
	//			and testable state sooner, we will put that transition off
	//			into its own work unit.
	//
	//		- this stuff is highly volatile atm, and for sure will change as
	//			we move to position-based reads (rather than name-based)
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Extract a value of the {@link #getReturnedClass() mapped class} from the JDBC result set. Implementors
	 * should handle possibility of null values.
	 * <p/>
	 * This is a form that could complete go away.
	 *
	 * @param rs The result set from which to extract value.
	 * @param names the column names making up this type value (use to read from result set)
	 * @param session The originating session
	 * @param owner the parent entity
	 *
	 * @return The extracted value
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 *
	 * @see org.hibernate.type.spi.Type#hydrate(ResultSet, String[], SharedSessionContractImplementor, Object) alternative, 2-phase property initialization
	 */
	Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException;

	/**
	 * Extract a value of the {@link #getReturnedClass() mapped class} from the JDBC result set. Implementors
	 * should handle possibility of null values.  This form might be called if the type is known to be a
	 * single-column type.
	 *
	 * @param rs The result set from which to extract value.
	 * @param name the column name making up this type value (use to read from result set)
	 * @param session The originating session
	 * @param owner the parent entity
	 *
	 * @return The extracted value
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 *
	 * todo : this form needs to go away.  Any form taking just one column name
	 */
	Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException;

	/**
	 * Bind a value represented by an instance of the {@link #getReturnedClass() mapped class} to the JDBC prepared
	 * statement, ignoring some columns as dictated by the 'settable' parameter.  Implementors should handle the
	 * possibility of null values.  A multi-column type should bind parameters starting from <tt>index</tt>.
	 *
	 * @param st The JDBC prepared statement to which to bind
	 * @param value the object to write
	 * @param index starting parameter bind index
	 * @param settable an array indicating which columns to bind/ignore
	 * @param session The originating session
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 */
	void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

	/**
	 * Bind a value represented by an instance of the {@link #getReturnedClass() mapped class} to the JDBC prepared
	 * statement.  Implementors should handle possibility of null values.  A multi-column type should bind parameters
	 * starting from <tt>index</tt>.
	 *
	 * @param st The JDBC prepared statement to which to bind
	 * @param value the object to write
	 * @param index starting parameter bind index
	 * @param session The originating session
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 */
	void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

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
	Object hydrate(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
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
	Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException;

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
	Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner)
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary inclusions from the legacy Type contract
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	default boolean isAssociationType() {
		return getClassification() == Classification.ENTITY
				|| getClassification() == Classification.MAPPED_SUPERCLASS
				|| getClassification() == Classification.ANY;
	}

	default boolean isCollectionType() {
		return getClassification() == Classification.COLLECTION;
	}

	default boolean isEntityType() {
		return getClassification() == Classification.ENTITY
				|| getClassification() == Classification.MAPPED_SUPERCLASS;
	}

	default boolean isAnyType() {
		return getClassification() == Classification.ANY;
	}

	default boolean isComponentType() {
		return getClassification() == Classification.COMPOSITE;
	}

	default Class getReturnedClass() {
		return getJavaTypeDescriptor().getJavaType();
	}

	boolean[] toColumnNullness(Object value);

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state) taking a shortcut for entity references.
	 * <p/>
	 * For most types this should equate to an {@link Object#equals} check on the values.  For associations
	 * the implication is a bit different.  For most types it is conceivable to simply delegate to {@link #isEqual}
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if they are considered the same (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isSame(T x, T y) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state).
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
	boolean isEqual(T x, T y) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state).
	 * <p/>
	 * This should always equate to some form of comparison of the value's internal state.  As an example, for
	 * something like a date the comparison should be based on its internal "time" state based on the specific portion
	 * it is meant to represent (timestamp, date, time).
	 *
	 * @param x The first value
	 * @param y The second value
	 * @param factory The session factory
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isEqual(T x, T y, SessionFactoryImplementor factory) throws HibernateException;

	/**
	 * Are objects of this type mutable. (With respect to the referencing object ...
	 * entities and collections are considered immutable because they manage their
	 * own internal state.)
	 *
	 * @return boolean
	 */
	boolean isMutable();

	/**
	 * Return a deep copy of the persistent state, stopping at entities and at collections.
	 *
	 * @param value The value to be copied
	 * @param factory The session factory
	 *
	 * @return The deep copy
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	T deepCopy(T value, SessionFactoryImplementor factory);

	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param original the value from the detached entity being merged
	 * @param target the value in the managed entity
	 * @param session The originating session
	 * @param owner The owner of the value
	 * @param copyCache The cache of already copied/replaced values
	 *
	 * @return the value to be merged
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	T replace(
			T original,
			T target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache) throws HibernateException;

	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param original the value from the detached entity being merged
	 * @param target the value in the managed entity
	 * @param session The originating session
	 * @param owner The owner of the value
	 * @param copyCache The cache of already copied/replaced values
	 * @param foreignKeyDirection For associations, which direction does the foreign key point?
	 *
	 * @return the value to be merged
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	T replace(
			T original,
			T target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException;

	/**
	 * Reconstruct the object from its disassembled state.  This method is the reciprocal of {@link #disassemble}
	 *
	 * @param cached the disassembled state from the cache
	 * @param session the originating session
	 * @param owner the parent entity object
	 *
	 * @return the (re)assembled object
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException;

	/**
	 * Return a disassembled representation of the object.  This is the value Hibernate will use in second level
	 * caching, so care should be taken to break values down to their simplest forms; for entities especially, this
	 * means breaking them down into their constituent parts.
	 *
	 * @param value the value to cache
	 * @param session the originating session
	 * @param owner optional parent entity object (needed for collections)
	 *
	 * @return the disassembled, deep cloned state
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	Serializable disassemble(T value, SharedSessionContractImplementor session, Object owner) throws HibernateException;

	/**
	 * Should the parent be considered dirty, given both the old and current value?
	 *
	 * @param old the old value
	 * @param current the current value
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field is dirty
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Should the parent be considered dirty, given both the old and current value?
	 *
	 * @param oldState the old value
	 * @param currentState the current value
	 * @param checkable An array of booleans indicating which columns making up the value are actually checkable
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field is dirty
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Has the value been modified compared to the current database state?  The difference between this
	 * and the {@link #isDirty} methods is that here we need to account for "partially" built values.  This is really
	 * only an issue with association types.  For most type implementations it is enough to simply delegate to
	 * {@link #isDirty} here/
	 *
	 * @param dbState the database state, in a "hydrated" form, with identifiers unresolved
	 * @param currentState the current state of the object
	 * @param checkable which columns are actually updatable
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field has been modified
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isModified(
			Object dbState,
			Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link Object#hashCode hashCode}.
	 *
	 * @param value The value for which to retrieve a hash code
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	int getHashCode(T value) throws HibernateException;

	/**
	 * Return the JDBC types codes (per {@link java.sql.Types}) for the columns mapped by this type.
	 * <p/>
	 * NOTE: The number of elements in this array matches the return from {@link #getColumnSpan}.
	 *
	 * @return The JDBC type codes.
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	int[] sqlTypes() throws MappingException;
}
