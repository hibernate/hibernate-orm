/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines a mapping between a Java type and one or more JDBC {@linkplain java.sql.Types types},
 * as well as describing the in-memory semantics of the given Java type, including:
 * <ul>
 *     <li>how to compare values and check for "dirtiness",
 *     <li>how to clone values, and
 *     <li>how to assemble/disassemble values for storage in the second-level cache.
 * </ul>
 * <p>
 * An application-defined custom types could, in principle, implement this interface directly,
 * but it's safer to implement the more stable interface {@link org.hibernate.usertype.UserType}.
 * <p>
 * An implementation of this interface must certainly be thread-safe. Ideally, it should also be
 * immutable.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Internal
public interface Type extends Serializable {
	/**
	 * Return true if the implementation is castable to {@link AssociationType}. This does not
	 * necessarily imply that the type actually represents an association. Shortcut for
	 * {@code type instanceof AssociationType}.
	 *
	 * @return True if this type is also an {@link AssociationType} implementor; false otherwise.
	 */
	boolean isAssociationType();

	/**
	 * Return true if the implementation is castable to {@link CollectionType}. Shortcut for
	 * {@code type instanceof CollectionType}
	 * <p>
	 * A {@link CollectionType} is additionally an {@link AssociationType}; so if this method
	 * returns true, {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also a {@link CollectionType} implementor; false otherwise.
	 */
	boolean isCollectionType();

	/**
	 * Return true if the implementation is castable to {@link EntityType}. Shortcut for
	 * {@code type instanceof EntityType}.
	 * <p>
	 * An {@link EntityType} is additionally an {@link AssociationType}; so if this method
	 * returns true, {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link EntityType} implementor; false otherwise.
	 */
	boolean isEntityType();

	/**
	 * Return true if the implementation is castable to {@link AnyType}. Shortcut for
	 * {@code type instanceof AnyType}.
	 * <p>
	 * An {@link AnyType} is additionally an {@link AssociationType}; so if this method
	 * returns true, then {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link AnyType} implementor; false otherwise.
	 */
	boolean isAnyType();

	/**
	 * Return true if the implementation is castable to {@link CompositeType}. Shortcut for
	 * {@code type instanceof CompositeType}.
	 * <p>
	 * A component type may own collections or associations and hence must provide certain
	 * extra functionality.
	 *
	 * @return True if this type is also a {@link CompositeType} implementor; false otherwise.
	 */
	boolean isComponentType();

	/**
	 * How many columns are used to persist this type?
	 * <p>
	 * Always the same as {@link #getSqlTypeCodes(Mapping) getSqlTypCodes(mapping).length}.
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The number of columns
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	int getColumnSpan(Mapping mapping) throws MappingException;

	/**
	 * Return the JDBC types codes as defined by {@link java.sql.Types} or {@link SqlTypes}
	 * for the columns mapped by this type.
	 * <p>
	 * The number of elements in this array must match the return from {@link #getColumnSpan}.
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The JDBC type codes.
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	int[] getSqlTypeCodes(Mapping mapping) throws MappingException;

	/**
	 * The class handled by this type.
	 *
	 * @return The Java class handled by this type.
	 */
	Class<?> getReturnedClass();

	/**
	 * The qualified name of the class handled by this type.
	 *
	 * @return The qualified Java class name.
	 *
	 * @since 6.5
	 */
	default String getReturnedClassName() {
		return getReturnedClass().getName();
	}

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality",
	 * that is, equality of persistent state, taking a shortcut for entity references.
	 * <p>
	 * For most types this should boil down to an {@linkplain Object#equals equality}
	 * comparison of the given values, and it's reasonable to simply delegate to
	 * {@link #isEqual(Object, Object)}. But for associations the semantics are a bit
	 * different.
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered the same (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isSame(@Nullable Object x, @Nullable Object y) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality",
	 * that is, equality of persistent state. For most types this could simply delegate to
	 * {@link java.util.Objects#equals(Object, Object) equals()}.
	 * <p>
	 * This should always equate to some form of comparison of the value's internal state.
	 * As an example, for Java's {@link java.util.Date} class, the comparison should be of
	 * its internal state, but based only on the specific part which is persistent (the
	 * timestamp, date, or time).
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isEqual(@Nullable Object x, @Nullable Object y) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality",
	 * that is, equality of persistent state. For most types this could simply delegate to
	 * {@link #isEqual(Object, Object)}.
	 * <p>
	 * This should always equate to some form of comparison of the value's internal state.
	 * As an example, for Java's {@link java.util.Date} class, the comparison should be of
	 * its internal state, but based only on the specific part which is persistent (the
	 * timestamp, date, or time).
	 *
	 * @param x The first value
	 * @param y The second value
	 * @param factory The session factory
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isEqual(@Nullable Object x, @Nullable Object y, SessionFactoryImplementor factory) throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality". For most types this could
	 * simply delegate to the given value's {@link Object#hashCode() hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	int getHashCode(Object x) throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  For most types this could
	 * simply delegate to {@link #getHashCode(Object)}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @param factory The session factory
	 *
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException;

	/**
	 * Perform a {@link java.util.Comparator}-style comparison of the given values.
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return The comparison result.
	 *
	 * @see java.util.Comparator#compare(Object, Object)
	 */
	int compare(@Nullable Object x, @Nullable Object y);

	int compare(@Nullable Object x, @Nullable Object y, SessionFactoryImplementor sessionFactory);

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
	boolean isDirty(@Nullable Object old, @Nullable Object current, SharedSessionContractImplementor session) throws HibernateException;

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
	boolean isDirty(@Nullable Object oldState, @Nullable Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Has the value been modified compared to the current database state?  The difference
	 * between this and the {@link #isDirty} methods is that here we need to account for
	 * "partially" built values. This is really only an issue with association types. For
	 * most type implementations it is enough to simply delegate to {@link #isDirty}.
	 *
	 * @param dbState the database state, in a "hydrated" form, with identifiers unresolved
	 * @param currentState the current state of the object
	 * @param checkable which columns are actually checkable
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field has been modified
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isModified(
			@Nullable Object dbState,
			@Nullable Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Bind a value represented by an instance of the {@link #getReturnedClass() mapped class}
	 * to the given JDBC {@link PreparedStatement}, ignoring some columns as dictated by the
	 * {@code settable} parameter. Implementors should handle the possibility of null values.
	 * A multi-column type should bind parameters starting from {@code index}.
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
			@Nullable Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session)
	throws HibernateException, SQLException;

	/**
	 * Bind a value represented by an instance of the {@link #getReturnedClass() mapped class}
	 * to the given JDBC {@link PreparedStatement}, ignoring some columns as dictated by the
	 * {@code settable} parameter. Implementors should handle the possibility of null values.
	 * A multi-column type should bind parameters starting from {@code index}.
	 *
	 * @param st The JDBC prepared statement to which to bind
	 * @param value the object to write
	 * @param index starting parameter bind index
	 * @param session The originating session
	 *
	 * @throws HibernateException An error from Hibernate
	 * @throws SQLException An error from the JDBC driver
	 */
	void nullSafeSet(PreparedStatement st, @Nullable Object value, int index, SharedSessionContractImplementor session)
	throws HibernateException, SQLException;

	/**
	 * Generate a representation of the given value for logging purposes.
	 *
	 * @param value The value to be logged
	 * @param factory The session factory
	 *
	 * @return The loggable representation
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	String toLoggableString(@Nullable Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * Returns the abbreviated name of the type.
	 *
	 * @return the Hibernate type name
	 */
	String getName();

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
	@Nullable Object deepCopy(@Nullable Object value, SessionFactoryImplementor factory)
			throws HibernateException;

	/**
	 * Are objects of this type mutable with respect to the referencing object?
	 * Entities and collections are considered immutable because they manage their
	 * own internal state.
	 *
	 * @return boolean
	 */
	boolean isMutable();

	/**
	 * Return a disassembled representation of the object. This is the representation that
	 * is stored in the second-level cache.
	 * <p>
	 * A reference to an associated entity should be disassembled to its primary key value.
	 * <p>
	 * A high-quality implementation of this method should ensure that:
	 * <pre>
	 * {@code Objects.equals(disassemble(x,s), disassemble(y,s))} == isEqual(x,y,sf)
	 * </pre>
	 * <p>
	 * and that:
	 * <pre>
	 * {@code Objects.equals(x, assemble(disassemble(x,s),s,o))}
	 * </pre>
	 * <p>
	 * That is, the implementation must be consistent with
	 * {@link #isEqual(Object, Object, SessionFactoryImplementor)} and with
	 * {@link #assemble(Serializable, SharedSessionContractImplementor, Object)}.
	 *
	 * @param value the value to cache
	 * @param sessionFactory the session factory
	 *
	 * @return the disassembled, deep cloned state
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	default @Nullable Serializable disassemble(@Nullable Object value, SessionFactoryImplementor sessionFactory) throws HibernateException {
		return disassemble( value, null, null );
	}

	/**
	 * Return a disassembled representation of the object. This is the representation that
	 * is stored in the second-level cache.
	 * <p>
	 * A reference to an associated entity should be disassembled to its primary key value.
	 *
	 * @param value the value to cache
	 * @param session the originating session
	 * @param owner optional parent entity object (needed for collections)
	 *
	 * @return the disassembled, deep cloned state
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	@Nullable Serializable disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session, @Nullable Object owner) throws HibernateException;

	/**
	 * Reconstruct the object from its disassembled state. This function is the inverse of
	 * {@link #disassemble(Object, SharedSessionContractImplementor, Object)}.
	 *
	 * @param cached the disassembled state from the cache
	 * @param session the originating session
	 * @param owner the parent entity object
	 *
	 * @return the (re)assembled object
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	@Nullable Object assemble(@Nullable Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException;

	/**
	 * Called before assembling a query result set from the query cache, to allow batch
	 * fetching of entities missing from the second-level cache.
	 *
	 * @param cached The key
	 * @param session The originating session
	 * @deprecated Is not called anymore
	 */
	@Deprecated(forRemoval = true, since = "6.6")
	void beforeAssemble(Serializable cached, SharedSessionContractImplementor session);

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
	@Nullable Object replace(
			@Nullable Object original,
			@Nullable Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache) throws HibernateException;

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
	@Nullable Object replace(
			@Nullable Object original,
			@Nullable Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException;

	/**
	 * Given an instance of the type, return an array of {@code boolean} values indicating which
	 * mapped columns would be null.
	 *
	 * @param value an instance of the type
	 * @param mapping The mapping abstraction
	 *
	 * @return array indicating column nullness for a value instance
	 */
	boolean[] toColumnNullness(@Nullable Object value, Mapping mapping);
}
