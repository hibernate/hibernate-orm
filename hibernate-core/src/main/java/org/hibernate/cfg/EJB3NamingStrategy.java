/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.binder.HashedNameUtil;

/**
 * Naming strategy implementing the EJB3 standards
 *
 * @author Emmanuel Bernard
 */
public class EJB3NamingStrategy implements NamingStrategy, Serializable {
	public static final NamingStrategy INSTANCE = new EJB3NamingStrategy();

	public String classToTableName(String className) {
		return StringHelper.unqualify( className );
	}

	public String propertyToColumnName(String propertyName) {
		return StringHelper.unqualify( propertyName );
	}

	public String tableName(String tableName) {
		return tableName;
	}

	public String columnName(String columnName) {
		return columnName;
	}

	public String collectionTableName(
			String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable,
			String propertyName
	) {
		return tableName(
				new StringBuilder( ownerEntityTable ).append( "_" )
						.append(
								associatedEntityTable != null ?
										associatedEntityTable :
										StringHelper.unqualify( propertyName )
						).toString()
		);
	}

	public String joinKeyColumnName(String joinedColumn, String joinedTable) {
		return columnName( joinedColumn );
	}

	public String foreignKeyColumnName(
			String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName
	) {
		String header = propertyName != null ? StringHelper.unqualify( propertyName ) : propertyTableName;
		if ( header == null ) throw new AssertionFailure( "NamingStrategy not properly filled" );
		return columnName( header + "_" + referencedColumnName );
	}
	
	public String foreignKeyName(String sourceTableName, List<String> sourceColumnNames,
			String targetTableName, List<String> targetColumnNames) {
		List<String> combinedColumnNames = new ArrayList<String>();
		combinedColumnNames.addAll( sourceColumnNames );
		combinedColumnNames.addAll( targetColumnNames );
		return HashedNameUtil.generateName( "FK_", sourceTableName + "_" + targetTableName, combinedColumnNames );
	}
	
	public String uniqueKeyName(String tableName, List<String> columnNames) {
		return HashedNameUtil.generateName( "UK_", tableName, columnNames );
	}
	
	public String indexName(String tableName, List<String> columnNames) {
		return HashedNameUtil.generateName( "IDX_", tableName, columnNames );
	}

	public String logicalColumnName(String columnName, String propertyName) {
		return StringHelper.isNotEmpty( columnName ) ? columnName : StringHelper.unqualify( propertyName );
	}

	public String logicalCollectionTableName(
			String tableName,
			String ownerEntityTable, String associatedEntityTable, String propertyName
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
									StringHelper.unqualify( propertyName )
					).toString();
		}
	}

	public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
		return StringHelper.isNotEmpty( columnName ) ?
				columnName :
				StringHelper.unqualify( propertyName ) + "_" + referencedColumn;
	}
}
