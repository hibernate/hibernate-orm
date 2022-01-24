/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * This enum defines how {@link org.hibernate.annotations.Immutable} entities are handled when
 * executing a bulk update statement.
 * <ul>
 *     <li>By default, the {@link ImmutableEntityUpdateQueryHandlingMode#WARNING} mode is used,
 *         and a warning log message is issued when an {@link org.hibernate.annotations.Immutable}
 *         entity is to be updated via a bulk update statement.
 *     <li>If the {@link ImmutableEntityUpdateQueryHandlingMode#EXCEPTION} mode is used, then a
 *         {@link HibernateException} is thrown instead.
 * </ul>
 *
 * @see org.hibernate.cfg.AvailableSettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
 *
 * @author Vlad Mihalcea
 */
public enum ImmutableEntityUpdateQueryHandlingMode {

	WARNING,
	EXCEPTION;

	/**
	 * Interpret the configured {@link ImmutableEntityUpdateQueryHandlingMode} value.
	 * Valid values are either a {@link ImmutableEntityUpdateQueryHandlingMode} object or
	 * its string representation. For string values, the matching is case-insensitive,
	 * so you can use either {@code warning} or {@code exception}.
	 *
	 * @param mode configured {@link ImmutableEntityUpdateQueryHandlingMode} representation
	 * @return associated {@link ImmutableEntityUpdateQueryHandlingMode} object
	 */
	public static ImmutableEntityUpdateQueryHandlingMode interpret(Object mode) {
		if ( mode == null ) {
			return WARNING;
		}
		else if ( mode instanceof ImmutableEntityUpdateQueryHandlingMode ) {
			return (ImmutableEntityUpdateQueryHandlingMode) mode;
		}
		else if ( mode instanceof String ) {
			for ( ImmutableEntityUpdateQueryHandlingMode value : values() ) {
				if ( value.name().equalsIgnoreCase( (String) mode ) ) {
					return value;
				}
			}
		}
		throw new HibernateException(
				"Unrecognized immutable_entity_update_query_handling_mode value : " + mode
						+ ".  Supported values include 'warning' and 'exception'."
		);
	}
}
