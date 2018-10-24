/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;

import org.locationtech.jts.geom.Geometry;

/**
 * A static factory class for spatial criteria using the Oracle Spatial native spatial operators
 * for the SDO_GEOMTRY type.
 *
 * @author Karel Maesen
 */
public class OracleSpatialRestrictions {

	private OracleSpatialRestrictions() {
	}

	/**
	 * Apply the "SDO_FILTER" constraint to the specified property, using the specified parameters
	 *
	 * @param propertyName The name of the proerty
	 * @param geom The search geometry to use in the constraint
	 * @param param The function parameters for the SDO_FILTER
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDOFilter(String propertyName, Geometry geom, SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {
			@Override
			public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
				final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, this.propertyName );
				final StringBuilder sql = new StringBuilder( "SDO_FILTER(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	/**
	 * Apply the "SDO_FILTER" constraint to the specified property, using the specified parameters
	 *
	 * @param propertyName The name of the proerty
	 * @param geom The search geometry to use in the constraint
	 * @param minResolution The min_resolution parameter
	 * @param maxResolution The max_resolution parameter
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDOFilter(String propertyName, Geometry geom, Double minResolution, Double maxResolution) {
		if ( minResolution == null && maxResolution == null ) {
			return SDOFilter( propertyName, geom, null );
		}
		else {
			final SDOParameterMap param = new SDOParameterMap();
			param.setMinResolution( minResolution );
			param.setMaxResolution( maxResolution );
			return SDOFilter( propertyName, geom, param );
		}
	}

	/**
	 * Apply the "SDO_NN" constraint to the specified property, using the specified parameters
	 *
	 * @param propertyName The name of the property
	 * @param geom The search geometry to use in the constraint
	 * @param distance The distance parameter
	 * @param numResults The num_results parameter
	 * @param unit The unit parameter
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDONN(
			String propertyName,
			Geometry geom,
			Double distance,
			Integer numResults,
			String unit) {
		if ( distance == null && numResults == null && unit == null ) {
			return SDONN( propertyName, geom, null );
		}
		else {
			final SDOParameterMap param = new SDOParameterMap();
			param.setDistance( distance );
			param.setSdoNumRes( numResults );
			param.setUnit( unit );
			return SDONN( propertyName, geom, param );
		}
	}

	/**
	 * Apply the "SDO_NN" constraint to the specified property, using the specified {@code SDOParameterMap}
	 *
	 * @param propertyName The name of the property
	 * @param geom The search geometry to use in the constraint
	 * @param param The parameters for the constraint function
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDONN(String propertyName, Geometry geom, SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {
			@Override
			public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
				final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, this.propertyName );
				final StringBuilder sql = new StringBuilder( "SDO_NN(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	/**
	 * Apply the "SDO_RELATE" constraint to the specified property, using the specified {@code SDOParameterMap}
	 *
	 * @param propertyName The name of the property
	 * @param geom The search geometry to use in the constraint
	 * @param param The parameters for the constraint function
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDORelate(String propertyName, Geometry geom, SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {
			@Override
			public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
				final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, this.propertyName );
				final StringBuilder sql = new StringBuilder( "SDO_RELATE(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	/**
	 * Apply the "SDO_RELATE" constraint to the specified property, using the specified parameters.
	 *
	 * @param propertyName The name of the property
	 * @param geom The search geometry to use in the constraint
	 * @param mask The mask parameter
	 * @param minResolution The min_resolution parameter
	 * @param maxResolution The max_resolution parameter
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDORelate(
			String propertyName,
			Geometry geom,
			RelationshipMask[] mask,
			Double minResolution,
			Double maxResolution) {
		final SDOParameterMap param = new SDOParameterMap();
		param.setMask( RelationshipMask.booleanCombination( mask ) );
		param.setMinResolution( minResolution );
		param.setMaxResolution( maxResolution );
		return SDORelate( propertyName, geom, param );
	}

	/**
	 * Apply the "SDO_WITHIN_DISTANCE" constraint to the specified property, using the specified {@code SDOParameterMap}.
	 *
	 * @param propertyName The name of the property
	 * @param geom The search geometry to use in the constraint
	 * @param param The parameters for the constraint function
	 *
	 * @return The Criterion
	 */
	@SuppressWarnings("serial")
	public static Criterion SDOWithinDistance(String propertyName, Geometry geom, SDOParameterMap param) {
		return new OracleSpatialCriterion( propertyName, geom, param ) {

			@Override
			public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
				final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, this.propertyName );
				final StringBuilder sql = new StringBuilder( "SDO_WITHIN_DISTANCE(" );
				sql.append( columns[0] ).append( "," ).append( "?" );
				if ( param != null && !param.isEmpty() ) {
					sql.append( "," ).append( param.toQuotedString() );
				}
				sql.append( ") = 'TRUE'" );
				return sql.toString();
			}
		};
	}

	/**
	 * Apply the "SDO_WITHIN_DISTANCE" constraint to the specified property, using the specified {@code SDOParameterMap}.
	 *
	 * @param propertyName The name of the property
	 * @param geom The search geometry to use in the constraint
	 * @param distance The distance parameter for the constraint function
	 * @param param The parameters for the constraint function
	 *
	 * @return The Criterion
	 */
	public static Criterion SDOWithinDistance(
			String propertyName,
			Geometry geom,
			Double distance,
			SDOParameterMap param) {
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

	public OracleSpatialCriterion(String propertyName, Geometry value, SDOParameterMap param) {
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

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				criteriaQuery.getTypedValue( criteria, propertyName, value )
		};
	}

	public abstract String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException;

}
