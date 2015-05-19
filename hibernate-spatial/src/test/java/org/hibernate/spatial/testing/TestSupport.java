/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;


/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Sep 30, 2010
 */
public abstract class TestSupport {

	protected ConfigurationService configurationService;

	public DataSourceUtils createDataSourceUtil(ServiceRegistry serviceRegistry) {
		this.configurationService = serviceRegistry.getService( ConfigurationService.class );
		return new DataSourceUtils( driver(), url(), user(), passwd(), getSQLExpressionTemplate() );
	}

	public GeometryEquality createGeometryEquality() {
		return new GeometryEquality();
	}

	public abstract TestData createTestData(BaseCoreFunctionalTestCase testcase);

	public abstract AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils);

	public abstract SQLExpressionTemplate getSQLExpressionTemplate();

	protected String driver() {
		return configurationService.getSetting( AvailableSettings.DRIVER, String.class, "" );
	}

	protected String url() {
		return configurationService.getSetting( AvailableSettings.URL, String.class, "" );
	}

	protected String user() {
		return configurationService.getSetting( AvailableSettings.USER, String.class, "" );
	}

	protected String passwd() {
		return configurationService.getSetting( AvailableSettings.PASS, String.class, "" );
	}
}