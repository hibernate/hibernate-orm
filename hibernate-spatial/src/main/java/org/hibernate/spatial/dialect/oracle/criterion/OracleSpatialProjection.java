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
package org.hibernate.spatial.dialect.oracle.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.SimpleProjection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.type.Type;

/**
 * Template class for Spatial Projections
 *
 * @author Tom Acree
 */
public class OracleSpatialProjection extends SimpleProjection {

	private static final long serialVersionUID = 1L;
	private final String propertyName;
	private final int aggregate;

	/**
	 * Constructs an instance for the specified aggregate function and property
	 *
	 * @param aggregate The aggregate function (a value of {@code OracleSpatialAggregate}
	 * @param propertyName The name of the geometry property
	 */
	public OracleSpatialProjection(int aggregate, String propertyName) {
		this.propertyName = propertyName;
		this.aggregate = aggregate;
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) throws HibernateException {

		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final String[] columns = criteriaQuery.getColumnsUsingProjection(
				criteria,
				this.propertyName
		);
		final Dialect dialect = factory.getDialect();
		if ( dialect instanceof SpatialDialect ) {
			final SpatialDialect seDialect = (SpatialDialect) dialect;

			return new StringBuffer(
					seDialect.getSpatialAggregateSQL(
							columns[0], this.aggregate
					)
			).append( " y" ).append( position )
					.append( "_" ).toString();
		}
		else {
			throw new IllegalStateException(
					"Dialect must be spatially enabled dialect"
			);
		}

	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return new Type[] { criteriaQuery.getType( criteria, this.propertyName ) };
	}

	@Override
	public String toString() {
		return aggregate + "(" + propertyName + ")";
	}
}
