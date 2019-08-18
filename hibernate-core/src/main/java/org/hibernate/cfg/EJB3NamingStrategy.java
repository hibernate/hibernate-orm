/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;

/**
 * Naming strategy implementing the EJB3 standards
 *
 * @author Emmanuel Bernard
 */
public class EJB3NamingStrategy implements NamingStrategy, Serializable {
	public static final NamingStrategy INSTANCE = new EJB3NamingStrategy();

	@Override
	public String classToTableName(String className) {
		return StringHelper.unqualify( className );
	}

	@Override
	public String propertyToColumnName(String propertyName) {
		return StringHelper.unqualify( propertyName );
	}

	@Override
	public String tableName(String tableName) {
		return tableName;
	}

	@Override
	public String columnName(String columnName) {
		return columnName;
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
										StringHelper.unqualify( propertyName )
						).toString()
		);
	}

	@Override
	public String joinKeyColumnName(String joinedColumn, String joinedTable) {
		return columnName( joinedColumn );
	}

	@Override
	public String foreignKeyColumnName(
			String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName
	) {
		String header = propertyName != null ? StringHelper.unqualify( propertyName ) : propertyTableName;
		if ( header == null ) throw new AssertionFailure( "NamingStrategy not properly filled" );
		return columnName( header + "_" + referencedColumnName );
	}

	@Override
	public String logicalColumnName(String columnName, String propertyName) {
		return StringHelper.isNotEmpty( columnName ) ? columnName : StringHelper.unqualify( propertyName );
	}

	@Override
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

	@Override
	public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
		return StringHelper.isNotEmpty( columnName ) ?
				columnName :
				StringHelper.unqualify( propertyName ) + "_" + referencedColumn;
	}
}
