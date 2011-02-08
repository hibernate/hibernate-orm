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
package org.hibernate.id;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Defines a common api for dealing with data of integral data type.
 *
 * @author Steve Ebersole
 */
public interface IntegralDataTypeHolder extends Serializable {
	/**
	 * Initialize the internal value from the given primitive long.
	 *
	 * @param value The primitive integral value.
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder initialize(long value);

	/**
	 * Initialize the internal value from the given result set, using the specified default value
	 * if we could not get a value from the result set (aka result was null).
	 *
	 * @param resultSet The JDBC result set
	 * @param defaultValue The default value to use if we did not get a result set value.
	 *
	 * @return <tt>this</tt>, for method chaining
	 *
	 * @throws SQLException Any exception from accessing the result set
	 */
	public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException;

	/**
	 * Bind this holders internal value to the given result set.
	 *
	 * @param preparedStatement The JDBC prepared statement
	 * @param position The position at which to bind
	 *
	 * @throws SQLException Any exception from accessing the statement
	 */
	public void bind(PreparedStatement preparedStatement, int position) throws SQLException;

	/**
	 * Equivalent to a ++ operation
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder increment();

	/**
	 * Perform an addition
	 *
	 * @param addend The value to add to this integral.
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder add(long addend);

	/**
	 * Equivalent to a -- operation
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder decrement();

	/**
	 * Perform a subtraction
	 *
	 * @param subtrahend The value to subtract from this integral.
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder subtract(long subtrahend);

	/**
	 * Perform a multiplication.
	 *
	 * @param factor The factor by which to multiple this integral
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor);

	/**
	 * Perform a multiplication.
	 *
	 * @param factor The factor by which to multiple this integral
	 *
	 * @return <tt>this</tt>, for method chaining
	 */
	public IntegralDataTypeHolder multiplyBy(long factor);

	/**
	 * Perform an equality comparison check
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if the two are equal
	 */
	public boolean eq(IntegralDataTypeHolder other);

	/**
	 * Perform an equality comparison check
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if the two are equal
	 */
	public boolean eq(long other);

	/**
	 * Perform a "less than" comparison check.  We check to see if our value is less than
	 * the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is less than the 'other' value.
	 */
	public boolean lt(IntegralDataTypeHolder other);

	/**
	 * Perform a "less than" comparison check.  We check to see if our value is less than
	 * the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is less than the 'other' value.
	 */
	public boolean lt(long other);

	/**
	 * Perform a "greater than" comparison check.  We check to see if our value is greater
	 * than the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is greater than the 'other' value.
	 */
	public boolean gt(IntegralDataTypeHolder other);

	/**
	 * Perform a "greater than" comparison check.  We check to see if our value is greater
	 * than the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is greater than the 'other' value.
	 */
	public boolean gt(long other);

	/**
	 * Make a copy of this holder.
	 *
	 * @return The copy.
	 */
	public IntegralDataTypeHolder copy();

	/**
	 * Return the internal value.
	 *
	 * @return The current internal value
	 */
	public Number makeValue();

	/**
	 * Increment the internal state, but return the pre-incremented value.
	 *
	 * @return The pre-incremented internal value
	 */
	public Number makeValueThenIncrement();

	/**
	 * Increment the internal state by the given addend, but return the pre-incremented value.
	 *
	 * @param addend The value to be added to our internal state
	 *
	 * @return The pre-incremented internal value
	 */
	public Number makeValueThenAdd(long addend);
}
