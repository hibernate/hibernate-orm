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
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;

import org.locationtech.jts.geom.Geometry;

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
