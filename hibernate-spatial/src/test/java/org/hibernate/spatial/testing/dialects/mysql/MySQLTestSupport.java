/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.mysql;


import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.dialect.mariadb.MariaDBGeometryType;
import org.hibernate.spatial.dialect.mysql.MySQLGeometryType;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.JTSGeometryEquality;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.Geometry;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 18, 2010
 */
public class MySQLTestSupport extends TestSupport {

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "mysql/test-mysql-functions-data-set.xml" );
	}

	@Override
	public NativeSQLTemplates templates() {
		return new MySqlNativeSqlTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new PredicateRegexes( "st_geomfromtext" );
	}

	@Override
	public Map<CommonSpatialFunction, String> hqlOverrides() {
		return super.hqlOverrides();
	}

	@Override
	public GeomCodec codec() {
		return new GeomCodec() {
			@Override
			public Geometry<?> toGeometry(Object in) {
				return MySQLGeometryType.INSTANCE.toGeometry( (byte[])in );
			}

		};

	}
}
