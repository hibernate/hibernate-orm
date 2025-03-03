/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.classloading.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

/**
 * Defines when the lookup in the current thread context {@link ClassLoader} should be
 * done according to the other ones.
 *
 * @author Cédric Tabin
 */
public enum TcclLookupPrecedence {
	/**
	 * The current thread context {@link ClassLoader} will never be used during
	 * the class lookup.
	 */
	NEVER,

	/**
	 * The class lookup will be done in the thread context {@link ClassLoader} prior
	 * to the other {@code ClassLoader}s.
	 */
	BEFORE,

	/**
	 * The class lookup will be done in the thread context {@link ClassLoader} if
	 * the former hasn't been found in the other {@code ClassLoader}s.
	 * This is the default value.
	 */
	AFTER;

	/**
	 * Resolves the precedence from a Map of settings.
	 *
	 * @return The precedence, or {@code null} if none was specified.
	 * @throws IllegalArgumentException If there is a setting defined for
	 * precedence, but it is not a legal value
	 */
	public static TcclLookupPrecedence from(Map<?,?> settings) {
		return from( settings, null );
	}

	/**
	 * Resolves the precedence from a Map of settings
	 *
	 * @return The precedence, or {@code defaultValue} if none was specified.
	 * @throws IllegalArgumentException If there is a setting defined for
	 * precedence, but it is not a legal value
	 */
	public static TcclLookupPrecedence from(Map<?,?> settings, TcclLookupPrecedence defaultValue) {
		final String explicitSetting = (String) settings.get( AvailableSettings.TC_CLASSLOADER );
		if ( explicitSetting == null ) {
			return defaultValue;
		}

		if ( NEVER.name().equalsIgnoreCase( explicitSetting ) ) {
			return NEVER;
		}

		if ( BEFORE.name().equalsIgnoreCase( explicitSetting ) ) {
			return BEFORE;
		}

		throw new IllegalArgumentException( "Unknown TcclLookupPrecedence - " + explicitSetting );
	}
}
