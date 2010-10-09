/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

/**
 * Defines a mapping between a Java type and one or more JDBC {@linkplain java.sql.Types types}, as well
 * as describing the in-memory semantics of the given java type (how do we check it for 'dirtiness', how do
 * we copy values, etc).
 * <p/>
 * Application developers needing custom types can implement this interface (either directly or via subclassing an
 * existing impl) or by the (slightly more stable, though more limited) {@link org.hibernate.usertype.UserType}
 * interface.
 * <p/>
 * Implementations of this interface must certainly be thread-safe.  It is recommended that they be immutable as
 * well, though that is difficult to achieve completely given the no-arg constructor requirement for custom types.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Type extends Serializable {
	/**
	 * Return true if the implementation is castable to {@link AssociationType}. This does not necessarily imply that
	 * the type actually represents an association.  Essentially a polymorphic version of
	 * {@code (type instanceof AssociationType.class)}
	 *
	 * @return True if this type is also an {@link AssociationType} implementor; false otherwise.
	 */
	public boolean isAssociationType();

	/**
	 * Return true if the implementation is castable to {@link CollectionType}. Essentially a polymorphic version of
	 * {@code (type instanceof CollectionType.class)}
	 * <p/>
	 * A {@link CollectionType} is additionally an {@link AssociationType}; so if this method returns true,
	 * {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link CollectionType} implementor; false otherwise.
	 */
	public boolean isCollectionType();

	/**
	 * Return true if the implementation is castable to {@link EntityType}. Essentially a polymorphic
	 * version of {@code (type instanceof EntityType.class)}.
	 * <p/>
	 * An {@link EntityType} is additionally an {@link AssociationType}; so if this method returns true,
	 * {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link EntityType} implementor; false otherwise.
	 */
	public boolean isEntityType();

	/**
	 * Return true if the implementation is castable to {@link AnyType}. Essentially a polymorphic
	 * version of {@code (type instanceof AnyType.class)}.
	 * <p/>
	 * An {@link AnyType} is additionally an {@link AssociationType}; so if this method returns true,
	 * {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link AnyType} implementor; false otherwise.
	 */
	public boolean isAnyType();

	/**
	 * Return true if the implementation is castable to {@link CompositeType}. Essentially a polymorphic
	 * version of {@code (type instanceof CompositeType.class)}.  A component type may own collections or
	 * associations and hence must provide certain extra functionality.
	 *
	 * @return True if this type is also an {@link CompositeType} implementor; false otherwise.
	 */
	public boolean isComponentType();

	/**
	 * Return the JDBC types codes (per {@link java.sql.Types}) for the columns mapped by this type.
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The JDBC type codes.
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	public int[] sqlTypes(Mapping mapping) throws MappingException;

	/**
	 * How many columns are used to persist this type.  Always the same as {@code sqlTypes(mapping).length}
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The number of columns
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	public int getColumnSpan(Mapping mapping) throws MappingException;

	/**
	 * The class returned by {@link #nullSafeGet} methods. This is used to  establish the class of an array of
	 * this type.
	 *
	 * @return The java type class handled by this type.
	 */
	public Class getReturnedClass();
	
	public boolean isXMLElement();

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state) taking a shortcut for entity references.
	 * <p/>
	 * For most types this should equate to {@link #equals} check on the values.  For associations the implication
	 * is a bit different.  For most types it is conceivable to simply delegate to {@link #isEqual}
	 *
	 * @param x The first value
	 * @param y The second value
	 * @param entityMode The entity mode of the values.
	 *
	 * @return True if there are considered the same (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	public boolean isSame(Object x, Object y, EntityMode entityMode) throws HibernateException;

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
	 * @param entityMode The entity mode of the values.
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	public boolean isEqual(Object x, Object y, EntityMode entityMode) throws HibernateException;

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
	 * @param entityMode The entity mode of the values.
	 * @param factory The session factory
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link #hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @param entityMode The entity mode of the value.
	 *
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	public int getHashCode(Object x, EntityMode entityMode) throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link #hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @param entityMode The entity mode of the value.
	 * @param factory The session factory
	 *
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) throws HibernateException;
	
	/**
	 * Perform a {@link java.util.Comparator} style comparison between values
	 *
	 * @param x The first value
	 * @param y The second value
	 * @param entityMode The entity mode of the values.
	 *
	 * @return The comparison result.  See {@link java.util.Comparator#compare} for a discussion.
	 */
	public int compare(Object x, Object y, EntityMode entityMode);

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
	public boolean isDirty(Object old, Object current, SessionImplementor session) throws HibernateException;

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
	public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SessionImplementor session)
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
	public boolean isModified(Object dbState, Object currentState, boolean[] checkable, SessionImplementor session)
			throws HibernateException;

	/**
	 * Retrieve an instance of the mapped class from a JDBC resultset. Implementors
	 * should handle possibility of null values.
	 *
	 * @see Type#hydrate(ResultSet, String[], SessionImplementor, Object) alternative, 2-phase property initialization
	 * @param rs
	 * @param names the column names
	 * @param session
	 * @param owner the parent entity
	 * @return Object
	 * @throws HibernateException
	 * @throws SQLException
	 */
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
	throws HibernateException, SQLException;

	/**
	 * Retrieve an instance of the mapped class from a JDBC resultset. Implementations
	 * should handle possibility of null values. This method might be called if the
	 * type is known to be a single-column type.
	 *
	 * @param rs
	 * @param name the column name
	 * @param session
	 * @param owner the parent entity
	 * @return Object
	 * @throws HibernateException
	 * @throws SQLException
	 */
	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
	throws HibernateException, SQLException;

	/**
	 * Write an instance of the mapped class to a prepared statement, ignoring some columns. 
	 * Implementors should handle possibility of null values. A multi-column type should be 
	 * written to parameters starting from <tt>index</tt>.
	 * @param st
	 * @param value the object to write
	 * @param index statement parameter index
	 * @param settable an array indicating which columns to ignore
	 * @param session
	 *
	 * @throws HibernateException
	 * @throws SQLException
	 */
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session)
	throws HibernateException, SQLException;

	/**
	 * Write an instance of the mapped class to a prepared statement. Implementors
	 * should handle possibility of null values. A multi-column type should be written
	 * to parameters starting from <tt>index</tt>.
	 * @param st
	 * @param value the object to write
	 * @param index statement parameter index
	 * @param session
	 *
	 * @throws HibernateException
	 * @throws SQLException
	 */
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
	throws HibernateException, SQLException;

	/**
	 * A representation of the value to be embedded in an XML element.
	 *
	 * @param value
	 * @param factory
	 * @return String
	 * @throws HibernateException
	 */
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * A representation of the value to be embedded in a log file.
	 *
	 * @param value
	 * @param factory
	 * @return String
	 * @throws HibernateException
	 */
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * Parse the XML representation of an instance.
	 * @param xml
	 * @param factory
	 *
	 * @return an instance of the type
	 * @throws HibernateException
	 */
	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException;

	/**
	 * Returns the abbreviated name of the type.
	 *
	 * @return String the Hibernate type name
	 */
	public String getName();

	/**
	 * Return a deep copy of the persistent state, stopping at entities and at
	 * collections.
	 * @param value generally a collection element or entity field
	 * @param entityMode 
	 * @param factory
	 * @return Object a copy
	 */
	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException;

	/**
	 * Are objects of this type mutable. (With respect to the referencing object ...
	 * entities and collections are considered immutable because they manage their
	 * own internal state.)
	 *
	 * @return boolean
	 */
	public boolean isMutable();

	/**
	 * Return a cacheable "disassembled" representation of the object.
	 * @param value the value to cache
	 * @param session the session
	 * @param owner optional parent entity object (needed for collections)
	 * @return the disassembled, deep cloned state
	 */
	public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException;

	/**
	 * Reconstruct the object from its cached "disassembled" state.
	 * @param cached the disassembled state from the cache
	 * @param session the session
	 * @param owner the parent entity object
	 * @return the the object
	 */
	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
	throws HibernateException;
	
	/**
	 * Called before assembling a query result set from the query cache, to allow batch fetching
	 * of entities missing from the second-level cache.
	 */
	public void beforeAssemble(Serializable cached, SessionImplementor session);

	/**
	 * Retrieve an instance of the mapped class, or the identifier of an entity or collection, 
	 * from a JDBC resultset. This is useful for 2-phase property initialization - the second 
	 * phase is a call to <tt>resolveIdentifier()</tt>.
	 * 
	 * @see Type#resolve(Object, SessionImplementor, Object)
	 * @param rs
	 * @param names the column names
	 * @param session the session
	 * @param owner the parent entity
	 * @return Object an identifier or actual value
	 * @throws HibernateException
	 * @throws SQLException
	 */
	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
	throws HibernateException, SQLException;

	/**
	 * Map identifiers to entities or collections. This is the second phase of 2-phase property 
	 * initialization.
	 * 
	 * @see Type#hydrate(ResultSet, String[], SessionImplementor, Object)
	 * @param value an identifier or value returned by <tt>hydrate()</tt>
	 * @param owner the parent entity
	 * @param session the session
	 * @return the given value, or the value associated with the identifier
	 * @throws HibernateException
	 */
	public Object resolve(Object value, SessionImplementor session, Object owner)
	throws HibernateException;
	
	/**
	 * Given a hydrated, but unresolved value, return a value that may be used to
	 * reconstruct property-ref associations.
	 */
	public Object semiResolve(Object value, SessionImplementor session, Object owner)
	throws HibernateException;
	
	/**
	 * Get the type of a semi-resolved value.
	 */
	public Type getSemiResolvedType(SessionFactoryImplementor factory);

	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param original the value from the detached entity being merged
	 * @param target the value in the managed entity
	 * @return the value to be merged
	 */
	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
			Object owner, 
			Map copyCache)
	throws HibernateException;
	
	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param original the value from the detached entity being merged
	 * @param target the value in the managed entity
	 * @return the value to be merged
	 */
	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
			Object owner, 
			Map copyCache, 
			ForeignKeyDirection foreignKeyDirection)
	throws HibernateException;
	
	/**
	 * Given an instance of the type, return an array of boolean, indicating
	 * which mapped columns would be null.
	 * 
	 * @param value an instance of the type
	 */
	public boolean[] toColumnNullness(Object value, Mapping mapping);
	
}






