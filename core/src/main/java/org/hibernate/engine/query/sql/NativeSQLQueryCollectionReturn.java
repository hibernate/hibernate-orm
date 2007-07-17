// $Id: NativeSQLQueryCollectionReturn.java 7232 2005-06-19 17:16:40 -0500 (Sun, 19 Jun 2005) maxcsaucdk $
package org.hibernate.engine.query.sql;

import java.util.Map;

import org.hibernate.LockMode;

/**
 * Represents a return defined as part of a native sql query which
 * names a collection role in the form {classname}.{collectionrole}; it
 * is used in defining a custom sql query for loading an entity's
 * collection in non-fetching scenarios (i.e., loading the collection
 * itself as the "root" of the result).
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryCollectionReturn extends NativeSQLQueryNonScalarReturn {
	private String ownerEntityName;
	private String ownerProperty;

	/**
	 * Construct a native-sql return representing a collection initializer
	 *
	 * @param alias The result alias
	 * @param ownerEntityName The entity-name of the entity owning the collection
	 * to be initialized.
	 * @param ownerProperty The property name (on the owner) which represents
	 * the collection to be initialized.
	 * @param propertyResults Any user-supplied column->property mappings
	 * @param lockMode The lock mode to apply to the collection.
	 */
	public NativeSQLQueryCollectionReturn(
			String alias,
			String ownerEntityName,
			String ownerProperty,
			Map propertyResults,
			LockMode lockMode) {
		super( alias, propertyResults, lockMode );
		this.ownerEntityName = ownerEntityName;
		this.ownerProperty = ownerProperty;
	}

	/**
	 * Returns the class owning the collection.
	 *
	 * @return The class owning the collection.
	 */
	public String getOwnerEntityName() {
		return ownerEntityName;
	}

	/**
	 * Returns the name of the property representing the collection from the {@link #getOwnerEntityName}.
	 *
	 * @return The name of the property representing the collection on the owner class.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}
}
