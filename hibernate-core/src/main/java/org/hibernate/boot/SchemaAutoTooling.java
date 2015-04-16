/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot;

import org.hibernate.Hibernate;
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
	CREATE,
	/**
	 * Drop the schema and recreate it on SessionFactory startup.  Additionally, drop the
	 * schema on SessionFactory shutdown.
	 */
	CREATE_DROP,
	/**
	 * Update (alter) the schema on SessionFactory startup.
	 */
	UPDATE,
	/**
	 * Validate the schema on SessionFactory startup.
	 */
	VALIDATE;

	public static SchemaAutoTooling interpret(String configurationValue) {
		if ( StringHelper.isEmpty( configurationValue ) ) {
			return null;
		}
		else if ( "validate".equals( configurationValue ) ) {
			return VALIDATE;
		}
		else if ( "update".equals( configurationValue ) ) {
			return UPDATE;
		}
		else if ( "create".equals( configurationValue ) ) {
			return CREATE;
		}
		else if ( "create-drop".equals( configurationValue ) ) {
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
