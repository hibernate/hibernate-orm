/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.MappingException;
import org.hibernate.id.IdentifierGeneratorHelper;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * A strategy for determining if a version value is a version of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the {@code unsaved-value} attribute in
 * the mapping file.
 *
 * @author Gavin King
 */
public class VersionValue implements UnsavedValueStrategy {

	private final @Nullable Object value;
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NULL = new VersionValue() {
		@Override
		public Boolean isUnsaved(@Nullable Object version) {
			CORE_LOGGER.versionUnsavedValueStrategy( "NULL" );
			return version == null;
		}

		@Override
		public @Nullable Object getDefaultValue(Object currentValue) {
			return null;
		}

		@Override
		public String toString() {
			return "VERSION_SAVE_NULL";
		}
	};

	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise defer to the identifier unsaved-value.
	 */
	public static final VersionValue UNDEFINED = new VersionValue() {
		@Override
		public @Nullable Boolean isUnsaved(@Nullable Object version) {
			CORE_LOGGER.versionUnsavedValueStrategy( "UNDEFINED" );
			return version == null ? Boolean.TRUE : null;
		}

		@Override
		public Object getDefaultValue(Object currentValue) {
			return currentValue;
		}

		@Override
		public String toString() {
			return "VERSION_UNDEFINED";
		}
	};

	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is negative, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NEGATIVE = new VersionValue() {

		@Override
		public Boolean isUnsaved(@Nullable Object version) throws MappingException {
			CORE_LOGGER.versionUnsavedValueStrategy( "NEGATIVE" );
			if ( version == null ) {
				return Boolean.TRUE;
			}
			if ( version instanceof Number number ) {
				return number.longValue() < 0L;
			}
			throw new MappingException( "unsaved-value NEGATIVE may only be used with short, int and long types" );
		}

		@Override
		public Object getDefaultValue(Object currentValue) {
			return IdentifierGeneratorHelper.getIntegralDataTypeHolder( currentValue.getClass() )
					.initialize( -1L )
					.makeValue();
		}

		@Override
		public String toString() {
			return "VERSION_NEGATIVE";
		}
	};

	protected VersionValue() {
		this.value = null;
	}

	/**
	 * Assume the transient instance is newly instantiated if
	 * its version is null or equal to {@code value}
	 *
	 * @param value value to compare to
	 */
	public VersionValue(Object value) {
		this.value = value;
	}

	@Override
	public @Nullable Boolean isUnsaved(@Nullable Object version) throws MappingException {
		CORE_LOGGER.versionUnsavedValue( value );
		return version == null || version.equals( value );
	}

	@Override
	public @Nullable Object getDefaultValue(@Nullable Object currentValue) {
		return value;
	}

	@Override
	public String toString() {
		return "version unsaved-value: " + value;
	}
}
