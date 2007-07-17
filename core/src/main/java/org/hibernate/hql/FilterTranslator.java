// $Id: FilterTranslator.java 4899 2004-12-06 14:17:24Z pgmjsd $
package org.hibernate.hql;

import org.hibernate.MappingException;
import org.hibernate.QueryException;

import java.util.Map;


/**
 * Specialized interface for filters.
 *
 * @author josh Mar 14, 2004 11:33:35 AM
 */
public interface FilterTranslator extends QueryTranslator {
	/**
	 * Compile a filter. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param collectionRole the role name of the collection used as the basis for the filter.
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	void compile(String collectionRole, Map replacements, boolean shallow)
			throws QueryException, MappingException;
}
