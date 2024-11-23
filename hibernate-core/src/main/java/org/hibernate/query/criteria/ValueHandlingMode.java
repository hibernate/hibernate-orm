/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.HibernateException;

/**
 * This enum defines how values passed to JPA Criteria API are handled.
 * <ul>
 *     <li>The {@code BIND} mode (default) will use bind variables for any value.
 *     <li> The {@code INLINE} mode inlines values as literals.
 * </ul>
 *
 * @see org.hibernate.cfg.AvailableSettings#CRITERIA_VALUE_HANDLING_MODE
 *
 * @author Christian Beikov
 */
public enum ValueHandlingMode {
	BIND,
	INLINE;

	/**
	 * Interpret the configured valueHandlingMode value.
	 * Valid values are either a {@link ValueHandlingMode} object or its String representation.
	 * For string values, the matching is case insensitive, so you can use either {@code BIND} or {@code bind}.
	 *
	 * @param valueHandlingMode configured {@link ValueHandlingMode} representation
	 * @return associated {@link ValueHandlingMode} object
	 */
	public static ValueHandlingMode interpret(Object valueHandlingMode) {
		if ( valueHandlingMode == null ) {
			return BIND;
		}
		else if ( valueHandlingMode instanceof ValueHandlingMode ) {
			return (ValueHandlingMode) valueHandlingMode;
		}
		else if ( valueHandlingMode instanceof String ) {
			for ( ValueHandlingMode value : values() ) {
				if ( value.name().equalsIgnoreCase( (String) valueHandlingMode ) ) {
					return value;
				}
			}
		}
		throw new HibernateException(
				"Unrecognized value_handling_mode value : " + valueHandlingMode
						+ ".  Supported values include 'inline' and 'bind'."
		);
	}
}
