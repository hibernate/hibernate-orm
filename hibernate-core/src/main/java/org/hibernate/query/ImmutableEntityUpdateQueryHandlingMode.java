/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;

/**
 * Controls for how {@linkplain org.hibernate.annotations.Immutable immutable} entities
 * are handled when executing a bulk update statement.
 * <ul>
 * <li>By default, the {@link #WARNING} mode is used, and a warning log message is issued
 *     when an immutable entity is updated via a bulk update statement.
 * <li>If the {@link #EXCEPTION} mode is configured, then a {@link HibernateException} is
 *     thrown instead.
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
	 * Interpret the setting specified via
	 * {@value AvailableSettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE}.
	 * <p>
	 * Valid values are an instance of {@link ImmutableEntityUpdateQueryHandlingMode}
	 * or its string representation. For string values, the matching is case-insensitive,
	 * so {@code warning} or {@code exception} are legal values.
	 *
	 * @param setting the configuration setting.
	 * @return the associated {@link ImmutableEntityUpdateQueryHandlingMode} object
	 */
	public static ImmutableEntityUpdateQueryHandlingMode interpret(Object setting) {
		if ( setting == null ) {
			return WARNING;
		}
		else if ( setting instanceof ImmutableEntityUpdateQueryHandlingMode mode ) {
			return mode;
		}
		else if ( setting instanceof String string ) {
			for ( ImmutableEntityUpdateQueryHandlingMode value : values() ) {
				if ( value.name().equalsIgnoreCase( string ) ) {
					return value;
				}
			}
		}
		throw new HibernateException( "Unrecognized value '" + setting
				+ "' specified via '" + AvailableSettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
				+ "' (should be 'warning' or 'exception')" );
	}
}
