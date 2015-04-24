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
