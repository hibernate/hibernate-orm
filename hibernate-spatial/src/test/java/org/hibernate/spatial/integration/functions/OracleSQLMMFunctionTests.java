/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.HibernateSpatialConfigurationSettings;
import org.hibernate.spatial.testing.dialects.oracle.OracleSTNativeSqlTemplates;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Only for Oracle: run the tests in "OGC_STRICT" mode (i.e. using the SQL MultiMedia functions)
 */
@RequiresDialect( value = OracleDialect.class)
@ServiceRegistry(settings = {
		@Setting(name = HibernateSpatialConfigurationSettings.ORACLE_OGC_STRICT, value = "true")
})
public class OracleSQLMMFunctionTests extends CommonFunctionTests{
	public OracleSQLMMFunctionTests() {
		this.templates = new OracleSTNativeSqlTemplates();
	}
}
