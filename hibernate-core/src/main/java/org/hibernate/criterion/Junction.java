/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.criterion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;

/**
 * A sequence of a logical expressions combined by some
 * associative logical operator
 *
 * @author Gavin King
 */
public class Junction implements Criterion {
	private final Nature nature;
	private final List<Criterion> conditions = new ArrayList<Criterion>();

	protected Junction(Nature nature) {
		this.nature = nature;
	}
	
	public Junction add(Criterion criterion) {
		conditions.add( criterion );
		return this;
	}

	public Nature getNature() {
		return nature;
	}

	public Iterable<Criterion> conditions() {
		return conditions;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria crit, CriteriaQuery criteriaQuery) throws HibernateException {
		ArrayList<TypedValue> typedValues = new ArrayList<TypedValue>();
		for ( Criterion condition : conditions ) {
			TypedValue[] subValues = condition.getTypedValues( crit, criteriaQuery );
			Collections.addAll( typedValues, subValues );
		}
		return typedValues.toArray( new TypedValue[ typedValues.size() ] );
	}

	@Override
	public String toSqlString(Criteria crit, CriteriaQuery criteriaQuery) throws HibernateException {
		if ( conditions.size()==0 ) {
			return "1=1";
		}

		StringBuilder buffer = new StringBuilder().append( '(' );
		Iterator itr = conditions.iterator();
		while ( itr.hasNext() ) {
			buffer.append( ( (Criterion) itr.next() ).toSqlString( crit, criteriaQuery ) );
			if ( itr.hasNext() ) {
				buffer.append(' ').append( nature.getOperator() ).append(' ');
			}
		}
		return buffer.append(')').toString();
	}

	@Override
	public String toString() {
		return '(' + StringHelper.join( ' ' + nature.getOperator() + ' ', conditions.iterator() ) + ')';
	}

	public static enum Nature {
		AND,
		OR
		;

		public String getOperator() {
			return name().toLowerCase();
		}
	}
}
