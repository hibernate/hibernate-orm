/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.criterion;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

import org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect;
import org.hibernate.type.StandardBasicTypes;

/**
 * A {@code Criterion} constraining a geometry property to be within a specified distance of a search geometry.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 2/1/11
 */
public class DWithinExpression implements Criterion {


	private final String propertyName;
	private final Geometry geometry;
	private final double distance;

	/**
	 * Constructs an instance
	 *
	 * @param propertyName The name of the property being constrained
	 * @param geometry The search geometry
	 * @param distance The search distance (in units of the spatial reference system of the search geometry)
	 */
	public DWithinExpression(String propertyName, Geometry geometry, double distance) {
		this.propertyName = propertyName;
		this.geometry = geometry;
		this.distance = distance;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String column = ExpressionUtil.findColumn( propertyName, criteria, criteriaQuery );
		final SpatialDialect spatialDialect = ExpressionUtil.getSpatialDialect(
				criteriaQuery,
				SpatialFunction.dwithin
		);
		return spatialDialect.getDWithinSQL( column );

	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final SpatialDialect spatialDialect = ExpressionUtil.getSpatialDialect( criteriaQuery, SpatialFunction.dwithin );
		TypedValue typedDistanceValue = new TypedValue( StandardBasicTypes.DOUBLE, distance );
		if ( spatialDialect instanceof OracleSpatial10gDialect) {
			typedDistanceValue = new TypedValue( StandardBasicTypes.STRING, "distance=" + distance );
		}
		return new TypedValue[] {
				criteriaQuery.getTypedValue( criteria, propertyName, geometry ),
				typedDistanceValue
		};
	}
}
