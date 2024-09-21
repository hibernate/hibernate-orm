/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmOrderByClause implements Serializable {
	private boolean hasPositionalSortItem;
	private List<SqmSortSpecification> sortSpecifications;

	public SqmOrderByClause() {
	}

	public SqmOrderByClause(int estimateSize) {
		this.sortSpecifications = new ArrayList<>( estimateSize );
	}

	private SqmOrderByClause(boolean hasPositionalSortItem, List<SqmSortSpecification> sortSpecifications) {
		this.hasPositionalSortItem = hasPositionalSortItem;
		this.sortSpecifications = sortSpecifications;
	}

	public SqmOrderByClause copy(SqmCopyContext context) {
		final List<SqmSortSpecification> sortSpecifications;
		if ( this.sortSpecifications == null ) {
			sortSpecifications = null;
		}
		else {
			sortSpecifications = new ArrayList<>( this.sortSpecifications.size() );
			for ( SqmSortSpecification sortSpecification : this.sortSpecifications ) {
				sortSpecifications.add( sortSpecification.copy( context ) );
			}
		}
		return new SqmOrderByClause( hasPositionalSortItem, sortSpecifications );
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
			this.hasPositionalSortItem = true;
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

	public void setSortSpecifications(List<SqmSortSpecification> sortSpecifications) {
		this.sortSpecifications = new ArrayList<>();
		this.sortSpecifications.addAll( sortSpecifications );
		this.hasPositionalSortItem = false;
		for ( int i = 0; i < sortSpecifications.size(); i++ ) {
			final SqmSortSpecification sortSpecification = sortSpecifications.get( i );
			if ( sortSpecification.getExpression() instanceof SqmAliasedNodeRef ) {
				this.hasPositionalSortItem = true;
			}
		}
	}
}
