/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.criterion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;

/**
 * A count projection
 *
 * @author Gavin King
 */
public class CountProjection extends AggregateProjection {
	private boolean distinct;

	/**
	 * Constructs the count projection.
	 *
	 * @param prop The property name
	 *
	 * @see Projections#count(String)
	 * @see Projections#countDistinct(String)
	 */
	protected CountProjection(String prop) {
		super( "count", prop );
	}

	@Override
	protected List buildFunctionParameterList(Criteria criteria, CriteriaQuery criteriaQuery) {
		final String[] cols = criteriaQuery.getColumns( propertyName, criteria );
		return ( distinct ? buildCountDistinctParameterList( cols ) : Arrays.asList( cols ) );
	}

	@SuppressWarnings("unchecked")
	private List buildCountDistinctParameterList(String[] cols) {
		final List params = new ArrayList( cols.length + 1 );
		params.add( "distinct" );
		params.addAll( Arrays.asList( cols ) );
		return params;
	}

	/**
	 * Sets the count as being distinct
	 *
	 * @return {@code this}, for method chaining
	 */
	public CountProjection setDistinct() {
		distinct = true;
		return this;
	}

	@Override
	public String toString() {
		if ( distinct ) {
			return "distinct " + super.toString();
		}
		else {
			return super.toString();
		}
	}

}
