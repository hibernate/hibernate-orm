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
package org.hibernate.spatial.criterion;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;

/**
 * A {@code Criterion} constraining a {@code Geometry} property to have specific spatial relation
 * to a search {@code Geometry}.
 *
 * @author Karel Maesen
 */
public class SpatialRelateExpression implements Criterion {

	private static final long serialVersionUID = 1L;
	/**
	 * The geometry property
	 */
	private String propertyName;
	/**
	 * The test geometry
	 */
	private Geometry value;
	/**
	 * The spatial relation that is queried for.
	 */
	private int spatialRelation = -1;

	/**
	 * Constructs an instance
	 *
	 * @param propertyName The name of the property being constrained
	 * @param value The search {@code Geometry}
	 * @param spatialRelation The type of {@code SpatialRelation} to use in the comparison
	 */
	public SpatialRelateExpression(String propertyName, Geometry value, int spatialRelation) {
		this.propertyName = propertyName;
		this.spatialRelation = spatialRelation;
		this.value = value;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] { criteriaQuery.getTypedValue( criteria, propertyName, value ) };
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, this.propertyName );
		final Dialect dialect = factory.getDialect();
		if ( dialect instanceof SpatialDialect ) {
			final SpatialDialect seDialect = (SpatialDialect) dialect;
			return seDialect.getSpatialRelateSQL( columns[0], spatialRelation );
		}
		else {
			throw new IllegalStateException( "Dialect must be spatially enabled dialect" );
		}
	}

}
