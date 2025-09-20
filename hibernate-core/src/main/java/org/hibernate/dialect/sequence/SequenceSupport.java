/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;

/**
 * A set of operations providing support for sequences in a
 * certain {@link org.hibernate.dialect.Dialect SQL dialect}.
 *
 * @author Gavin King
 */
public interface SequenceSupport {

	/**
	 * Does this dialect support sequences?
	 *
	 * @return True if sequences supported; false otherwise.
	 */
	default boolean supportsSequences() {
		return true;
	}

	/**
	 * Does this dialect support "pooled" sequences.  Not aware of a better
	 * name for this.  Essentially can we specify the initial and increment values?
	 *
	 * @return True if such "pooled" sequences are supported; false otherwise.
	 * @see #getCreateSequenceStrings(String, int, int)
	 * @see #getCreateSequenceString(String, int, int)
	 */
	default boolean supportsPooledSequences() {
		return supportsSequences();
	}

	/**
	 * Generate the select expression fragment that will retrieve the next
	 * value of a sequence as part of another (typically DML) statement.
	 * <p>
	 * This differs from {@link #getSequenceNextValString(String)} in that
	 * it must return an expression usable within another statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return The "next value" fragment.
	 * @throws MappingException If sequences are not supported.
	 */
	String getSelectSequenceNextValString(String sequenceName) throws MappingException;

	/**
	 * Generate the select expression fragment that will retrieve the previous
	 * value of a sequence as part of another (typically DML) statement.
	 * <p>
	 * This differs from {@link #getSequencePreviousValString(String)} in that
	 * it must return an expression usable within another statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return The "previous value" fragment.
	 * @throws MappingException If sequences are not supported.
	 */
	default String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		throw new UnsupportedOperationException( "No support for retrieving previous value" );
	}

	/**
	 * Generate the appropriate select statement to to retrieve the next value
	 * of a sequence.
	 * <p>
	 * This should be a stand alone select statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return String The select "next value" statement.
	 * @throws MappingException If sequences are not supported.
	 */
	default String getSequenceNextValString(String sequenceName) throws MappingException {
		return "select " + getSelectSequenceNextValString( sequenceName ) + getFromDual();
	}

	/**
	 * Generate the appropriate select statement to to retrieve the previous value
	 * of a sequence.
	 * <p>
	 * This should be a stand alone select statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return String The select "previous value" statement.
	 * @throws MappingException If sequences are not supported.
	 */
	default String getSequencePreviousValString(String sequenceName) throws MappingException {
		return "select " + getSelectSequencePreviousValString( sequenceName ) + getFromDual();
	}

	default String getFromDual() {
		return "";
	}

	/**
	 * Generate the appropriate select statement to to retrieve the next value
	 * of a sequence.
	 * <p>
	 * This should be a stand alone select statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @param increment the increment, in case it needs to be passed explicitly
	 * @return String The select "next value" statement.
	 * @throws MappingException If sequences are not supported.
	 */
	default String getSequenceNextValString(String sequenceName, int increment) throws MappingException {
		return getSequenceNextValString( sequenceName );
	}

	/**
	 * An optional multi-line form for databases which {@link #supportsPooledSequences()}.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @param options A SQL fragment appended to the generated DDL.
	 * @return The sequence creation commands
	 * @throws MappingException If sequences are not supported.
	 */
	default String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize, String options)
			throws MappingException {
		return new String[] {
				StringHelper.isNotEmpty( options ) ?
						getCreateSequenceString( sequenceName, initialValue, incrementSize ) + " " + options :
						getCreateSequenceString( sequenceName, initialValue, incrementSize ),

		};
	}

	/**
	 * An optional multi-line form for databases which {@link #supportsPooledSequences()}.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @return The sequence creation commands
	 * @throws MappingException If sequences are not supported.
	 */
	default String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		return new String[] { getCreateSequenceString( sequenceName, initialValue, incrementSize ) };
	}

	/**
	 * Typically, dialects which support sequences can create a sequence with
	 * a single command. This method is a convenience making it easier to
	 * implement {@link #getCreateSequenceStrings(String,int,int)} for these
	 * dialects.
	 * <p>
	 * The default definition is to return {@code create sequence sequenceName}
	 * for the argument {@code sequenceName}. Dialects need to override this
	 * method if a sequence created in this manner does not start at 1, or if
	 * the syntax is nonstandard.
	 * <p>
	 * Dialects which support sequences and can create a sequence in a single
	 * command need *only* override this method. Dialects which support
	 * sequences but require multiple commands to create a sequence should
	 * override {@link #getCreateSequenceStrings(String,int,int)} instead.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence creation command
	 * @throws MappingException If sequences are not supported.
	 */
	default String getCreateSequenceString(String sequenceName) throws MappingException {
		return "create sequence " + sequenceName;
	}

	/**
	 * Typically, dialects which support sequences can create a sequence with
	 * a single command. This method is a convenience making it easier to
	 * implement {@link #getCreateSequenceStrings(String,int,int)} for these
	 * dialects.
	 * <p>
	 * Overloaded form of {@link #getCreateSequenceString(String)}, additionally
	 * taking the initial value and increment size to be applied to the sequence
	 * definition.
	 * </p>
	 * The default definition is to suffix {@link #getCreateSequenceString(String)}
	 * with the string: {@code start with initialValue increment by incrementSize}
	 * for the arguments {@code initialValue} and {@code incrementSize}. Dialects
	 * need to override this method if different key phrases are used to apply the
	 * allocation information.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @return The sequence creation command
	 * @throws MappingException If sequences are not supported.
	 */
	default String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( incrementSize == 0 ) {
			throw new MappingException( "Unable to create the sequence [" + sequenceName + "]: the increment size must not be 0" );
		}
		return getCreateSequenceString( sequenceName )
				+ startingValue( initialValue, incrementSize )
				+ " start with " + initialValue
				+ " increment by " + incrementSize;
	}

	/**
	 * The multiline script used to drop a sequence.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence drop commands
	 * @throws MappingException If sequences are not supported.
	 */
	default String[] getDropSequenceStrings(String sequenceName) throws MappingException {
		return new String[]{ getDropSequenceString( sequenceName ) };
	}

	/**
	 * Typically, dialects which support sequences can drop a sequence
	 * with a single command. This is a convenience form of
	 * {@link #getDropSequenceStrings} which facilitates that.
	 * <p>
	 * Dialects which support sequences and can drop a sequence in a
	 * single command need *only* override this method.  Dialects
	 * which support sequences but require multiple commands to drop
	 * a sequence should instead override {@link #getDropSequenceStrings}.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence drop commands
	 * @throws MappingException If sequences are not supported.
	 */
	default String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence " + sequenceName;
	}

	/**
	 * A DDL statement to restart a sequence with a given value.
	 *
	 * @param sequenceName The name of the sequence
	 * @param startWith The value to restart at
	 * @return The {@code alter sequence ... restart} command
	 */
	default String getRestartSequenceString(String sequenceName, long startWith) {
		return "alter sequence " + sequenceName + " restart with " + startWith;
	}

	/**
	 * Do we need to explicitly specify {@code minvalue} or
	 * {@code maxvalue} when the initial value doesn't have
	 * the same sign as the increment?
	 */
	default boolean sometimesNeedsStartingValue() {
		return false;
	}

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
