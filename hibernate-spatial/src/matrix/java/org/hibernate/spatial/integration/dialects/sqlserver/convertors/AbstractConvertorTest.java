/*
 * $Id: AbstractConvertorTest.java 278 2010-12-18 14:03:32Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.integration.dialects.sqlserver.convertors;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.dialect.sqlserver.convertors.Decoders;
import org.hibernate.spatial.dialect.sqlserver.convertors.Encoders;
import org.hibernate.spatial.dialect.sqlserver.convertors.OpenGisType;
import org.hibernate.spatial.integration.DataSourceUtils;
import org.hibernate.spatial.integration.SpatialFunctionalTestCase;
import org.hibernate.spatial.integration.TestData;
import org.hibernate.spatial.integration.TestSupport;
import org.hibernate.spatial.integration.dialects.sqlserver.SQLServerExpressionTemplate;
import org.hibernate.spatial.integration.dialects.sqlserver.SQLServerTestSupport;
import org.slf4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
public abstract class AbstractConvertorTest extends SpatialFunctionalTestCase {

    private DataSourceUtils dataSourceUtils;

    private final static TestSupport support = new SQLServerTestSupport();

    Map<Integer, Geometry> decodedGeoms;
    Map<Integer, Object> rawResults;
    Map<Integer, byte[]> encodedGeoms;
    Map<Integer, Geometry> expectedGeoms;


    public void beforeClass() {
        dataSourceUtils = new DataSourceUtils(
            "sqlserver/hibernate-spatial-sqlserver-test.properties",
            new SQLServerExpressionTemplate()
            );
        try {
            String sql = dataSourceUtils.parseSqlIn("sqlserver/create-sqlserver-test-schema.sql");
            dataSourceUtils.executeStatement(sql);
            TestData testData = support.createTestData(null);
            dataSourceUtils.insertTestData(testData);
        }catch(SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void afterClass() {
        try {
            String sql = dataSourceUtils.parseSqlIn("sqlserver/drop-sqlserver-test-schema.sql");
            dataSourceUtils.executeStatement(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void doDecoding(OpenGisType type) {
        rawResults = dataSourceUtils.rawDbObjects(type.toString());
        TestData testData = support.createTestData(null);
        expectedGeoms = dataSourceUtils.expectedGeoms(type.toString(), testData);
        decodedGeoms = new HashMap<Integer, Geometry>();

        for (Integer id : rawResults.keySet()) {
            Geometry geometry = Decoders.decode((byte[]) rawResults.get(id));
            decodedGeoms.put(id, geometry);
        }
    }

    public void doEncoding() {
        encodedGeoms = new HashMap<Integer, byte[]>();
        for (Integer id : decodedGeoms.keySet()) {
            Geometry geom = decodedGeoms.get(id);
            byte[] bytes = Encoders.encode(geom);
            encodedGeoms.put(id, bytes);
        }
    }

    public void test_encoding() {
        for (Integer id : encodedGeoms.keySet()) {
            assertTrue(
                    "Wrong encoding for case " + id,
                    Arrays.equals((byte[]) rawResults.get(id), encodedGeoms.get(id))
            );
        }
    }

    public void test_decoding() {
        for (Integer id : decodedGeoms.keySet()) {
            Geometry expected = expectedGeoms.get(id);
            Geometry received = decodedGeoms.get(id);
            assertTrue("Wrong decoding for case " + id, expected.equalsExact(received));
        }
    }

    //TODO -- add logger.
    @Override
    protected Logger getLogger() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}