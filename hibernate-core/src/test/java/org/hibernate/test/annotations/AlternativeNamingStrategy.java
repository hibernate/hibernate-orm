//$Id$
package org.hibernate.test.annotations;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class AlternativeNamingStrategy extends EJB3NamingStrategy {
	public static NamingStrategy INSTANCE = new AlternativeNamingStrategy();

	public String classToTableName(String className) {
		return tableName( StringHelper.unqualify( className ) );
	}

	public String propertyToColumnName(String propertyName) {
		return columnName( StringHelper.unqualify( propertyName ) );
	}

	public String tableName(String tableName) {
		return "table_" + tableName;
	}

	public String columnName(String columnName) {
		return "f_" + columnName;
	}

	public String propertyToTableName(String className, String propertyName) {
		return tableName( StringHelper.unqualify( className ) + "_" + StringHelper.unqualify( propertyName ) );
	}

	public String logicalColumnName(String columnName, String propertyName) {
		return StringHelper.isNotEmpty( columnName ) ? columnName : propertyName;
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

	public String logicalCollectionTablelName(
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
		return StringHelper.isNotEmpty( columnName ) ? columnName : propertyName + "_" + referencedColumn;
	}
}
