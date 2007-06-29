package org.hibernate.criterion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.util.StringHelper;

/**
 * A sequence of a logical expressions combined by some
 * associative logical operator
 *
 * @author Gavin King
 */
public class Junction implements Criterion {

	private final List criteria = new ArrayList();
	private final String op;
	
	protected Junction(String op) {
		this.op = op;
	}
	
	public Junction add(Criterion criterion) {
		criteria.add(criterion);
		return this;
	}

	public String getOp() {
		return op;
	}

	public TypedValue[] getTypedValues(Criteria crit, CriteriaQuery criteriaQuery)
	throws HibernateException {
		ArrayList typedValues = new ArrayList();
		Iterator iter = criteria.iterator();
		while ( iter.hasNext() ) {
			TypedValue[] subvalues = ( (Criterion) iter.next() ).getTypedValues(crit, criteriaQuery);
			for ( int i=0; i<subvalues.length; i++ ) {
				typedValues.add( subvalues[i] );
			}
		}
		return (TypedValue[]) typedValues.toArray( new TypedValue[ typedValues.size() ] );
	}

	public String toSqlString(Criteria crit, CriteriaQuery criteriaQuery)
	throws HibernateException {

		if ( criteria.size()==0 ) return "1=1";

		StringBuffer buffer = new StringBuffer()
			.append('(');
		Iterator iter = criteria.iterator();
		while ( iter.hasNext() ) {
			buffer.append( ( (Criterion) iter.next() ).toSqlString(crit, criteriaQuery) );
			if ( iter.hasNext() ) buffer.append(' ').append(op).append(' ');
		}
		return buffer.append(')').toString();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return '(' + StringHelper.join( ' ' + op + ' ', criteria.iterator() ) + ')';
	}

}
