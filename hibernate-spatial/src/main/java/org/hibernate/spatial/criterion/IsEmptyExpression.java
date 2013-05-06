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
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

/**
 * A {@code Criterion} constraining a geometry property to be (non-)empty.
 *
 * @author Karel Maesen, Geovise BVBA
 *
 */
public class IsEmptyExpression implements Criterion {

	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	private final String propertyName;
	private final boolean isEmpty;

	/**
	 * Constructs an instance for the specified property
	 *
	 * @param propertyName The name of the property being constrained
	 * @param isEmpty Whether to constrain the property to be empty or non-empty
	 */
	public IsEmptyExpression(String propertyName, boolean isEmpty) {
		this.propertyName = propertyName;
		this.isEmpty = isEmpty;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String column = ExpressionUtil.findColumn( propertyName, criteria, criteriaQuery );
		final SpatialDialect spatialDialect = ExpressionUtil.getSpatialDialect( criteriaQuery, SpatialFunction.isempty );
		return spatialDialect.getIsEmptySQL( column, isEmpty );
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return NO_VALUES;
	}

}
