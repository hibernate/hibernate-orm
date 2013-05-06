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
