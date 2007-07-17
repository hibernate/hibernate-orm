// $Id: NativeSQLQueryRootReturn.java 7232 2005-06-19 17:16:40 -0500 (Sun, 19 Jun 2005) maxcsaucdk $
package org.hibernate.engine.query.sql;

import java.util.Map;

import org.hibernate.LockMode;

/**
 * Represents a return defined as part of a native sql query which
 * names a "root" entity.  A root entity means it is explicitly a
 * "column" in the result, as opposed to a fetched relationship or role.
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryRootReturn extends NativeSQLQueryNonScalarReturn {
	private String returnEntityName;

	/**
	 * Construct a return representing an entity returned at the root
	 * of the result.
	 *
	 * @param alias The result alias
	 * @param entityName The entity name.
	 * @param lockMode The lock mode to apply
	 */
	public NativeSQLQueryRootReturn(String alias, String entityName, LockMode lockMode) {
		this(alias, entityName, null, lockMode);
	}

	/**
	 *
	 * @param alias The result alias
	 * @param entityName The entity name.
	 * @param propertyResults Any user-supplied column->property mappings
	 * @param lockMode The lock mode to apply
	 */
	public NativeSQLQueryRootReturn(String alias, String entityName, Map propertyResults, LockMode lockMode) {
		super( alias, propertyResults, lockMode );
		this.returnEntityName = entityName;

	}

	/**
	 * The name of the entity to be returned.
	 *
	 * @return The entity name
	 */
	public String getReturnEntityName() {
		return returnEntityName;
	}

}
