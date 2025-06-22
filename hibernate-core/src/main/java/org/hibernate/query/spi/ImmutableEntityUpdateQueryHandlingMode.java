/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;

/**
 * Controls how {@linkplain org.hibernate.annotations.Immutable immutable}
 * entities are handled when executing a bulk update or delete statement.
 * <p>
 * By default, the {@link #EXCEPTION} mode is used, and so bulk update or
 * delete queries affecting immutable entities are disallowed.
 *
 * @deprecated This enumeration will be removed, and replaced with a simpler
 *             boolean-valued switch.
 *
 * @see org.hibernate.cfg.AvailableSettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
 *
 * @author Vlad Mihalcea
 */
@Deprecated(since = "7.0", forRemoval = true)
public enum ImmutableEntityUpdateQueryHandlingMode {

	/**
	 * Allow update or delete queries for immutable entities.
	 */
	ALLOW,
	/**
	 * Log a warning when an immutable entity is affected by a bulk update or
	 * delete statement.
	 */
	WARNING,
	/**
	 * Throw a {@link HibernateException} when an immutable entity is affected
	 * by a bulk update or delete statement.
	 */
	EXCEPTION;

	/**
	 * Interpret the setting specified via
	 * {@value AvailableSettings#IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE}.
	 * <p>
	 * Valid values are an instance of {@link ImmutableEntityUpdateQueryHandlingMode}
	 * or its string representation. For string values, the matching is case-insensitive,
	 * so {@code allow}, {@code warning}, or {@code exception} are legal values.
	 *
	 * @param setting the configuration setting.
	 * @return the associated {@link ImmutableEntityUpdateQueryHandlingMode} object
	 */
	public static ImmutableEntityUpdateQueryHandlingMode interpret(Object setting) {
		if ( setting == null ) {
			return EXCEPTION;
		}
		else if ( setting instanceof ImmutableEntityUpdateQueryHandlingMode mode ) {
			return mode;
		}
		else if ( setting instanceof String string ) {
			for ( var value : values() ) {
				if ( value.name().equalsIgnoreCase( string ) ) {
					return value;
				}
			}
		}
		throw new HibernateException( "Unrecognized value '" + setting
				+ "' specified via '" + AvailableSettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE
				+ "' (should be 'allow', 'warning', or 'exception')" );
	}
}
