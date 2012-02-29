/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.hibernate.spatial.dialect.oracle.criterion;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;

/**
 * A static factory class for creating <code>Criterion</code> instances that
 * correspond to Oracle Spatial "native" spatial operators.
 *
 * @author Karel Maesen
 */
public class OracleSpatialRestrictions {

	@SuppressWarnings("serial")
	public static Criterion SDOFilter(String propertyName, Geometry geom,
									  SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {
			@Override
			public String toSqlString(Criteria criteria,
									  CriteriaQuery criteriaQuery) throws HibernateException {
				String[] columns = criteriaQuery.getColumnsUsingProjection(
						criteria, this.propertyName
				);
				StringBuilder sql = new StringBuilder( "SDO_FILTER(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	@SuppressWarnings("serial")
	public static Criterion SDOFilter(String propertyName, Geometry geom,
									  Double minResolution, Double maxResolution) {
		if ( minResolution == null && maxResolution == null ) {
			return SDOFilter( propertyName, geom, null );
		}
		else {
			SDOParameterMap param = new SDOParameterMap();
			param.setMinResolution( minResolution );
			param.setMaxResolution( maxResolution );
			return SDOFilter( propertyName, geom, param );
		}
	}

	@SuppressWarnings("serial")
	public static Criterion SDONN(String propertyName, Geometry geom,
								  Double distance, Integer numResults, String unit) {
		if ( distance == null && numResults == null && unit == null ) {
			return SDONN( propertyName, geom, null );
		}
		else {
			SDOParameterMap param = new SDOParameterMap();
			param.setDistance( distance );
			param.setSdoNumRes( numResults );
			param.setUnit( unit );
			return SDONN( propertyName, geom, param );
		}
	}

	@SuppressWarnings("serial")
	public static Criterion SDONN(String propertyName, Geometry geom,
								  SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {
			@Override
			public String toSqlString(Criteria criteria,
									  CriteriaQuery criteriaQuery) throws HibernateException {
				String[] columns = criteriaQuery.getColumnsUsingProjection(
						criteria, this.propertyName
				);
				StringBuilder sql = new StringBuilder( "SDO_NN(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	@SuppressWarnings("serial")
	public static Criterion SDORelate(String propertyName, Geometry geom,
									  SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {
			@Override
			public String toSqlString(Criteria criteria,
									  CriteriaQuery criteriaQuery) throws HibernateException {
				String[] columns = criteriaQuery.getColumnsUsingProjection(
						criteria, this.propertyName
				);
				StringBuilder sql = new StringBuilder( "SDO_RELATE(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	@SuppressWarnings("serial")
	public static Criterion SDORelate(String propertyName, Geometry geom,
									  RelationshipMask[] mask, Double minResolution, Double maxResolution) {
		SDOParameterMap param = new SDOParameterMap();
		param.setMask( RelationshipMask.booleanCombination( mask ) );
		param.setMinResolution( minResolution );
		param.setMaxResolution( maxResolution );
		return SDORelate( propertyName, geom, param );
	}

	@SuppressWarnings("serial")
	public static Criterion SDOWithinDistance(String propertyName,
											  Geometry geom, SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {

			@Override
			public String toSqlString(Criteria criteria,
									  CriteriaQuery criteriaQuery) throws HibernateException {
				String[] columns = criteriaQuery.getColumnsUsingProjection(
						criteria, this.propertyName
				);
				StringBuilder sql = new StringBuilder( "SDO_WITHIN_DISTANCE(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	public static Criterion SDOWithinDistance(String propertyName,
											  Geometry geom, Double distance, SDOParameterMap param) {
		if ( param == null ) {
			param = new SDOParameterMap();
		}
		param.setDistance( distance );
		return SDOWithinDistance( propertyName, geom, param );
	}
}

abstract class OracleSpatialCriterion implements Criterion {
	protected String propertyName;

	protected Geometry value;

	protected SDOParameterMap param;

	public OracleSpatialCriterion(String propertyName, Geometry value,
								  SDOParameterMap param) {
		this.propertyName = propertyName;
		this.value = value;
		this.param = param;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.criterion.Criterion#getTypedValues(org.hibernate.Criteria,
		  *      org.hibernate.criterion.CriteriaQuery)
		  */

	public TypedValue[] getTypedValues(Criteria criteria,
									   CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				criteriaQuery.getTypedValue(
						criteria,
						propertyName, value
				)
		};
	}

	abstract public String toSqlString(Criteria criteria,
									   CriteriaQuery criteriaQuery) throws HibernateException;

}