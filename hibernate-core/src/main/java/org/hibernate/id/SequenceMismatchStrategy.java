/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.HibernateException;

/**
 * Describes the strategy for handling the mismatch between a database sequence configuration and
 * the one defined by the entity mapping.
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
	FIX;

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
	public static SequenceMismatchStrategy interpret(Object sequenceMismatchStrategy) {
		if ( sequenceMismatchStrategy == null ) {
			return EXCEPTION;
		}
		else if ( sequenceMismatchStrategy instanceof SequenceMismatchStrategy ) {
			return (SequenceMismatchStrategy) sequenceMismatchStrategy;
		}
		else if ( sequenceMismatchStrategy instanceof String ) {
			String sequenceMismatchStrategyString = (String) sequenceMismatchStrategy;
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
