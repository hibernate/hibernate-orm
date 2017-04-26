/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmOrderByClause {
	private List<SqmSortSpecification> sortSpecifications;

	public SqmOrderByClause() {
	}

	public SqmOrderByClause addSortSpecification(SqmSortSpecification sortSpecification) {
		if ( sortSpecifications == null ) {
			sortSpecifications = new ArrayList<SqmSortSpecification>();
		}
		sortSpecifications.add( sortSpecification );
		return this;
	}

	public SqmOrderByClause addSortSpecification(SqmExpression expression) {
		addSortSpecification( new SqmSortSpecification( expression ) );
		return this;
	}

	public List<SqmSortSpecification> getSortSpecifications() {
		if ( sortSpecifications == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( sortSpecifications );
		}
	}
}
