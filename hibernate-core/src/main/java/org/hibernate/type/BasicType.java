/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Marker interface for basic types.
 *
 * @author Steve Ebersole
 *
 * @deprecated This package is considered deprecated.  This
 * can all be achieved through some combination of: <ul>
 *     <li>{@link org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor} - custom or standard</li>
 *     <li>{@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor} - custom or standard</li>
 *     <li>{@link javax.persistence.AttributeConverter}</li>
 * </ul>
 */
@Deprecated
public interface BasicType {
	/**
	 * Returns the abbreviated name of the type.
	 *
	 * @return String the Hibernate type name
	 */
	String getName();

	/**
	 * Get the names under which this type should be registered in the type registry.
	 *
	 * @return The keys under which to register this type.
	 */
	String[] getRegistrationKeys();

	/**
	 * Descriptor for the Java type
	 */
	BasicJavaDescriptor getJavaTypeDescriptor();

	/**
	 * Descriptor for the SQL type
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();

	/**
	 * Extract an instance of the mapped class from a JDBC ResultSet.
	 *
	 * @implNote Implementors should handle possibility of null values.
	 */
	Object nullSafeGet(ResultSet rs, int parameterPosition, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

	/**
	 * Bind a value to the JDBC prepared statement.  Implementors should handle
	 * possibility of null values.
	 */
	void nullSafeSet(PreparedStatement st, Object value, int parameterPosition, SharedSessionContractImplementor session)
			throws HibernateException, SQLException;

	/**
	 * Are instances of this described type mutable.
	 */
	boolean isMutable();

	/**
	 * Return a deep copy of the basic value
	 */
	Object deepCopy(Object value) throws HibernateException;

	/**
	 * Return a disassembled representation of the object.  This is the value
	 * Hibernate will use in second level caching, so care should be taken to
	 * break values down into a "shareable" form
	 */
	Serializable disassemble(Object value) throws HibernateException;

	/**
	 * Reconstruct the object from its disassembled state - the reciprocal of
	 * {@link #disassemble}
	 */
	Object assemble(Serializable cached) throws HibernateException;

	default boolean areEqual(Object one, Object another) {
		return Objects.equals( one, another );
	}

	/**
	 * Generate a representation of the value for logging purposes.
	 */
	String toLoggableString(Object value) throws HibernateException;
}
