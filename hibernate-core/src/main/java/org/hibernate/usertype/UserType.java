/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * This interface should be implemented by user-defined "types".
 * A "type" class is <em>not</em> the actual property type - it
 * is a class that knows how to serialize instances of another
 * class to and from JDBC.<br>
 * <br>
 * This interface
 * <ul>
 * <li>abstracts user code from future changes to the <tt>Type</tt>
 * interface,</li>
 * <li>simplifies the implementation of custom types and</li>
 * <li>hides certain "internal" interfaces from user code.</li>
 * </ul>
 * <br>
 * Implementors must be immutable and must declare a public
 * default constructor.<br>
 * <br>
 * The actual class mapped by a <tt>UserType</tt> may be just
 * about anything.<br>
 * <br>
 * <tt>CompositeUserType</tt> provides an extended version of
 * this interface that is useful for more complex cases.<br>
 * <br>
 * Alternatively, custom types could implement <tt>Type</tt>
 * directly or extend one of the abstract classes in
 * <tt>org.hibernate.type</tt>. This approach risks future
 * incompatible changes to classes or interfaces in that
 * package.
 *
 * @see CompositeUserType for more complex cases
 * @see org.hibernate.type.Type
 * @author Gavin King
 */
public interface UserType {

	/**
	 * Return the SQL type codes for the columns mapped by this type. The
	 * codes are defined on <tt>java.sql.Types</tt>.
	 * @see java.sql.Types
	 * @return int[] the typecodes
	 */
	int[] sqlTypes();

	/**
	 * The class returned by <tt>nullSafeGet()</tt>.
	 *
	 * @return Class
	 */
	Class returnedClass();

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality".
	 * Equality of the persistent state.
	 *
	 * @param x
	 * @param y
	 * @return boolean
	 */
	boolean equals(Object x, Object y) throws HibernateException;

	/**
	 * Get a hashcode for the instance, consistent with persistence "equality"
	 */
	int hashCode(Object x) throws HibernateException;

	/**
	 * Retrieve an instance of the mapped class from a JDBC resultset. Implementors
	 * should handle possibility of null values.
	 *
	 *
	 * @param rs a JDBC result set
	 * @param names the column names
	 * @param session
	 *@param owner the containing entity  @return Object
	 * @throws HibernateException
	 * @throws SQLException
	 */
	Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException;

	/**
	 * Write an instance of the mapped class to a prepared statement. Implementors
	 * should handle possibility of null values. A multi-column type should be written
	 * to parameters starting from <tt>index</tt>.
	 *
	 *
	 * @param st a JDBC prepared statement
	 * @param value the object to write
	 * @param index statement parameter index
	 * @param session
	 * @throws HibernateException
	 * @throws SQLException
	 */
	void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException;

	/**
	 * Return a deep copy of the persistent state, stopping at entities and at
	 * collections. It is not necessary to copy immutable objects, or null
	 * values, in which case it is safe to simply return the argument.
	 *
	 * @param value the object to be cloned, which may be null
	 * @return Object a copy
	 */
	Object deepCopy(Object value) throws HibernateException;

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
	 * @return a cachable representation of the object
	 * @throws HibernateException
	 */
	Serializable disassemble(Object value) throws HibernateException;

	/**
	 * Reconstruct an object from the cacheable representation. At the very least this
	 * method should perform a deep copy if the type is mutable. (optional operation)
	 *
	 * @param cached the object to be cached
	 * @param owner the owner of the cached object
	 * @return a reconstructed object from the cachable representation
	 * @throws HibernateException
	 */
	Object assemble(Serializable cached, Object owner) throws HibernateException;

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
	Object replace(Object original, Object target, Object owner) throws HibernateException;
}
