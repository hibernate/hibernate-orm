/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Defines a common API for dealing with data of integral data type.
 *
 * @author Steve Ebersole
 */
public interface IntegralDataTypeHolder extends Serializable {
	/**
	 * Initialize the internal value from the given primitive long.
	 *
	 * @param value The primitive integral value.
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder initialize(long value);

	/**
	 * Initialize the internal value from the given result set, using the specified default value
	 * if we could not get a value from the result set (aka result was null).
	 *
	 * @param resultSet The JDBC result set
	 * @param defaultValue The default value to use if we did not get a result set value.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws SQLException Any exception from accessing the result set
	 */
	IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException;

	/**
	 * Bind this holder's internal value to the given result set.
	 *
	 * @param preparedStatement The JDBC prepared statement
	 * @param position The position at which to bind
	 *
	 * @throws SQLException Any exception from accessing the statement
	 */
	void bind(PreparedStatement preparedStatement, int position) throws SQLException;

	/**
	 * Equivalent to a ++ operation
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder increment();

	/**
	 * Perform an addition
	 *
	 * @param addend The value to add to this integral.
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder add(long addend);

	/**
	 * Equivalent to a -- operation
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder decrement();

	/**
	 * Perform a subtraction
	 *
	 * @param subtrahend The value to subtract from this integral.
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder subtract(long subtrahend);

	/**
	 * Perform a multiplication.
	 *
	 * @param factor The factor by which to multiple this integral
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor);

	/**
	 * Perform a multiplication.
	 *
	 * @param factor The factor by which to multiple this integral
	 *
	 * @return {@code this}, for method chaining
	 */
	IntegralDataTypeHolder multiplyBy(long factor);

	/**
	 * Perform an equality comparison check
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if the two are equal
	 */
	boolean eq(IntegralDataTypeHolder other);

	/**
	 * Perform an equality comparison check
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if the two are equal
	 */
	boolean eq(long other);

	/**
	 * Perform a "less than" comparison check.  We check to see if our value is less than
	 * the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is less than the 'other' value.
	 */
	boolean lt(IntegralDataTypeHolder other);

	/**
	 * Perform a "less than" comparison check.  We check to see if our value is less than
	 * the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is less than the 'other' value.
	 */
	boolean lt(long other);

	/**
	 * Perform a "greater than" comparison check.  We check to see if our value is greater
	 * than the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is greater than the 'other' value.
	 */
	boolean gt(IntegralDataTypeHolder other);

	/**
	 * Perform a "greater than" comparison check.  We check to see if our value is greater
	 * than the incoming value...
	 *
	 * @param other The other value to check against our internal state
	 *
	 * @return True if our value is greater than the 'other' value.
	 */
	boolean gt(long other);

	/**
	 * Make a copy of this holder.
	 *
	 * @return The copy.
	 */
	IntegralDataTypeHolder copy();

	/**
	 * Return the internal value.
	 *
	 * @return The current internal value
	 */
	Number makeValue();

	/**
	 * Increment the internal state, but return the pre-incremented value.
	 *
	 * @return The pre-incremented internal value
	 */
	Number makeValueThenIncrement();

	/**
	 * Increment the internal state by the given addend, but return the pre-incremented value.
	 *
	 * @param addend The value to be added to our internal state
	 *
	 * @return The pre-incremented internal value
	 */
	Number makeValueThenAdd(long addend);

	/**
	 * Convert the internal value to {@code long}.
	 */
	long toLong();

	/**
	 * Convert the internal value to {@link BigInteger}.
	 */
	BigInteger toBigInteger();

	/**
	 * Convert the internal value to {@link BigDecimal}.
	 */
	BigDecimal toBigDecimal();
}
