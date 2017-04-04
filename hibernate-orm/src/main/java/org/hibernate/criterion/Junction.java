/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;

/**
 * A sequence of a logical expressions combined by some
 * associative logical operator
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Junction implements Criterion {
	private final Nature nature;
	private final List<Criterion> conditions = new ArrayList<Criterion>();

	protected Junction(Nature nature) {
		this.nature = nature;
	}

	protected Junction(Nature nature, Criterion... criterion) {
		this( nature );
		Collections.addAll( conditions, criterion );
	}

	/**
	 * Adds a criterion to the junction (and/or)
	 *
	 * @param criterion The criterion to add
	 *
	 * @return {@code this}, for method chaining
	 */
	public Junction add(Criterion criterion) {
		conditions.add( criterion );
		return this;
	}

	public Nature getNature() {
		return nature;
	}

	/**
	 * Access the conditions making up the junction
	 *
	 * @return the criterion
	 */
	public Iterable<Criterion> conditions() {
		return conditions;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria crit, CriteriaQuery criteriaQuery) throws HibernateException {
		final ArrayList<TypedValue> typedValues = new ArrayList<TypedValue>();
		for ( Criterion condition : conditions ) {
			final TypedValue[] subValues = condition.getTypedValues( crit, criteriaQuery );
			Collections.addAll( typedValues, subValues );
		}
		return typedValues.toArray( new TypedValue[ typedValues.size() ] );
	}

	@Override
	public String toSqlString(Criteria crit, CriteriaQuery criteriaQuery) throws HibernateException {
		if ( conditions.size()==0 ) {
			return "1=1";
		}

		final StringBuilder buffer = new StringBuilder().append( '(' );
		final Iterator itr = conditions.iterator();
		while ( itr.hasNext() ) {
			buffer.append( ( (Criterion) itr.next() ).toSqlString( crit, criteriaQuery ) );
			if ( itr.hasNext() ) {
				buffer.append( ' ' )
						.append( nature.getOperator() )
						.append( ' ' );
			}
		}

		return buffer.append( ')' ).toString();
	}

	@Override
	public String toString() {
		return '(' + StringHelper.join( ' ' + nature.getOperator() + ' ', conditions.iterator() ) + ')';
	}

	/**
	 * The type of junction
	 */
	public static enum Nature {
		/**
		 * An AND
		 */
		AND,
		/**
		 * An OR
		 */
		OR;

		/**
		 * The corresponding SQL operator
		 *
		 * @return SQL operator
		 */
		public String getOperator() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
