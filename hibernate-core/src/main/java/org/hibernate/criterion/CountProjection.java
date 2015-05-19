/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
