/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2014 Adtech Geospatial
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

package org.hibernate.spatial.testing.dialects.db2;

import java.sql.SQLException;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;

/**
 * @author David Adler, Adtech Geospatial
 * creation-date: 5/22/2014
 */
public class DB2DataSourceUtils extends DataSourceUtils {

	public DB2DataSourceUtils(
			String jdbcDriver,
			String jdbcUrl,
			String jdbcUser,
			String jdbcPass,
			SQLExpressionTemplate sqlExpressionTemplate) {
		super( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, sqlExpressionTemplate );
	}

	private void createIndex() throws SQLException {
		String sql = "create index idx_spatial_geomtest on geomtest (geom) extend using db2gse.spatial_index(0.1,0,0)";
		executeStatement( sql );
	}

}
