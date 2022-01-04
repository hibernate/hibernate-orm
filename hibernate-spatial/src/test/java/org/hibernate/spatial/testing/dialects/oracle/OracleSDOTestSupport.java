/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.oracle;


import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Decoders;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 22, 2010
 */
public class OracleSDOTestSupport extends TestSupport {

	@Override
	public NativeSQLTemplates templates() {
		return new OracleSDONativeSqlTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new PredicateRegexes( "st_" );
	}

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "oracle10g/test-sdo-geometry-data-set-2D.xml", new SDOTestDataReader() );
	}

	public GeomCodec codec() {
		return new GeomCodec() {
			@Override
			public Geometry<?> toGeometry(Object in) {
				return Decoders.decode( (SDOGeometry) in );
			}
		};
	}

}
