//$Id$
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
