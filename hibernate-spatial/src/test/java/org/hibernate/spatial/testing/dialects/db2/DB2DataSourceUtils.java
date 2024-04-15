/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
