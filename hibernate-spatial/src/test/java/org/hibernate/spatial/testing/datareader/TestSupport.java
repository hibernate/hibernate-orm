/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.datareader;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.JTSGeometryEquality;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.SQLExpressionTemplate;


/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
@Deprecated
public abstract class TestSupport {

	//TODO -- make this abstract

	public NativeSQLTemplates templates() {
		return null;
	};

	public enum TestDataPurpose {
		SpatialFunctionsData,
		StoreRetrieveData
	}

	protected ConfigurationService configurationService;



	public JTSGeometryEquality createGeometryEquality() {
		return new JTSGeometryEquality();
	}

	public abstract TestData createTestData(TestDataPurpose purpose);

	public GeomCodec codec() {
		throw new NotYetImplementedFor6Exception();
	};

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
