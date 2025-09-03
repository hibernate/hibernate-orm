/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A strategy for determining if an identifier value is an identifier of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the {@code unsaved-value} attribute in
 * the mapping file.
 *
 * @author Gavin King
 */
public class IdentifierValue implements UnsavedValueStrategy {

	private static final Logger log = CoreLogging.logger( IdentifierValue.class );

	private final @Nullable Object value;

	/**
	 * Always assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue ANY = new IdentifierValue() {
		@Override
		public Boolean isUnsaved(Object id) {
			log.trace( "ID unsaved-value strategy ANY" );
			return Boolean.TRUE;
		}

		@Override
		public Object getDefaultValue(Object currentValue) {
			return currentValue;
		}

		@Override
		public String toString() {
			return "SAVE_ANY";
		}
	};

	/**
	 * Never assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue NONE = new IdentifierValue() {
		@Override
		public Boolean isUnsaved(Object id) {
			log.trace( "ID unsaved-value strategy NONE" );
			return Boolean.FALSE;
		}

		@Override
		public Object getDefaultValue(Object currentValue) {
			return currentValue;
		}

		@Override
		public String toString() {
			return "SAVE_NONE";
		}
	};

	/**
	 * Assume the transient instance is newly instantiated if the identifier
	 * is null.
	 */
	public static final IdentifierValue NULL = new IdentifierValue() {
		@Override
		public Boolean isUnsaved(@Nullable Object id) {
			log.trace( "ID unsaved-value strategy NULL" );
			return id == null;
		}

		@Override
		public @Nullable Serializable getDefaultValue(Object currentValue) {
			return null;
		}

		@Override
		public String toString() {
			return "SAVE_NULL";
		}
	};

	/**
	 * Assume nothing.
	 */
	public static final IdentifierValue UNDEFINED = new IdentifierValue() {
		@Override
		public @Nullable Boolean isUnsaved(Object id) {
			log.trace( "ID unsaved-value strategy UNDEFINED" );
			return null;
		}

		@Override
		public @Nullable Serializable getDefaultValue(Object currentValue) {
			return null;
		}

		@Override
		public String toString() {
			return "UNDEFINED";
		}
	};

	protected IdentifierValue() {
		this.value = null;
	}

	/**
	 * Assume the transient instance is newly instantiated if
	 * its identifier is null or equal to {@code value}
	 */
	public IdentifierValue(Object value) {
		this.value = value;
	}

	/**
	 * Does the given identifier belong to a new instance?
	 */
	@Override
	public @Nullable Boolean isUnsaved(@Nullable Object id) {
		log.tracef( "ID unsaved-value: %s", value );
		return id == null || id.equals( value );
	}

	@Override
	public @Nullable Object getDefaultValue(@Nullable Object currentValue) {
		return value;
	}

	@Override
	public String toString() {
		return "identifier unsaved-value: " + value;
	}
}
