/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;

/**
 * An expression pertaining to an entity's defined natural identifier
 *
 * @author Gavin King
 *
 * @see org.hibernate.Session#byNaturalId(Class)
 * @see org.hibernate.Session#byNaturalId(String)
 * @see org.hibernate.Session#bySimpleNaturalId(Class)
 * @see org.hibernate.Session#bySimpleNaturalId(String)
 */
public class NaturalIdentifier implements Criterion {
	private final Conjunction conjunction = new Conjunction();

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return conjunction.getTypedValues( criteria, criteriaQuery );
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return conjunction.toSqlString( criteria, criteriaQuery );
	}

	/**
	 * Get a map of set of the natural identifier values set on this criterion (for composite natural identifiers
	 * this need not be the full set of properties).
	 *
	 * @return The value map.
	 */
	public Map<String, Object> getNaturalIdValues() {
		final Map<String, Object> naturalIdValueMap = new ConcurrentHashMap<String, Object>();
		for ( Criterion condition : conjunction.conditions() ) {
			if ( !SimpleExpression.class.isInstance( condition ) ) {
				continue;
			}
			final SimpleExpression equalsCondition = SimpleExpression.class.cast( condition );
			if ( !"=".equals( equalsCondition.getOp() ) ) {
				continue;
			}

			naturalIdValueMap.put( equalsCondition.getPropertyName(), equalsCondition.getValue() );
		}
		return naturalIdValueMap;
	}

	/**
	 * Set a natural identifier value for this expression
	 *
	 * @param property The specific property name
	 * @param value The value to use
	 *
	 * @return {@code this}, for method chaining
	 */
	public NaturalIdentifier set(String property, Object value) {
		conjunction.add( Restrictions.eq( property, value ) );
		return this;
	}

}
