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
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * @author Gavin King
 */
public class ProjectionList implements EnhancedProjection {
	
	private List elements = new ArrayList();
	
	protected ProjectionList() {}
	
	public ProjectionList create() {
		return new ProjectionList();
	}
	
	public ProjectionList add(Projection proj) {
		elements.add(proj);
		return this;
	}

	public ProjectionList add(Projection projection, String alias) {
		return add( Projections.alias(projection, alias) );
	}

	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		List types = new ArrayList( getLength() );
		for ( int i=0; i<getLength(); i++ ) {
			Type[] elemTypes = getProjection(i).getTypes(criteria, criteriaQuery);
			ArrayHelper.addAll(types, elemTypes);
		}
		return ArrayHelper.toTypeArray(types);
	}
	
	public String toSqlString(Criteria criteria, int loc, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		StringBuffer buf = new StringBuffer();
		for ( int i=0; i<getLength(); i++ ) {
			Projection proj = getProjection(i);
			buf.append( proj.toSqlString(criteria, loc, criteriaQuery) );
			loc += getColumnAliases(loc, criteria, criteriaQuery, proj ).length;
			if ( i<elements.size()-1 ) buf.append(", ");
		}
		return buf.toString();
	}
	
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		StringBuffer buf = new StringBuffer();
		for ( int i=0; i<getLength(); i++ ) {
			Projection proj = getProjection(i);
			if ( proj.isGrouped() ) {
				buf.append( proj.toGroupSqlString(criteria, criteriaQuery) )
					.append(", ");
			}
		}
		if ( buf.length()>2 ) buf.setLength( buf.length()-2 ); //pull off the last ", "
		return buf.toString();
	}
	
	public String[] getColumnAliases(int loc) {
		List result = new ArrayList( getLength() );
		for ( int i=0; i<getLength(); i++ ) {
			String[] colAliases = getProjection(i).getColumnAliases(loc);
			ArrayHelper.addAll(result, colAliases);
			loc+=colAliases.length;
		}
		return ArrayHelper.toStringArray(result);
	}

	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		List result = new ArrayList( getLength() );
		for ( int i=0; i<getLength(); i++ ) {
			String[] colAliases = getColumnAliases( loc, criteria, criteriaQuery, getProjection( i ) );
			ArrayHelper.addAll(result, colAliases);
			loc+=colAliases.length;
		}
		return ArrayHelper.toStringArray(result);
	}

	public String[] getColumnAliases(String alias, int loc) {
		for ( int i=0; i<getLength(); i++ ) {
			String[] result = getProjection(i).getColumnAliases(alias, loc);
			if (result!=null) return result;
			loc += getProjection(i).getColumnAliases(loc).length;
		}
		return null;
	}

	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		for ( int i=0; i<getLength(); i++ ) {
			String[] result = getColumnAliases( alias, loc, criteria, criteriaQuery, getProjection(i) );
			if (result!=null) return result;
			loc += getColumnAliases( loc, criteria, criteriaQuery, getProjection( i ) ).length;
		}
		return null;
	}

	private static String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery, Projection projection) {
		return projection instanceof EnhancedProjection ?
				( ( EnhancedProjection ) projection ).getColumnAliases( loc, criteria, criteriaQuery ) :
				projection.getColumnAliases( loc );
	}

	private static String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery, Projection projection) {
		return projection instanceof EnhancedProjection ?
				( ( EnhancedProjection ) projection ).getColumnAliases( alias, loc, criteria, criteriaQuery ) :
				projection.getColumnAliases( alias, loc );
	}

	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) {
		for ( int i=0; i<getLength(); i++ ) {
			Type[] result = getProjection(i).getTypes(alias, criteria, criteriaQuery);
			if (result!=null) return result;
		}
		return null;
	}

	public String[] getAliases() {
		List result = new ArrayList( getLength() );
		for ( int i=0; i<getLength(); i++ ) {
			String[] aliases = getProjection(i).getAliases();
			ArrayHelper.addAll(result, aliases);
		}
		return ArrayHelper.toStringArray(result);

	}
	
	public Projection getProjection(int i) {
		return (Projection) elements.get(i);
	}
	
	public int getLength() {
		return elements.size();
	}

	public String toString() {
		return elements.toString();
	}

	public boolean isGrouped() {
		for ( int i=0; i<getLength(); i++ ) {
			if ( getProjection(i).isGrouped() ) return true;
		}
		return false;
	}
}
