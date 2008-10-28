//$Id$
package org.hibernate.annotations;

/**
 * Fetch options on associations
 *
 * @author Emmanuel Bernard
 */
public enum FetchMode {
	/**
	 * use a select for each individual entity, collection, or join load
	 */
	SELECT,
	/**
	 * use an outer join to load the related entities, collections or joins
	 */
	JOIN,
	/**
	 * use a subselect query to load the additional collections
	 */
	SUBSELECT
}
