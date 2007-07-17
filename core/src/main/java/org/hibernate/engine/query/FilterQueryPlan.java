package org.hibernate.engine.query;

import org.hibernate.engine.SessionFactoryImplementor;

import java.io.Serializable;
import java.util.Map;

/**
 * Extends an HQLQueryPlan to maintain a reference to the collection-role name
 * being filtered.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class FilterQueryPlan extends HQLQueryPlan implements Serializable {

	private final String collectionRole;

	public FilterQueryPlan(
			String hql,
	        String collectionRole,
	        boolean shallow,
	        Map enabledFilters,
	        SessionFactoryImplementor factory) {
		super( hql, collectionRole, shallow, enabledFilters, factory );
		this.collectionRole = collectionRole;
	}

	public String getCollectionRole() {
		return collectionRole;
	}
}
