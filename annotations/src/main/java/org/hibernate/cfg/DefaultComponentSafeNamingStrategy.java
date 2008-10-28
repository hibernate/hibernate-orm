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
 */
package org.hibernate.cfg;

import org.hibernate.AssertionFailure;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class DefaultComponentSafeNamingStrategy extends EJB3NamingStrategy {
	public static final NamingStrategy INSTANCE = new DefaultComponentSafeNamingStrategy();

	protected static String addUnderscores(String name) {
		return name.replace( '.', '_' ).toLowerCase();
	}

	@Override
	public String propertyToColumnName(String propertyName) {
		return addUnderscores( propertyName );
	}

	@Override
	public String collectionTableName(
			String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable,
			String propertyName
	) {
		return tableName(
				new StringBuilder( ownerEntityTable ).append( "_" )
						.append(
								associatedEntityTable != null ?
										associatedEntityTable :
										addUnderscores( propertyName )
						).toString()
		);
	}


	public String foreignKeyColumnName(
			String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName
	) {
		String header = propertyName != null ? addUnderscores( propertyName ) : propertyTableName;
		if ( header == null ) throw new AssertionFailure( "NamingStrategy not properly filled" );
		return columnName( header + "_" + referencedColumnName );
	}

	@Override
	public String logicalColumnName(String columnName, String propertyName) {
		return StringHelper.isNotEmpty( columnName ) ? columnName : propertyName;
	}

	@Override
	public String logicalCollectionTableName(
			String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName
	) {
		if ( tableName != null ) {
			return tableName;
		}
		else {
			//use of a stringbuffer to workaround a JDK bug
			return new StringBuffer( ownerEntityTable ).append( "_" )
					.append(
							associatedEntityTable != null ?
									associatedEntityTable :
									propertyName
					).toString();
		}

	}

	@Override
	public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
		return StringHelper.isNotEmpty( columnName ) ?
				columnName :
				propertyName + "_" + referencedColumn;
	}

}
