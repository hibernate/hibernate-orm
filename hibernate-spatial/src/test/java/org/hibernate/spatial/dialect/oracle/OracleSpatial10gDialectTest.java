package org.hibernate.spatial.dialect.oracle;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import junit.framework.TestCase;

public class OracleSpatial10gDialectTest extends TestCase {
	public void testConfigureNullProperties() throws Exception {
		ConnectionFinder previous = OracleJDBCTypeFactory.getConnectionFinder();
		new OracleSpatial10gDialect(null);
		assertEquals(previous, OracleJDBCTypeFactory.getConnectionFinder());
	}

	public void testConfigureDefaultLocation() throws Exception {
		File rootDir = new File(getClass().getResource("/").getPath());
		File propsFile = new File(rootDir,
				OracleSpatial10gDialect.class.getCanonicalName()
						+ ".properties");

		Properties props = new Properties();
		props.put(OracleSpatial10gDialect.CONNECTION_FINDER_PROPERTY,
				MockConnectionFinder.class.getCanonicalName());
		props.store(new FileOutputStream(propsFile), "");

		ConnectionFinder previous = OracleJDBCTypeFactory.getConnectionFinder();
		new OracleSpatial10gDialect();
		ConnectionFinder finder = OracleJDBCTypeFactory.getConnectionFinder();
		
		assertNotSame(previous, finder);
		assertEquals(MockConnectionFinder.class, finder.getClass());
		
		propsFile.delete();
	}

	public void testConfigureCustomProperties() throws Exception {
		ConnectionFinder previous = OracleJDBCTypeFactory.getConnectionFinder();

		Properties props = new Properties();
		props.put(OracleSpatial10gDialect.CONNECTION_FINDER_PROPERTY,
				MockConnectionFinder.class.getCanonicalName());
		new OracleSpatial10gDialect(props);

		ConnectionFinder finder = OracleJDBCTypeFactory.getConnectionFinder();
		assertNotSame(previous, finder);
		assertEquals(MockConnectionFinder.class, finder.getClass());
	}
}
