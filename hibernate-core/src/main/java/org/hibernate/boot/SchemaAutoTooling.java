/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.StringHelper;

/**
 * Defines the possible values for "hbm2ddl_auto"
 *
 * @author Steve Ebersole
 */
public enum SchemaAutoTooling {
	/**
	 * Drop the schema and recreate it on SessionFactory startup.
	 */
	CREATE( "create" ),
	/**
	 * Drop the schema and recreate it on SessionFactory startup.  Additionally, drop the
	 * schema on SessionFactory shutdown.
	 */
	CREATE_DROP( "create-drop" ),
	/**
	 * Create the schema on SessionFactory startup.
	 */
	CREATE_ONLY( "create-only" ),
	/**
	 * Drop the schema and don't recreate it.
	 */
	DROP( "drop" ),
	/**
	 * Update (alter) the schema on SessionFactory startup.
	 */
	UPDATE( "update" ),
	/**
	 * Validate the schema on SessionFactory startup.
	 */
	VALIDATE( "validate" ),
	/**
	 * Do not attempt to update nor validate the schema.
	 */
	NONE( "none" );

	private final String externalForm;

	SchemaAutoTooling(String externalForm) {
		this.externalForm = externalForm;
	}

	public static SchemaAutoTooling interpret(String configurationValue) {
		if ( configurationValue == null ) {
			return null;
		}
		configurationValue = configurationValue.trim();
		if ( configurationValue.isEmpty() || NONE.externalForm.equals( configurationValue ) ) {
			return null;
		}
		else if ( VALIDATE.externalForm.equals( configurationValue ) ) {
			return VALIDATE;
		}
		else if ( UPDATE.externalForm.equals( configurationValue ) ) {
			return UPDATE;
		}
		else if ( CREATE.externalForm.equals( configurationValue ) ) {
			return CREATE;
		}
		else if ( CREATE_DROP.externalForm.equals( configurationValue ) ) {
			return CREATE_DROP;
		}
		else if ( CREATE_ONLY.externalForm.equals( configurationValue ) ) {
			return CREATE_ONLY;
		}
		else if ( DROP.externalForm.equals( configurationValue ) ) {
			return DROP;
		}
		else {
			throw new HibernateException(
					"Unrecognized " + AvailableSettings.HBM2DDL_AUTO + " value: '" + configurationValue
							+ "'.  Supported values include 'create', 'create-drop', 'create-only', 'drop', 'update', 'none' and 'validate'."
			);
		}
	}
}
