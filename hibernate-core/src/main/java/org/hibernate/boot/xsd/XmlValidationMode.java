/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.xsd;



import java.util.Locale;

/**
 * Indicates the kind of validation that should be applied to xml mappings (hbm or orm.xml)
 *
 * @author Jan Schatteman
 */
public enum XmlValidationMode {
	/*
	* No validation of the xml file is done.
	*/
	DISABLED,
	/*
	 * The xml file is validated against the latest version of the orm.xsd schema.
	 */
	STRICT,
	/*
	 * The xml file is validated against the latest version of hibernate's own extension of the orm schema, i.e. the mapping.xsd schema.
	 * This is the default setting.
	 */
	EXTENDED;

	/**
	 * Resolve the parameter to a valid {@link XmlValidationMode} instance.
	 *
	 * @param xmlValidationMode the configured {@link XmlValidationMode}, as a case-insensitive String or a XmlValidationMode Object
	 * @return the corresponding {@link XmlValidationMode} object
	 */
	public static XmlValidationMode interpret(Object xmlValidationMode) {
		if ( xmlValidationMode instanceof XmlValidationMode ) {
			return (XmlValidationMode) xmlValidationMode;
		}

		final String stringVal;
		if ( xmlValidationMode == null || (stringVal = xmlValidationMode.toString()).isBlank() ) {
			return DISABLED;
		}

		return XmlValidationMode.valueOf( stringVal.toUpperCase( Locale.ROOT ) );
	}

}
