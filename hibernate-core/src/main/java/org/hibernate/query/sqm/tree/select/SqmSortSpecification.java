/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmSortSpecification implements JpaOrder {
	private final SqmExpression sortExpression;
	private final SortDirection sortOrder;
	private final boolean ignoreCase;
	private NullPrecedence nullPrecedence;

	public SqmSortSpecification(
			SqmExpression sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		this( sortExpression, sortOrder, nullPrecedence, false );
	}

	public SqmSortSpecification(
				SqmExpression sortExpression,
				SortDirection sortOrder,
				NullPrecedence nullPrecedence,
				boolean ignoreCase) {
		assert sortExpression != null;
		assert sortOrder != null;
		assert nullPrecedence != null;
		this.sortExpression = sortExpression;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
		this.ignoreCase = ignoreCase;
	}

	public SqmSortSpecification(SqmExpression sortExpression) {
		this( sortExpression, SortDirection.ASCENDING, NullPrecedence.NONE );
	}

	public SqmSortSpecification(SqmExpression sortExpression, SortDirection sortOrder) {
		this( sortExpression, sortOrder, NullPrecedence.NONE );
	}

	public SqmSortSpecification copy(SqmCopyContext context) {
		return new SqmSortSpecification( sortExpression.copy( context ), sortOrder, nullPrecedence, ignoreCase );
	}

	public SqmExpression<?> getSortExpression() {
		return sortExpression;
	}

	@Override
	public SortDirection getSortDirection() {
		return sortOrder;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public JpaOrder nullPrecedence(NullPrecedence nullPrecedence) {
		this.nullPrecedence = nullPrecedence;
		return this;
	}

	@Override
	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}

	@Override
	public JpaOrder reverse() {
		SortDirection newSortOrder = this.sortOrder == null ? SortDirection.DESCENDING : sortOrder.reverse();
		return new SqmSortSpecification( sortExpression, newSortOrder, nullPrecedence, ignoreCase );
	}

	@Override
	public JpaExpression<?> getExpression() {
		return getSortExpression();
	}

	@Override
	public boolean isAscending() {
		return sortOrder == SortDirection.ASCENDING;
	}

	public void appendHqlString(StringBuilder sb) {
		sortExpression.appendHqlString( sb );
		if ( sortOrder == SortDirection.DESCENDING ) {
			sb.append( " desc" );
			if ( nullPrecedence != null ) {
				if ( nullPrecedence == NullPrecedence.FIRST ) {
					sb.append( " nulls first" );
				}
				else {
					sb.append( " nulls last" );
				}
			}
		}
		else if ( nullPrecedence != null ) {
			sb.append( " asc" );
			if ( nullPrecedence == NullPrecedence.FIRST ) {
				sb.append( " nulls first" );
			}
			else {
				sb.append( " nulls last" );
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		else if ( !(o instanceof SqmSortSpecification) ) {
			return false;
		}
		else {
			// used in SqmInterpretationsKey.equals()
			SqmSortSpecification that = (SqmSortSpecification) o;
			return Objects.equals( sortExpression, that.sortExpression )
				&& sortOrder == that.sortOrder
				&& nullPrecedence == that.nullPrecedence;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( sortExpression, sortOrder, nullPrecedence );
	}
}
