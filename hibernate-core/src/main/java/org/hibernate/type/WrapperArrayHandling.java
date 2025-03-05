/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.Locale;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Possible options for how to handle {@code Byte[]} and {@code Character[]} basic mappings
 * encountered in the application domain model.
 *
 * @author Steve Ebersole
 *
 * @since 6.2
 */
public enum WrapperArrayHandling {
	/**
	 * Throw an informative and actionable error if the types are used explicitly in the domain model
	 *
	 * @implNote The default behavior; unless {@linkplain AvailableSettings#JPA_COMPLIANCE JPA compliance}
	 * is enabled - see {@linkplain #PICK}
	 */
	DISALLOW,

	/**
	 * Allows the use of the wrapper arrays.  Stores the arrays using {@linkplain SqlTypes#ARRAY ARRAY}
	 * or {@linkplain SqlTypes#SQLXML SQLXML} SQL types to maintain proper null element semantics.
	 */
	ALLOW,

	/**
	 * Allows the use of the wrapper arrays.  Stores the arrays using {@linkplain SqlTypes#VARBINARY VARBINARY}
	 * and {@linkplain SqlTypes#VARCHAR VARCHAR}, disallowing null elements.
	 *
	 * @see ByteArrayJavaType
	 * @see CharacterArrayJavaType
	 *
	 * @implNote The pre-6.2 behavior
	 * @apiNote Hibernate recommends users who want the legacy semantic change the domain model to use
	 * {@code byte[]} and {@code char[]} rather than using this setting.
	 */
	LEGACY,

	/**
	 * Hibernate will pick between {@linkplain #ALLOW} and {@linkplain #LEGACY} depending on
	 * whether the Dialect supports SQL {@code ARRAY} types.
	 *
	 * @implNote The default if {@linkplain AvailableSettings#JPA_COMPLIANCE JPA compliance} is enabled.
	 */
	PICK;

	public static WrapperArrayHandling interpretExternalSetting(Object value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "Null value passed to convert" );
		}

		return value instanceof WrapperArrayHandling wrapperArrayHandling
				? wrapperArrayHandling
				: valueOf( value.toString().toUpperCase( Locale.ROOT ) );
	}

	/**
	 * Form of {@link #interpretExternalSetting(Object)} which allows incoming {@code null} values and
	 * simply returns {@code null}.  Useful for chained resolutions
	 */
	public static WrapperArrayHandling interpretExternalSettingLeniently(@Nullable Object value) {
		if ( value == null ) {
			return null;
		}

		return value instanceof WrapperArrayHandling wrapperArrayHandling
				? wrapperArrayHandling
				: valueOf( value.toString().toUpperCase( Locale.ROOT ) );
	}
}
