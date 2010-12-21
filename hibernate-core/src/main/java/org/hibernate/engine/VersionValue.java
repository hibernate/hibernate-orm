/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import static org.jboss.logging.Logger.Level.TRACE;
import org.hibernate.MappingException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * A strategy for determining if a version value is an version of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
 * the mapping file.
 *
 * @author Gavin King
 */
public class VersionValue {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                VersionValue.class.getPackage().getName());

	private final Object value;
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NULL = new VersionValue() {
		@Override
        public final Boolean isUnsaved(Object version) {
            LOG.versionUnsavedValueStrategy("NULL");
			return version==null ? Boolean.TRUE : Boolean.FALSE;
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
            LOG.versionUnsavedValueStrategy("UNDEFINED");
			return version==null ? Boolean.TRUE : null;
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
            LOG.versionUnsavedValueStrategy("NEGATIVE");
			if (version==null) return Boolean.TRUE;
            if (version instanceof Number) return ((Number)version).longValue() < 0l ? Boolean.TRUE : Boolean.FALSE;
            throw new MappingException("unsaved-value NEGATIVE may only be used with short, int and long types");
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
	 * @param value value to compare to
	 */
	public VersionValue(Object value) {
		this.value = value;
	}

	/**
	 * Does the given version belong to a new instance?
	 *
	 * @param version version to check
	 * @return true is unsaved, false is saved, null is undefined
	 */
	public Boolean isUnsaved(Object version) throws MappingException  {
        LOG.versionUnsavedValue(value);
		return version==null || version.equals(value) ? Boolean.TRUE : Boolean.FALSE;
	}

	public Object getDefaultValue(Object currentValue) {
		return value;
	}

	@Override
    public String toString() {
		return "version unsaved-value: " + value;
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = TRACE )
        @Message( value = "Version unsaved-value: %s" )
        void versionUnsavedValue( Object value );

        @LogMessage( level = TRACE )
        @Message( value = "Version unsaved-value strategy %s" )
        void versionUnsavedValueStrategy( String string );
    }
}