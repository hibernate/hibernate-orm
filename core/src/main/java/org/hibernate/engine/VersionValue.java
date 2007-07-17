//$Id: VersionValue.java 7017 2005-06-05 04:31:34Z oneovthafew $
package org.hibernate.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.MappingException;
import org.hibernate.id.IdentifierGeneratorFactory;

/**
 * A strategy for determining if a version value is an version of
 * a new transient instance or a previously persistent transient instance.
 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
 * the mapping file.
 * 
 * @author Gavin King
 */
public class VersionValue {

	private static final Log log = LogFactory.getLog(VersionValue.class);

	private final Object value;
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NULL = new VersionValue() {
		public final Boolean isUnsaved(Object version) {
			log.trace("version unsaved-value strategy NULL");
			return version==null ? Boolean.TRUE : Boolean.FALSE;
		}
		public Object getDefaultValue(Object currentValue) {
			return null;
		}
		public String toString() {
			return "VERSION_SAVE_NULL";
		}
	};
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise defer to the identifier unsaved-value.
	 */
	public static final VersionValue UNDEFINED = new VersionValue() {
		public final Boolean isUnsaved(Object version) {
			log.trace("version unsaved-value strategy UNDEFINED");
			return version==null ? Boolean.TRUE : null;
		}
		public Object getDefaultValue(Object currentValue) {
			return currentValue;
		}
		public String toString() {
			return "VERSION_UNDEFINED";
		}
	};
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is negative, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NEGATIVE = new VersionValue() {
	
		public final Boolean isUnsaved(Object version) throws MappingException {
			log.trace("version unsaved-value strategy NEGATIVE");
			if (version==null) return Boolean.TRUE;
			if (version instanceof Number) {
				return ( (Number) version ).longValue() < 0l ? Boolean.TRUE : Boolean.FALSE;
			}
			else {
				throw new MappingException("unsaved-value NEGATIVE may only be used with short, int and long types");
			}
		}
		public Object getDefaultValue(Object currentValue) {
			return IdentifierGeneratorFactory.createNumber( -1l, currentValue.getClass() );
		}
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
		if ( log.isTraceEnabled() ) log.trace("version unsaved-value: " + value);
		return version==null || version.equals(value) ? Boolean.TRUE : Boolean.FALSE;
	}
	
	public Object getDefaultValue(Object currentValue) {
		return value;
	}
	
	public String toString() {
		return "version unsaved-value: " + value;
	}
}