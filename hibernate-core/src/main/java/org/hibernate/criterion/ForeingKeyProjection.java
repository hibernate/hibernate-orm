/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.type.Type;

public class ForeingKeyProjection extends SimpleProjection {
	private String associationPropertyName;

	protected ForeingKeyProjection(String associationPropertyName) {
		this.associationPropertyName = associationPropertyName;
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) {
		return new Type[] { criteriaQuery.getForeignKeyType( criteria, associationPropertyName ) };
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) {
		final StringBuilder buf = new StringBuilder();
		final String[] cols = criteriaQuery.getForeignKeyColumns( criteria, associationPropertyName );
		for ( int i = 0; i < cols.length; i++ ) {
			buf.append( cols[i] )
					.append( " as y" )
					.append( position + i )
					.append( '_' );
			if ( i < cols.length - 1 ) {
				buf.append( ", " );
			}
		}
		return buf.toString();
	}

	@Override
	public boolean isGrouped() {
		return false;
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return super.toGroupSqlString( criteria, criteriaQuery );
	}

	@Override
	public String toString() {
		return "fk";
	}

}
