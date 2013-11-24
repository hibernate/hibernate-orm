/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

import org.hibernate.MappingException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

/**
 * A strategy for determining if a version value is an version of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
 * the mapping file.
 *
 * @author Gavin King
 */
public class VersionValue implements UnsavedValueStrategy {
	private static final Logger LOG = CoreLogging.logger( VersionValue.class );

	private final Object value;
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NULL = new VersionValue() {
		@Override
		public final Boolean isUnsaved(Object version) {
			LOG.trace( "Version unsaved-value strategy NULL" );
			return version == null;
		}

		@Override
		public Object getDefaultValue(Object currentValue) {
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
		public final Boolean isUnsaved(Object version) {
			LOG.trace( "Version unsaved-value strategy UNDEFINED" );
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
		public final Boolean isUnsaved(Object version) throws MappingException {
			LOG.trace( "Version unsaved-value strategy NEGATIVE" );
			if ( version == null ) {
				return Boolean.TRUE;
			}
			if ( version instanceof Number ) {
				return ((Number) version).longValue() < 0L;
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
	 * its version is null or equal to <tt>value</tt>
	 *
	 * @param value value to compare to
	 */
	public VersionValue(Object value) {
		this.value = value;
	}

	@Override
	public Boolean isUnsaved(Object version) throws MappingException {
		LOG.tracev( "Version unsaved-value: {0}", value );
		return version == null || version.equals( value );
	}

	@Override
	public Object getDefaultValue(Object currentValue) {
		return value;
	}

	@Override
	public String toString() {
		return "version unsaved-value: " + value;
	}
}
