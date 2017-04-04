/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

/**
 * A strategy for determining if an identifier value is an identifier of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
 * the mapping file.
 *
 * @author Gavin King
 */
public class IdentifierValue implements UnsavedValueStrategy {
	private static final Logger LOG = CoreLogging.logger( IdentifierValue.class );

	private final Serializable value;

	/**
	 * Always assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue ANY = new IdentifierValue() {
		@Override
		public final Boolean isUnsaved(Object id) {
			LOG.trace( "ID unsaved-value strategy ANY" );
			return Boolean.TRUE;
		}

		@Override
		public Serializable getDefaultValue(Object currentValue) {
			return (Serializable) currentValue;
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
		public final Boolean isUnsaved(Object id) {
			LOG.trace( "ID unsaved-value strategy NONE" );
			return Boolean.FALSE;
		}

		@Override
		public Serializable getDefaultValue(Object currentValue) {
			return (Serializable) currentValue;
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
		public final Boolean isUnsaved(Object id) {
			LOG.trace( "ID unsaved-value strategy NULL" );
			return id == null;
		}

		@Override
		public Serializable getDefaultValue(Object currentValue) {
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
		public final Boolean isUnsaved(Object id) {
			LOG.trace( "ID unsaved-value strategy UNDEFINED" );
			return null;
		}

		@Override
		public Serializable getDefaultValue(Object currentValue) {
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
	 * its identifier is null or equal to <tt>value</tt>
	 */
	public IdentifierValue(Serializable value) {
		this.value = value;
	}

	/**
	 * Does the given identifier belong to a new instance?
	 */
	@Override
	public Boolean isUnsaved(Object id) {
		LOG.tracev( "ID unsaved-value: {0}", value );
		return id == null || id.equals( value );
	}

	@Override
	public Serializable getDefaultValue(Object currentValue) {
		return value;
	}

	@Override
	public String toString() {
		return "identifier unsaved-value: " + value;
	}
}
