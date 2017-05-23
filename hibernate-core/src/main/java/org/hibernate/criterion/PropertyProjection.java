/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

/**
 * A property value, or grouped property value
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyProjection extends SimpleProjection {
	private String propertyName;
	private boolean grouped;

	protected PropertyProjection(String prop, boolean grouped) {
		this.propertyName = prop;
		this.grouped = grouped;
	}

	protected PropertyProjection(String prop) {
		this( prop, false );
	}

	@Override
	public boolean isGrouped() {
		return grouped;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new Type[] { criteriaQuery.getType( criteria, propertyName ) };
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) throws HibernateException {
		final StringBuilder buf = new StringBuilder();
		final String[] cols = criteriaQuery.getColumns( propertyName, criteria );
		for ( int i=0; i<cols.length; i++ ) {
			buf.append( cols[i] )
					.append( " as y" )
					.append( position + i )
					.append( '_' );
			if (i < cols.length -1) {
				buf.append( ", " );
			}
		}
		return buf.toString();
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		if ( !grouped ) {
			return super.toGroupSqlString( criteria, criteriaQuery );
		}
		else {
			return StringHelper.join( ", ", criteriaQuery.getColumns( propertyName, criteria ) );
		}
	}

	@Override
	public String toString() {
		return propertyName;
	}

}
