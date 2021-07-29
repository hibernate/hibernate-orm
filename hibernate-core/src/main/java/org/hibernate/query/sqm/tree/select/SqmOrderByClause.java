/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmOrderByClause {
	private boolean hasPositionalSortItem;
	private List<SqmSortSpecification> sortSpecifications;

	public SqmOrderByClause() {
	}

	public SqmOrderByClause(int estimateSize) {
		this.sortSpecifications = new ArrayList<>( estimateSize );
	}

	public boolean hasPositionalSortItem() {
		return hasPositionalSortItem;
	}

	@SuppressWarnings("UnusedReturnValue")
	public SqmOrderByClause addSortSpecification(SqmSortSpecification sortSpecification) {
		if ( sortSpecifications == null ) {
			sortSpecifications = new ArrayList<>();
		}
		sortSpecifications.add( sortSpecification );
		if ( sortSpecification.getExpression() instanceof SqmAliasedNodeRef ) {
			hasPositionalSortItem = true;
		}
		return this;
	}

	@SuppressWarnings("unused")
	public SqmOrderByClause addSortSpecification(SqmExpression<?> expression) {
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

	@SuppressWarnings("WeakerAccess")
	public void setSortSpecifications(List<SqmSortSpecification> sortSpecifications) {
		this.sortSpecifications = new ArrayList<>();
		this.sortSpecifications.addAll( sortSpecifications );
		for ( int i = 0; i < sortSpecifications.size(); i++ ) {
			final SqmSortSpecification sortSpecification = sortSpecifications.get( i );
			if ( sortSpecification.getExpression() instanceof SqmAliasedNodeRef ) {
				hasPositionalSortItem = true;
			}
		}
	}
}
