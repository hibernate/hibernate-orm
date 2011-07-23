/**
 * $Id: SpatialProjections.java 64 2007-12-16 16:02:31Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *  
 * Copyright © 2007 Geovise BVBA
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
 * @author Karel Maesen
 *
 */
public class SpatialProjections {

	public static Projection extent(final String propertyName) {
		return new SimpleProjection() {

			public Type[] getTypes(Criteria criteria,
					CriteriaQuery criteriaQuery) throws HibernateException {
				return new Type[] { criteriaQuery.getType(criteria,
						propertyName) };
			}

			public String toSqlString(Criteria criteria, int position,
					CriteriaQuery criteriaQuery) throws HibernateException {
				StringBuilder stbuf = new StringBuilder();

				SessionFactoryImplementor factory = criteriaQuery.getFactory();
				String[] columns = criteriaQuery.getColumnsUsingProjection(
						criteria, propertyName);
				Dialect dialect = factory.getDialect();
				if (dialect instanceof SpatialDialect) {
					SpatialDialect seDialect = (SpatialDialect) dialect;

					stbuf.append(seDialect.getSpatialAggregateSQL(columns[0],
							SpatialAggregate.EXTENT));
					stbuf.append(" as y").append(position).append('_');
					return stbuf.toString();
				}
				return null;
			}

		};

	}

}
