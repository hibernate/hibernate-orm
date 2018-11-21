/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Custom type mapping between a java-type and a sql-type.
 *
 * @implSpec In additional to being deprecated, support for these UserTypes has
 * been limited to just basic type mappings; specifically, support for multi-column
 * mappings via UserType has been removed - use {@link javax.persistence.Embeddable}
 * instead
 *
 * @see org.hibernate.annotations.SqlType
 * @see org.hibernate.annotations.SqlTypeDescriptor
 * @see org.hibernate.annotations.JavaTypeDescriptor
 * @see org.hibernate.annotations.Type
 * @see org.hibernate.annotations.TypeDef
 * @see org.hibernate.annotations.Immutable
 * @see org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry
 * @see org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry
 * @see org.hibernate.boot.model.TypeContributions#contributeSqlTypeDescriptor
 * @see org.hibernate.boot.model.TypeContributions#contributeJavaTypeDescriptor
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated This package is considered deprecated.  This
 * can all be achieved through some combination of: <ul>
 *     <li>{@link org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor}</li>
 *     <li>{@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor}</li>
 *     <li>{@link javax.persistence.AttributeConverter}</li>
 * </ul>
 */
@Deprecated
public interface UserType {

	/**
	 * The SQL type code for the {@link SqlTypeDescriptor} to use - the key into
	 * {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry}.
	 * Generally speaking, defined by {@link java.sql.Types} but may be
	 * extension codes
	 */
	int sqlTypeCode();

	/**
	 * The class returned by <tt>nullSafeGet()</tt>.
	 *
	 * @return Class
	 */
	Class returnedClass();

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality".
	 * Equality of the persistent state.
	 */
	boolean equals(Object x, Object y) throws HibernateException;

	/**
	 * Get a hashcode for the instance, consistent with persistence "equality"
	 */
	int hashCode(Object x) throws HibernateException;

	/**
	 * Extract an instance of the mapped class from a JDBC ResultSet. Implementors
	 * should handle possibility of null values.
	 */
	Object nullSafeGet(ResultSet rs, int parameterPosition, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

	/**
	 * Write an instance of the mapped class to a prepared statement. Implementors
	 * should handle possibility of null values.
	 *
	 * @param st a JDBC prepared statement
	 * @param value the object to write
	 * @param index statement parameter index
	 */
	void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

	/**
	 * Are objects of this type mutable?
	 *
	 * @return boolean
	 */
	boolean isMutable();

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
}
