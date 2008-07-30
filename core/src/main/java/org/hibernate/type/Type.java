/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
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
 * Defines a mapping from a Java type to an JDBC datatype. This interface is intended to
 * be implemented by applications that need custom types.<br>
 * <br>
 * Implementors should usually be immutable and <b>must</b> certainly be threadsafe.
 *
 * @author Gavin King
 */
public interface Type extends Serializable {

	/**
	 * Return true if the implementation is castable to
	 * <tt>AssociationType</tt>. This does not necessarily imply that
	 * the type actually represents an association.
	 * @see AssociationType
	 * @return boolean
	 */
	public boolean isAssociationType();
	/**
	 * Is this type a collection type.
	 */
	public boolean isCollectionType();

	/**
	 * Is this type a component type. If so, the implementation
	 * must be castable to <tt>AbstractComponentType</tt>. A component
	 * type may own collections or associations and hence must provide
	 * certain extra functionality.
	 * @see AbstractComponentType
	 * @return boolean
	 */
	public boolean isComponentType();

	/**
	 * Is this type an entity type?
	 * @return boolean
	 */
	public boolean isEntityType();

	/**
	 * Is this an "any" type.
	 *
	 * i.e. a reference to a persistent entity
	 * that is not modelled as a (foreign key) association.
	 */
	public boolean isAnyType();
	
	public boolean isXMLElement();

	/**
	 * Return the SQL type codes for the columns mapped by this type. The codes
	 * are defined on <tt>java.sql.Types</tt>.
	 * @see java.sql.Types
	 * @return the typecodes
	 * @throws MappingException
	 */
	public int[] sqlTypes(Mapping mapping) throws MappingException;

	/**
	 * How many columns are used to persist this type.
	 */
	public int getColumnSpan(Mapping mapping) throws MappingException;

	/**
	 * The class returned by <tt>nullSafeGet()</tt> methods. This is used to 
	 * establish the class of an array of this type.
	 *
	 * @return Class
	 */
	public Class getReturnedClass();

	/**
	 * Compare two instances of the class mapped by this type for persistence
	 * "equality" - equality of persistent state - taking a shortcut for
	 * entity references.
	 * @param x
	 * @param y
	 * @param entityMode
	 *
	 * @return boolean
	 * @throws HibernateException
	 */
	public boolean isSame(Object x, Object y, EntityMode entityMode) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence
	 * "equality" - equality of persistent state.
	 * @param x
	 * @param y
	 * @param entityMode 
	 *
	 * @return boolean
	 * @throws HibernateException
	 */
	public boolean isEqual(Object x, Object y, EntityMode entityMode) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence
	 * "equality" - equality of persistent state.
	 * @param x
	 * @param y
	 * @param entityMode 
	 *
	 * @return boolean
	 * @throws HibernateException
	 */
	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException;

	/**
	 * Get a hashcode, consistent with persistence "equality"
	 * @param x
	 * @param entityMode 
	 */
	public int getHashCode(Object x, EntityMode entityMode) throws HibernateException;

	/**
	 * Get a hashcode, consistent with persistence "equality"
	 * @param x
	 * @param entityMode 
	 * @param factory
	 */
	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException;
	
	/**
	 * compare two instances of the type
	 * @param entityMode 
	 */
	public int compare(Object x, Object y, EntityMode entityMode);

	/**
	 * Should the parent be considered dirty, given both the old and current field or 
	 * element value?
	 * 
	 * @param old the old value
	 * @param current the current value
	 * @param session
	 * @return true if the field is dirty
	 */
	public boolean isDirty(Object old, Object current, SessionImplementor session)
	throws HibernateException;
	/**
	 * Should the parent be considered dirty, given both the old and current field or 
	 * element value?
	 * 
	 * @param old the old value
	 * @param current the current value
	 * @param checkable which columns are actually updatable
	 * @param session
	 * @return true if the field is dirty
	 */
	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
	throws HibernateException;

	/**
	 * Has the parent object been modified, compared to the current database state?
	 * @param oldHydratedState the database state, in a "hydrated" form, with identifiers unresolved
	 * @param currentState the current state of the object
	 * @param checkable which columns are actually updatable
	 * @param session
	 * @return true if the field has been modified
	 */
	public boolean isModified(Object oldHydratedState, Object currentState, boolean[] checkable, SessionImplementor session)
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






