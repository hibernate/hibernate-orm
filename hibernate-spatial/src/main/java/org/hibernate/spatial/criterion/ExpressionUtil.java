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

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

/**
 * This class assists in the formation of a SQL-fragment in the various spatial query expressions.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 2/15/11
 */
public class ExpressionUtil {

	public static SpatialDialect getSpatialDialect(CriteriaQuery criteriaQuery, SpatialFunction function) {
		Dialect dialect = criteriaQuery.getFactory().getDialect();
		if (!(dialect instanceof SpatialDialect)) {
			throw new HibernateException("A spatial expression requires a spatial dialect.");
		}
		SpatialDialect spatialDialect = (SpatialDialect) dialect;
		if (!spatialDialect.supports(function)) {
			throw new HibernateException(function + " function not supported by this dialect");
		}
		return spatialDialect;
	}

	public static String findColumn(String propertyName, Criteria criteria, CriteriaQuery criteriaQuery) {
		String[] columns = criteriaQuery.findColumns(propertyName, criteria);
		if (columns.length != 1) {
			throw new HibernateException("Spatial Expression may only be used with single-column properties");
		}
		return columns[0];
	}
}
