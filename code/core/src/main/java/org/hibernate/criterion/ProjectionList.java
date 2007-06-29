//$Id: ProjectionList.java 6490 2005-04-23 07:09:03Z oneovthafew $
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
public class ProjectionList implements Projection {
	
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
			loc += proj.getColumnAliases(loc).length;
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

	public String[] getColumnAliases(String alias, int loc) {
		for ( int i=0; i<getLength(); i++ ) {
			String[] result = getProjection(i).getColumnAliases(alias, loc);
			if (result!=null) return result;
			loc += getProjection(i).getColumnAliases(loc).length;
		}
		return null;
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
