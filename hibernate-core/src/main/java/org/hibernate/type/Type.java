/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.jdbc.Size;

import org.dom4j.Node;

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
	 * Return the JDBC types codes (per {@link java.sql.Types}) for the columns mapped by this type.
	 * <p/>
	 * NOTE: The number of elements in this array matches the return from {@link #getColumnSpan}.
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The JDBC type codes.
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	public int[] sqlTypes(Mapping mapping) throws MappingException;

	/**
	 * Return the column sizes dictated by this type.  For example, the mapping for a {@code char}/{@link Character} would
	 * have a dictated length limit of 1; for a string-based {@link java.util.UUID} would have a size limit of 36; etc.
	 * <p/>
	 * NOTE: The number of elements in this array matches the return from {@link #getColumnSpan}.
	 *
	 * @param mapping The mapping object :/
	 * @todo Would be much much better to have this aware of Dialect once the service/metamodel split is done
	 *
	 * @return The dictated sizes.
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	public Size[] dictatedSizes(Mapping mapping) throws MappingException;

	/**
	 * Defines the column sizes to use according to this type if the user did not explicitly say (and if no
	 * {@link #dictatedSizes} were given).
	 * <p/>
	 * NOTE: The number of elements in this array matches the return from {@link #getColumnSpan}.
	 *
	 * @param mapping The mapping object :/
	 * @todo Would be much much better to have this aware of Dialect once the service/metamodel split is done
	 *
	 * @return The default sizes.
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	public Size[] defaultSizes(Mapping mapping) throws MappingException;

	/**
	 * The class returned by {@link #nullSafeGet} methods. This is used to  establish the class of an array of
	 * this type.
	 *
	 * @return The java type class handled by this type.
	 */
	public Class getReturnedClass();

	/**
	 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	@Deprecated
	public boolean isXMLElement();

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state) taking a shortcut for entity references.
	 * <p/>
	 * For most types this should equate to an {@link Object#equals equals} check on the values.  For associations
	 * the implication is a bit different.  For most types it is conceivable to simply delegate to {@link #isEqual}
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered the same (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	public boolean isSame(Object x, Object y) throws HibernateException;

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
	public boolean isEqual(Object x, Object y) throws HibernateException;

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
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link Object#hashCode hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	public int getHashCode(Object x) throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link Object#hashCode hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @param factory The session factory
	 *
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	public int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException;
	
	/**
	 * Perform a {@link java.util.Comparator} style comparison between values
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return The comparison result.  See {@link java.util.Comparator#compare} for a discussion.
	 */
	public int compare(Object x, Object y);

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
	 * Extract a value of the {@link #getReturnedClass() mapped class} from the JDBC result set. Implementors
	 * should handle possibility of null values.
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
	 * @see Type#hydrate(ResultSet, String[], SessionImplementor, Object) alternative, 2-phase property initialization
	 */
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
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
	 */
	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
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
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session)
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
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
	throws HibernateException, SQLException;

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
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * A representation of the value to be embedded in an XML element.
	 *
	 * @param node The XML node to which to write the value
	 * @param value The value to write
	 * @param factory The session factory
	 *
	 * @throws HibernateException An error from Hibernate
	 *
	 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * Parse the XML representation of an instance.
	 *
	 * @param xml The XML node from which to read the value
	 * @param factory The session factory
	 *
	 * @return an instance of the {@link #getReturnedClass() mapped class}
	 *
	 * @throws HibernateException An error from Hibernate
	 *
	 * @deprecated To be removed in 5.  Removed as part of removing the notion of DOM entity-mode.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException;

	/**
	 * Returns the abbreviated name of the type.
	 *
	 * @return String the Hibernate type name
	 */
	public String getName();

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
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
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
	public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException;

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
	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
	throws HibernateException;
	
	/**
	 * Called before assembling a query result set from the query cache, to allow batch fetching
	 * of entities missing from the second-level cache.
	 *
	 * @param cached The key
	 * @param session The originating session
	 */
	public void beforeAssemble(Serializable cached, SessionImplementor session);

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
	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
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
	public Object resolve(Object value, SessionImplementor session, Object owner)
	throws HibernateException;
	
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
	public Object semiResolve(Object value, SessionImplementor session, Object owner)
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
	 * @param session The originating session
	 * @param owner The owner of the value
	 * @param copyCache The cache of already copied/replaced values
	 *
	 * @return the value to be merged
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
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
	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
			Object owner, 
			Map copyCache, 
			ForeignKeyDirection foreignKeyDirection) throws HibernateException;
	
	/**
	 * Given an instance of the type, return an array of boolean, indicating
	 * which mapped columns would be null.
	 * 
	 * @param value an instance of the type
	 * @param mapping The mapping abstraction
	 *
	 * @return array indicating column nullness for a value instance
	 */
	public boolean[] toColumnNullness(Object value, Mapping mapping);
	
}
