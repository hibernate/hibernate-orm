package org.hibernate.spatial.dialect.sqlserver.convertors;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.dialect.sqlserver.SQLServerExpressionTemplate;
import org.hibernate.spatial.dialect.sqlserver.SQLServerTestSupport;
import org.hibernate.spatial.test.DataSourceUtils;
import org.hibernate.spatial.test.TestData;
import org.hibernate.spatial.test.TestSupport;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple utitlity to generate the binary geometry objects by inserting spatial objects into a SQL Server 2008 instance,
 * and reading back the results.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/11/12
 */
public class SpatialObjectGenerator {

    private final static String TEST_DATA = "sqlserver-2008-test-data.ser";

    private final static DataSourceUtils dataSourceUtils = new DataSourceUtils(
            "sqlserver/hibernate-spatial-sqlserver-test.properties",
            new SQLServerExpressionTemplate()
    );

    private final static TestSupport support = new SQLServerTestSupport();

    private final static String[] TYPES;

    static {
        TYPES = new String[OpenGisType.values().length];
        int i = 0;
        for (OpenGisType type : OpenGisType.values()) {
            TYPES[i++] = type.toString();
        }
    }

    private final TestData testData = support.createTestData(null);

    public static void main(String[] argv) {
        File outFile = createOutputFile(argv);
        SpatialObjectGenerator generator = new SpatialObjectGenerator();
        try {
            generator.prepare();
            List<ConvertorTestData> result = generator.generateTestDataObjects();
            writeTo(outFile, result);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally{
            try {
                generator.discard();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    private static void writeTo(File outFile, List<ConvertorTestData> result) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;

        try {
            fos = new FileOutputStream(outFile);
            out = new ObjectOutputStream(fos);
            out.writeObject(result);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.printf("Wrote %d objects to %s.", result.size(), outFile.getAbsolutePath());
    }

    private static File createOutputFile(String[] argv) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File outFile = new File(tmpDir, TEST_DATA);
        if (argv.length > 0) {
            outFile = new File(argv[1]);
        }
        return outFile;
    }


    public void prepare() throws IOException, SQLException {
        String sql = dataSourceUtils.parseSqlIn("sqlserver/create-sqlserver-test-schema.sql");
        dataSourceUtils.executeStatement(sql);
        dataSourceUtils.insertTestData(testData);
    }

    public List<ConvertorTestData> generateTestDataObjects() {
        List<ConvertorTestData> result = new ArrayList<ConvertorTestData>();
        for (String type : TYPES) {
            addTestObjectForType(type, result);
        }
        return result;
    }

    private void addTestObjectForType(String type, List<ConvertorTestData> result) {
        Map<Integer, Object> rawResults = dataSourceUtils.rawDbObjects(type.toString());
        Map<Integer, Geometry> geometries = dataSourceUtils.expectedGeoms(type.toString(), testData);
        addToResult(type, result, rawResults, geometries);
    }

    private void addToResult(String type, List<ConvertorTestData> result, Map<Integer, Object> rawResults, Map<Integer, Geometry> geometries) {
        for (Integer id : rawResults.keySet()) {
            ConvertorTestData data = new ConvertorTestData();
            data.id = id;
            data.geometry = geometries.get(id);
            data.type = type;
            data.bytes = (byte[]) rawResults.get(id);
            result.add(data);
        }
    }


    public void discard() throws SQLException, IOException {
        String sql = dataSourceUtils.parseSqlIn("sqlserver/drop-sqlserver-test-schema.sql");
        dataSourceUtils.executeStatement(sql);
    }

}
