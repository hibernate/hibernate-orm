/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;

import java.util.Locale;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * Defines the possible values for {@value AvailableSettings#HBM2DDL_AUTO}.
 *
 * @deprecated This enumeration is currently unused and will be removed.
 *             Use {@link org.hibernate.tool.schema.Action} instead.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.tool.schema.Action
 */
@Deprecated(since = "7.0", forRemoval = true)
public enum SchemaAutoTooling {
	/**
	 * Drop the schema and recreate it on {@code SessionFactory} startup.
	 */
	CREATE,
	/**
	 * Drop the schema and recreate it on {@code SessionFactory} startup.
	 * Additionally, drop the schema on {@code SessionFactory} shutdown.
	 */
	CREATE_DROP,
	/**
	 * Create the schema on {@code SessionFactory} startup.
	 */
	CREATE_ONLY,
	/**
	 * Drop the schema and don't recreate it.
	 */
	DROP,
	/**
	 * Update (alter) the schema on {@code SessionFactory} startup.
	 */
	UPDATE,
	/**
	 * Validate the schema on {@code SessionFactory} startup.
	 */
	VALIDATE,
	/**
	 * Do not attempt to update nor validate the schema.
	 */
	NONE;

	public String externalForm() {
		return toString().toLowerCase(Locale.ROOT).replace('_','-');
	}

	public static SchemaAutoTooling interpret(String configurationValue) {
		if ( configurationValue == null ) {
			return null;
		}
		configurationValue = configurationValue.trim();
		if ( configurationValue.isEmpty()
				|| NONE.externalForm().equals( configurationValue ) ) {
			return null;
		}
		else {
			for ( var value : values() ) {
				if ( value.externalForm().equals( configurationValue ) ) {
					return value;
				}
			}
			throw new HibernateException( "Unrecognized " + HBM2DDL_AUTO + " value: '" + configurationValue
					+ "' (supported values include 'create', 'create-drop', 'create-only', 'drop', 'update', 'none', and 'validate')" );
		}
	}
}
