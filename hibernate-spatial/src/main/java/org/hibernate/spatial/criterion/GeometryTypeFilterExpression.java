/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.criterion;

import org.geolatte.geom.GeometryType;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.type.StandardBasicTypes;

public class GeometryTypeFilterExpression implements Criterion {

	private final String propertyName;
	private final GeometryType geometryType;

	public GeometryTypeFilterExpression(String propertyName, GeometryType geometryType) {
		this.propertyName = propertyName;
		this.geometryType = geometryType;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final SpatialDialect spatialDialect = getSpatialDialect( criteriaQuery );
		return spatialDialect.getGeometryTypeSQL( getColumn( criteria, criteriaQuery ));
	}

	private String getColumn(Criteria criteria, CriteriaQuery criteriaQuery) {
		return ExpressionUtil.findColumn(propertyName, criteria, criteriaQuery);
	}

	private SpatialDialect getSpatialDialect(CriteriaQuery criteriaQuery){
		return ExpressionUtil.getSpatialDialect(criteriaQuery, SpatialFunction.geometrytype);
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		String typeString = getSpatialDialect(criteriaQuery).getNameFor(this.geometryType);
		return new TypedValue[] {
			new TypedValue( StandardBasicTypes.STRING, typeString )
		};
	}
}
