/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

/**
 * A property value, or grouped property value
 *
 * @author Gavin King
 */
public class IdentifierProjection extends SimpleProjection {
	private boolean grouped;

	/**
	 * Constructs a non-grouped identifier projection
	 *
	 * @see Projections#id
	 */
	protected IdentifierProjection() {
		this( false );
	}

	/**
	 *
	 * Not used externally
	 */
	private IdentifierProjection(boolean grouped) {
		this.grouped = grouped;
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) {
		return new Type[] { criteriaQuery.getIdentifierType( criteria ) };
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) {
		final StringBuilder buf = new StringBuilder();
		final String[] cols = criteriaQuery.getIdentifierColumns( criteria );
		for ( int i=0; i<cols.length; i++ ) {
			buf.append( cols[i] )
					.append( " as y" )
					.append( position + i )
					.append( '_' );
			if ( i < cols.length -1 ) {
				buf.append( ", " );
			}
		}
		return buf.toString();
	}

	@Override
	public boolean isGrouped() {
		return grouped;
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		if ( !grouped ) {
			return super.toGroupSqlString( criteria, criteriaQuery );
		}
		else {
			return StringHelper.join( ", ", criteriaQuery.getIdentifierColumns( criteria ) );
		}
	}

	@Override
	public String toString() {
		return "id";
	}

}
