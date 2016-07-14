/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.sqm.ast.sort.SortSpecification;


/**
 * @author Steve Ebersole
 */
public class SelectQuery {
	private final QuerySpec querySpec;
	private List<SortSpecification> sortSpecifications;

	public SelectQuery(QuerySpec querySpec) {
		this.querySpec = querySpec;
	}

	public QuerySpec getQuerySpec() {
		return querySpec;
	}

	public List<SortSpecification> getSortSpecifications() {
		if ( sortSpecifications == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( sortSpecifications );
		}
	}

	public void addSortSpecification(SortSpecification sortSpecification) {
		if ( sortSpecifications == null ) {
			sortSpecifications = new ArrayList<>();
		}
		sortSpecifications.add( sortSpecification );
	}
}
