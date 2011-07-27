/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2011 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission,
 * under the 6th Framework Programme, contract IST-2-004688-STP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */

package org.hibernate.spatial.criterion;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 2/1/11
 */
public class DWithinExpression implements Criterion {


	private final String propertyName;
	private final Geometry geometry;
	private final double distance;

	public DWithinExpression(String propertyName, Geometry geometry, double distance) {
		this.propertyName = propertyName;
		this.geometry = geometry;
		this.distance = distance;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		String column = ExpressionUtil.findColumn( propertyName, criteria, criteriaQuery );
		SpatialDialect spatialDialect = ExpressionUtil.getSpatialDialect( criteriaQuery, SpatialFunction.dwithin );
		return spatialDialect.getDWithinSQL( column );

	}


	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				criteriaQuery.getTypedValue( criteria, propertyName, geometry ),
				new TypedValue( StandardBasicTypes.DOUBLE, Double.valueOf( distance ), EntityMode.POJO )
		};
	}
}
