/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.oracle;

import java.sql.SQLException;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;


public class SDODataSourceUtils extends DataSourceUtils {

	public SDODataSourceUtils(String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPass, SQLExpressionTemplate sqlExpressionTemplate) {
		super( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, sqlExpressionTemplate );
	}

	@Override
	public void afterCreateSchema() {
		super.afterCreateSchema();
		try {
			setGeomMetaDataTo2D();
			createIndex();
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}

	}

	private void createIndex() throws SQLException {
		String sql = "create index idx_spatial_geomtest on geomtest (geom) indextype is mdsys.spatial_index";
		executeStatement( sql );
	}

	private void setGeomMetaDataTo2D() throws SQLException {
		String sql1 = "delete from user_sdo_geom_metadata where TABLE_NAME = 'GEOMTEST'";
		String sql2 = "insert into user_sdo_geom_metadata values (" +
				"  'GEOMTEST'," +
				"  'geom'," +
				"  SDO_DIM_ARRAY(" +
				"    SDO_DIM_ELEMENT('X', -180, 180, 0.00001)," +
				"    SDO_DIM_ELEMENT('Y', -90, 90, 0.00001)" +
				"    )," +
				"  4326)";
		executeStatement( sql1 );
		executeStatement( sql2 );

	}


}
