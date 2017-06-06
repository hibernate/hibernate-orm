/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.namingstrategy.regression;

import org.hibernate.cfg.EJB3NamingStrategy;

public class MyNamingStrategy extends EJB3NamingStrategy {

	private static final long serialVersionUID = -5713413771290957530L;

	@Override
	public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName) {
		String name = null;

		if ( "localized".equals( propertyName ) ) {
			// NOTE: The problem is that ownerEntity is not the fully qualified class name since 4.2.15 anymore
			name = ownerEntity.replaceAll( "\\.", "_" ).toUpperCase() + "_LOCALIZED";
		}

		if ( name == null ) {
			name = super.collectionTableName( ownerEntity, ownerEntityTable, associatedEntity, associatedEntityTable, propertyName );
		}

		return name;
	}
}
