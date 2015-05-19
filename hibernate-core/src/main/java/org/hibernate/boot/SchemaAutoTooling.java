/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.HibernateException;
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
	 * Update (alter) the schema on SessionFactory startup.
	 */
	UPDATE( "update" ),
	/**
	 * Validate the schema on SessionFactory startup.
	 */
	VALIDATE( "validate" );

	private final String externalForm;

	SchemaAutoTooling(String externalForm) {
		this.externalForm = externalForm;
	}

	public static SchemaAutoTooling interpret(String configurationValue) {
		if ( StringHelper.isEmpty( configurationValue ) ) {
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
		else {
			throw new HibernateException(
					"Unrecognized hbm2ddl_auto value : " + configurationValue
							+ ".  Supported values include create, create-drop, update, and validate."
			);
		}
	}
}
