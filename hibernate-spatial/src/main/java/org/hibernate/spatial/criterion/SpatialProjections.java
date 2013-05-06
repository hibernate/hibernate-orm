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
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.SimpleProjection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.type.Type;

/**
 * A factory for spatial projections.
 *
 * @author Karel Maesen
 */
public class SpatialProjections {

	private SpatialProjections() {
	}

	/**
	 * Applies an extent projection to the specified geometry function
	 *
	 * <p>The extent of a set of {@code Geometry}s is the union of their bounding boxes.</p>
	 *
	 * @param propertyName The property to use for calculating the extent
	 *
	 * @return an extent-projection for the specified property.
	 */
	public static Projection extent(final String propertyName) {
		return new SimpleProjection() {

			public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
				return new Type[] {
						criteriaQuery.getType( criteria, propertyName )
				};
			}

			public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery)
					throws HibernateException {
				final StringBuilder stbuf = new StringBuilder();

				final SessionFactoryImplementor factory = criteriaQuery.getFactory();
				final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, propertyName );
				final Dialect dialect = factory.getDialect();
				if ( dialect instanceof SpatialDialect ) {
					final SpatialDialect seDialect = (SpatialDialect) dialect;
					stbuf.append(
							seDialect.getSpatialAggregateSQL( columns[0], SpatialAggregate.EXTENT )
					);
					stbuf.append( " as y" ).append( position ).append( '_' );
					return stbuf.toString();
				}
				return null;
			}

		};

	}

}
