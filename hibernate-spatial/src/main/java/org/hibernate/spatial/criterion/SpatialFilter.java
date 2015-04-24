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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.jts.EnvelopeAdapter;

/**
 * A <code>Criterion</code> constraining a geometry property to have a bounding box that overlaps with
 * a specified bounding box.
 *
 * @author Karel Maesen
 */
public class SpatialFilter implements Criterion {

	private static final long serialVersionUID = 1L;
	private String propertyName;
	private Geometry filter;

	/**
	 * Constructs an instance with the specified property and the bounding box of the specified geometry.
	 *
	 * @param propertyName The name of the propety being constrained
	 * @param filter The geometry whose bounding box is used as search geometry
	 */
	public SpatialFilter(String propertyName, Geometry filter) {
		this.propertyName = propertyName;
		this.filter = filter;
	}

	/**
	 * Constructs an instance with the specified property and the bounding box of the specified geometry.
	 *
	 * @param propertyName The name of the propety being constrained
	 * @param envelope The bounding box is used as search geometry
	 * @param srid The SRID of the specified bounding box
	 */
	public SpatialFilter(String propertyName, Envelope envelope, int srid) {
		this.propertyName = propertyName;
		this.filter = EnvelopeAdapter.toPolygon( envelope, srid );

	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				criteriaQuery.getTypedValue( criteria, propertyName, filter )
		};
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, this.propertyName );
		final Dialect dialect = factory.getDialect();
		if ( dialect instanceof SpatialDialect ) {
			final SpatialDialect seDialect = (SpatialDialect) dialect;
			return seDialect.getSpatialFilterExpression( columns[0] );
		}
		else {
			throw new IllegalStateException( "Dialect must be spatially enabled dialect" );
		}
	}

}
