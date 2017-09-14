package org.hibernate.spatial.testing.dialects.hana;

import java.sql.SQLException;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;

public class HANADataSourceUtils extends DataSourceUtils {
	public HANADataSourceUtils(String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPass,
			SQLExpressionTemplate sqlExpressionTemplate) {
		super(jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, sqlExpressionTemplate);
	}

	public HANADataSourceUtils(String propertyFile, SQLExpressionTemplate template) {
		super(propertyFile, template);
	}

	@Override
	public void afterCreateSchema() {
		try {
			executeStatement("ALTER TABLE GEOMTEST DROP (GEOM)");
			executeStatement("ALTER TABLE GEOMTEST ADD (GEOM ST_GEOMETRY(4326))");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
