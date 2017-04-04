/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * A {@code Criterion} constraining a geometry property to have a specified SRID.
 *
 * @author Karel Maesen, Geovise BVBA
 *
 */
public class HavingSridExpression implements Criterion {

	private final String propertyName;
	private final int srid;

	/**
	 * Constructs an instance for the specified property and srid
	 *
	 * @param propertyName The name of the property being constrained
	 * @param srid The srid
	 */
	public HavingSridExpression(String propertyName, int srid) {
		this.propertyName = propertyName;
		this.srid = srid;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String column = ExpressionUtil.findColumn( propertyName, criteria, criteriaQuery );
		final SpatialDialect spatialDialect = ExpressionUtil.getSpatialDialect( criteriaQuery, SpatialFunction.srid );
		return spatialDialect.getHavingSridSQL( column );
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				new TypedValue( StandardBasicTypes.INTEGER, srid )
		};
	}

}
