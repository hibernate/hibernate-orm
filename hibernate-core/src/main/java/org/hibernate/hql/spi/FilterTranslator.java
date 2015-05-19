/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;


/**
 * Specialized interface for filters.
 *
 * @author josh
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
