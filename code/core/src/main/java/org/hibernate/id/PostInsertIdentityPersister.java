//$Id: PostInsertIdentityPersister.java 9681 2006-03-24 18:10:04Z steve.ebersole@jboss.com $
package org.hibernate.id;

import org.hibernate.persister.entity.EntityPersister;

/**
 * A persister that may have an identity assigned by execution of 
 * a SQL <tt>INSERT</tt>.
 *
 * @author Gavin King
 */
public interface PostInsertIdentityPersister extends EntityPersister {
	/**
	 * Get a SQL select string that performs a select based on a unique
	 * key determined by the given property name).
	 *
	 * @param propertyName The name of the property which maps to the
	 * column(s) to use in the select statement restriction.
	 * @return The SQL select string
	 */
	public String getSelectByUniqueKeyString(String propertyName);

	/**
	 * Get the database-specific SQL command to retrieve the last
	 * generated IDENTITY value.
	 *
	 * @return The SQL command string
	 */
	public String getIdentitySelectString();

	/**
	 * The names of the primary key columns in the root table.
	 *
	 * @return The primary key column names.
	 */
	public String[] getRootTableKeyColumnNames();
}
