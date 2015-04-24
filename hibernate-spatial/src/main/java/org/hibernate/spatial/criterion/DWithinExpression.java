/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
