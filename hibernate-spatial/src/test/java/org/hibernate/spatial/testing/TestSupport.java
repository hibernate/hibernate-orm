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