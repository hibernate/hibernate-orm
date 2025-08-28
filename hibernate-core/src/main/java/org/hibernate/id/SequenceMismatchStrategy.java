/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import org.hibernate.HibernateException;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes the strategy for handling the mismatch between a database sequence configuration and
 * the one defined by the entity mapping.
 *
 * @see org.hibernate.cfg.AvailableSettings#SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY
 *
 * @author Vlad Mihalcea
 */
public enum SequenceMismatchStrategy {

	/**
	 * When detecting a mismatch, Hibernate simply logs the sequence whose entity mapping configuration conflicts
	 * with the one found in the database.
	 */
	LOG,

	/**
	 * When detecting a mismatch, Hibernate throws a {@link org.hibernate.MappingException} indicating the sequence
	 * whose entity mapping configuration conflict with the one found in the database.
	 */
	EXCEPTION,

	/**
	 * When detecting a mismatch, Hibernate tries to fix it by overriding the entity sequence mapping using the one
	 * found in the database.
	 */
	FIX,

	/**
	 * Don't perform any check. This is useful to speedup bootstrap as it won't query the sequences on the DB,
	 * at cost of not validating the sequences.
	 */
	NONE;

	/**
	 * Interpret the configured SequenceMismatchStrategy value.
	 * <p>
	 * Valid values are either a {@link SequenceMismatchStrategy} object or its String representation.
	 * <p>
	 * For string values, the matching is case insensitive, so you can use either {@code FIX} or {@code fix}.
	 *
	 * @param sequenceMismatchStrategy configured {@link SequenceMismatchStrategy} representation
	 *
	 * @return associated {@link SequenceMismatchStrategy} object
	 */
	public static SequenceMismatchStrategy interpret(@Nullable Object sequenceMismatchStrategy) {
		if ( sequenceMismatchStrategy == null ) {
			return EXCEPTION;
		}
		else if ( sequenceMismatchStrategy instanceof SequenceMismatchStrategy mismatchStrategy ) {
			return mismatchStrategy;
		}
		else if ( sequenceMismatchStrategy instanceof String sequenceMismatchStrategyString ) {
			for ( SequenceMismatchStrategy value : values() ) {
				if ( value.name().equalsIgnoreCase( sequenceMismatchStrategyString ) ) {
					return value;
				}
			}
		}
		throw new HibernateException(
				"Unrecognized sequence.increment_size_mismatch_strategy value : [" + sequenceMismatchStrategy
						+ "].  Supported values include [log], [exception], and [fix]."
		);
	}
}
