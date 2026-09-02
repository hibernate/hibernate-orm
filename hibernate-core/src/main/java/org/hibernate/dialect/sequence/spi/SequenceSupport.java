/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.spi;

import org.hibernate.MappingException;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines database sequence value expressions and lifecycle DDL for a
/// [Dialect].
///
/// Implement every database-specific difference as one stable, thread-safe
/// strategy. The default methods provide ordinary single-statement create,
/// drop, next-value, and restart grammar. Override the multi-statement methods
/// only when one logical operation requires more than one SQL command.
///
/// An increment size of zero is invalid. Preserve the optional sequence-options
/// fragment exactly once, and use `null` nowhere as a capability signal;
/// databases without sequences must use [SequenceSupports#none()].
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getSequenceSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface SequenceSupport {

	/// Report whether this strategy represents a database with sequences.
	///
	/// @return `true` when sequences are supported
	default boolean supportsSequences() {
		return true;
	}

	/// Report whether sequence creation accepts explicit initial and increment
	/// values.
	///
	/// @return `true` when pooled sequence definitions are supported
	/// @see #getCreateSequenceStrings(String, int, int)
	/// @see #getCreateSequenceString(String, int, int)
	default boolean supportsPooledSequences() {
		return supportsSequences();
	}

	/// Render the expression which obtains the next value inside another SQL
	/// statement. Do not include a surrounding `select` statement.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the next-value expression
	/// @throws MappingException when the operation is unsupported
	String getSelectSequenceNextValString(String sequenceName) throws MappingException;

	/// Render the expression which obtains the previous value inside another
	/// SQL statement. Override this method only when the database exposes such
	/// an expression.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the previous-value expression
	/// @throws MappingException when the operation is unsupported
	default String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		throw new UnsupportedOperationException( "No support for retrieving previous value" );
	}

	/// Render a stand-alone statement which obtains the next sequence value.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the next-value statement
	/// @throws MappingException when the operation is unsupported
	default String getSequenceNextValString(String sequenceName) throws MappingException {
		return "select " + getSelectSequenceNextValString( sequenceName ) + getFromDual();
	}

	/// Render a stand-alone statement which obtains the previous sequence value.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the previous-value statement
	/// @throws MappingException when the operation is unsupported
	default String getSequencePreviousValString(String sequenceName) throws MappingException {
		return "select " + getSelectSequencePreviousValString( sequenceName ) + getFromDual();
	}

	/// Return the optional relation fragment appended to default stand-alone
	/// value-selection statements, including any required leading whitespace.
	default String getFromDual() {
		return "";
	}

	/// Render a stand-alone next-value statement for databases whose selection
	/// syntax depends on the configured increment.
	///
	/// @param sequenceName the qualified sequence name
	/// @param increment the configured increment
	/// @return the next-value statement
	/// @throws MappingException when the operation is unsupported
	default String getSequenceNextValString(String sequenceName, int increment) throws MappingException {
		return getSequenceNextValString( sequenceName );
	}

	/// Render every command required to create a pooled sequence, appending a
	/// non-empty options fragment exactly once to the final default command.
	/// Override this method when creation requires multiple commands.
	///
	/// @param sequenceName the qualified sequence name
	/// @param initialValue the initial sequence value
	/// @param incrementSize the nonzero sequence increment
	/// @param options the optional trailing SQL fragment
	/// @return the ordered sequence-creation commands
	/// @throws MappingException when the operation is unsupported or the increment is zero
	default String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize, String options)
			throws MappingException {
		return new String[] {
				StringHelper.isNotEmpty( options ) ?
						getCreateSequenceString( sequenceName, initialValue, incrementSize ) + " " + options :
						getCreateSequenceString( sequenceName, initialValue, incrementSize ),

		};
	}

	/// Render every command required to create a pooled sequence without an
	/// options fragment.
	///
	/// @param sequenceName the qualified sequence name
	/// @param initialValue the initial sequence value
	/// @param incrementSize the nonzero sequence increment
	/// @return the ordered sequence-creation commands
	/// @throws MappingException when the operation is unsupported or the increment is zero
	default String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		return new String[] { getCreateSequenceString( sequenceName, initialValue, incrementSize ) };
	}

	/// Render the single command which creates a sequence with database-default
	/// allocation. Override the multi-command form instead when one logical
	/// creation requires multiple commands.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the sequence-creation command
	/// @throws MappingException when the operation is unsupported
	default String getCreateSequenceString(String sequenceName) throws MappingException {
		return "create sequence " + sequenceName;
	}

	/// Render the single command which creates a sequence with explicit
	/// allocation. Reject an increment of zero and include any required
	/// sign-sensitive starting bound returned by [#startingValue(int, int)].
	///
	/// @param sequenceName the qualified sequence name
	/// @param initialValue the initial sequence value
	/// @param incrementSize the nonzero sequence increment
	/// @return the sequence-creation command
	/// @throws MappingException when the operation is unsupported or the increment is zero
	default String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( incrementSize == 0 ) {
			throw new MappingException( "Unable to create the sequence [" + sequenceName + "]: the increment size must not be 0" );
		}
		return getCreateSequenceString( sequenceName )
				+ startingValue( initialValue, incrementSize )
				+ " start with " + initialValue
				+ " increment by " + incrementSize;
	}

	/// Render every command required to drop a sequence.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the ordered sequence-drop commands
	/// @throws MappingException when the operation is unsupported
	default String[] getDropSequenceStrings(String sequenceName) throws MappingException {
		return new String[]{ getDropSequenceString( sequenceName ) };
	}

	/// Render the single command which drops a sequence. Override the
	/// multi-command form instead when one logical drop requires several
	/// commands.
	///
	/// @param sequenceName the qualified sequence name
	/// @return the sequence-drop command
	/// @throws MappingException when the operation is unsupported
	default String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence " + sequenceName;
	}

	/// Render the DDL command which restarts a sequence at a specified value.
	///
	/// @param sequenceName the qualified sequence name
	/// @param startWith the value returned by the next sequence access
	/// @return the sequence-restart command
	default String getRestartSequenceString(String sequenceName, long startWith) {
		return "alter sequence " + sequenceName + " restart with " + startWith;
	}

	/// Report whether creation must state `minvalue` or `maxvalue` when the
	/// initial value and increment direction cross zero.
	default boolean sometimesNeedsStartingValue() {
		return false;
	}

	/// Render the sign-sensitive starting bound required by this strategy.
	/// Return an empty fragment when no explicit bound is needed.
	///
	/// @param initialValue the initial sequence value
	/// @param incrementSize the sequence increment
	/// @return a fragment including its own leading whitespace, or an empty string
	default String startingValue(int initialValue, int incrementSize) {
		if ( sometimesNeedsStartingValue() ) {
			if (incrementSize > 0 && initialValue <= 0) {
				return " minvalue " + initialValue;
			}
			if (incrementSize < 0 && initialValue >= 0) {
				return " maxvalue " + initialValue;
			}
		}
		return "";
	}

}
