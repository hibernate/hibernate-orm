/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.TypedValue;

/**
 * @author Gavin King
 * @see Session#byNaturalId(Class)
 * @see Session#byNaturalId(String)
 * @see Session#bySimpleNaturalId(Class)
 * @see Session#bySimpleNaturalId(String)
 */
public class NaturalIdentifier implements Criterion {
	private final Conjunction conjunction = new Conjunction();

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return conjunction.getTypedValues( criteria, criteriaQuery );
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return conjunction.toSqlString( criteria, criteriaQuery );
	}

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

	public NaturalIdentifier set(String property, Object value) {
		conjunction.add( Restrictions.eq( property, value ) );
		return this;
	}

}
