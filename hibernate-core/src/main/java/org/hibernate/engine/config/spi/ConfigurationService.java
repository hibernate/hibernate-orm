/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.config.spi;

import java.util.Map;

import org.hibernate.service.Service;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * Provides access to the initial user-provided configuration values.  Generally speaking
 * these values come from:<ul>
 *     <li>Calls to {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#loadProperties}</li>
 *     <li>Calls to {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#applySetting}</li>
 *     <li>Calls to {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#applySettings}</li>
 *     <li>Calls to {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#configure}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface ConfigurationService extends Service {
	/**
	 * Access to the complete map of config settings.  The returned map is immutable
	 *
	 * @return The immutable map of config settings.
	 */
	Map<String,Object> getSettings();

	/**
	 * Get the named setting, using the specified converter.
	 *
	 * @param name The name of the setting to get.
	 * @param converter The converter to apply
	 * @param <T> The Java type of the conversion
	 *
	 * @return The converted (typed) setting.  May return {@code null} (see {@link #getSetting(String, Class, Object)})
	 */
	<T> @Nullable T getSetting(String name, Converter<T> converter);

	/**
	 * Get the named setting, using the specified converter and default value.
	 *
	 * @param name The name of the setting to get.
	 * @param converter The converter to apply
	 * @param defaultValue If no setting with that name is found, return this default value as the result.
	 * @param <T> The Java type of the conversion
	 *
	 * @return The converted (typed) setting.  Will be the defaultValue if no such setting was defined.
	 */
	<T> @PolyNull T getSetting(String name, Converter<T> converter, @PolyNull T defaultValue);

	/**
	 * Get the named setting.  Differs from the form taking a Converter in that here we expect to have a simple
	 * cast rather than any involved conversion.
	 *
	 * @param name The name of the setting to get.
	 * @param expected The expected Java type.
	 * @param defaultValue If no setting with that name is found, return this default value as the result.
	 * @param <T> The Java type of the conversion
	 *
	 * @return The converted (typed) setting.  Will be the defaultValue if no such setting was defined.
	 */
	<T> @PolyNull T getSetting(String name, Class<T> expected, @PolyNull T defaultValue);

	/**
	 * Simple conversion contract for converting an untyped object to a specified type.
	 *
	 * @param <T> The Java type of the converted value
	 */
	interface Converter<T> {
		/**
		 * Convert an untyped Object reference to the Converter's type.
		 *
		 * @param value The untyped value
		 *
		 * @return The converted (typed) value.
		 */
		@NonNull T convert(Object value);
	}
}
