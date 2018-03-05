/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.spatial.HSMessageLogger;

import org.jboss.logging.Logger;

import org.apache.commons.dbcp.BasicDataSource;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecodeException;
import org.geolatte.geom.codec.WktDecoder;

/**
 * <p>Unit testsuite-suite support class.</p>
 *
 * @author Karel Maesen, Geovise BVBA.
 */
public class DataSourceUtils {


	private static HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			DataSourceUtils.class.getName()
	);


	private final SQLExpressionTemplate sqlExpressionTemplate;
	private final String jdbcDriver;
	private final String jdbcUrl;
	private final String jdbcUser;
	private final String jdbcPass;

	private DataSource dataSource;


	/**
	 * Constructor for the DataSourceUtils object.
	 * <p/>
	 * <p>The following entities are required in the property file:
	 * <il>
	 * <li> jdbcUrl: jdbc connection URL</li>
	 * <li> dbUsername: username for the database</li>
	 * <li> dbPassword: password for the database</li>
	 * <li> driver: fully-qualified class name for the JDBC Driver</li>
	 * </il>
	 *
	 * @param jdbcDriver
	 * @param jdbcUrl
	 * @param jdbcUser
	 * @param jdbcPass
	 * @param sqlExpressionTemplate SQLExpressionTemplate object that generates SQL statements for this database
	 */
	public DataSourceUtils(
			String jdbcDriver,
			String jdbcUrl,
			String jdbcUser,
			String jdbcPass,
			SQLExpressionTemplate sqlExpressionTemplate) {
		this.jdbcDriver = jdbcDriver;
		this.jdbcUrl = jdbcUrl;
		this.jdbcUser = jdbcUser;
		this.jdbcPass = jdbcPass;
		this.sqlExpressionTemplate = sqlExpressionTemplate;
		createBasicDataSource();
	}

	/**
	 * Constructor using a properties file
	 *
	 * @param propertyFile
	 * @param template
	 */
	public DataSourceUtils(String propertyFile, SQLExpressionTemplate template) {
		Properties properties = readProperties( propertyFile );
		this.jdbcUrl = properties.getProperty( "jdbcUrl" );
		this.jdbcDriver = properties.getProperty( "jdbcDriver" );
		this.jdbcUser = properties.getProperty( "jdbcUser" );
		this.jdbcPass = properties.getProperty( "jdbcPass" );
		this.sqlExpressionTemplate = template;
		createBasicDataSource();
	}

	private static int sum(int[] insCounts) {
		int result = 0;
		for ( int idx = 0; idx < insCounts.length; idx++ ) {
			result += insCounts[idx];
		}
		return result;
	}

	private Properties readProperties(String propertyFile) {
		InputStream is = null;
		try {
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream( propertyFile );
			if ( is == null ) {
				throw new RuntimeException( String.format( "File %s not found on classpath.", propertyFile ) );
			}
			Properties properties = new Properties();
			properties.load( is );
			return properties;
		}
		catch (IOException e) {
			throw ( new RuntimeException( e ) );
		}
		finally {
			if ( is != null ) {
				try {
					is.close();
				}
				catch (IOException e) {
					//nothing to do
				}
			}
		}
	}

	private void createBasicDataSource() {
		BasicDataSource bds = new BasicDataSource();
		bds.setDriverClassName( jdbcDriver );
		bds.setUrl( jdbcUrl );
		bds.setUsername( jdbcUser );
		bds.setPassword( jdbcPass );
		dataSource = bds;
	}

	/**
	 * Closes the connections to the database.
	 *
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		( (BasicDataSource) dataSource ).close();
	}

	/**
	 * Returns a DataSource for the configured database.
	 *
	 * @return a DataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Returns a JDBC connection to the database
	 *
	 * @return a JDBC Connection object
	 *
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		Connection cn = getDataSource().getConnection();
		cn.setAutoCommit( false );
		return cn;
	}

	/**
	 * Delete all testsuite-suite data from the database
	 *
	 * @throws SQLException
	 */
	public void deleteTestData() throws SQLException {
		Connection cn = null;
		try {
			cn = getDataSource().getConnection();
			cn.setAutoCommit( false );
			PreparedStatement pmt = cn.prepareStatement( "delete from GEOMTEST" );
			if ( !pmt.execute() ) {
				int updateCount = pmt.getUpdateCount();
				LOG.info( "Removing " + updateCount + " rows." );
			}
			cn.commit();
			pmt.close();
		}
		finally {
			try {
				if ( cn != null ) {
					cn.close();
				}
			}
			catch (SQLException e) {
				// nothing to do
			}
		}
	}

	public void insertTestData(TestData testData) throws SQLException {
		Connection cn = null;
		try {
			cn = getDataSource().getConnection();
			cn.setAutoCommit( false );
			Statement stmt = cn.createStatement();
			for ( TestDataElement testDataElement : testData ) {
				String sql = sqlExpressionTemplate.toInsertSql( testDataElement );
				LOG.debug( "adding stmt: " + sql );
				stmt.addBatch( sql );
			}
			int[] insCounts = stmt.executeBatch();
			cn.commit();
			stmt.close();
			LOG.info( "Loaded " + sum( insCounts ) + " rows." );
		}
		catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			try {
				if ( cn != null ) {
					cn.close();
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parses the content of a file into an executable SQL statement.
	 *
	 * @param fileName name of a file containing SQL-statements
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	public String parseSqlIn(String fileName) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( fileName );
		if ( is == null ) {
			throw new RuntimeException( "File " + fileName + " not found on Classpath." );
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new InputStreamReader( is, Charset.forName( "UTF-8" ) )
			);

			StringWriter sw = new StringWriter();
			BufferedWriter writer = new BufferedWriter( sw );

			for ( int c = reader.read(); c != -1; c = reader.read() ) {
				writer.write( c );
			}
			writer.flush();
			return sw.toString();
		}
		finally {
			if ( reader != null ) {
				reader.close();
			}
			is.close();
		}
	}

	/**
	 * Executes a SQL statement.
	 * <p/>
	 * This is used e.g. to drop/create a spatial index, or update the
	 * geometry metadata statements.
	 *
	 * @param sql the (native) SQL Statement to execute
	 *
	 * @throws SQLException
	 */
	public void executeStatement(String sql) throws SQLException {
		Connection cn = null;
		try {
			cn = getDataSource().getConnection();
			cn.setAutoCommit( false );
			PreparedStatement statement = cn.prepareStatement( sql );
			LOG.info( "Executing statement: " + sql );
			statement.execute();
			cn.commit();
			statement.close();
		}
		finally {
			try {
				if ( cn != null ) {
					cn.close();
				}
			}
			catch (SQLException e) {
			} //do nothing.
		}
	}

	/**
	 * Operations to fully initialize the
	 */
	public void afterCreateSchema() {

	}

	/**
	 * Return the geometries of the testsuite-suite objects as raw (i.e. undecoded) objects from the database.
	 *
	 * @param type type of geometry
	 *
	 * @return map of identifier, undecoded geometry object
	 */
	public Map<Integer, Object> rawDbObjects(String type) {
		Map<Integer, Object> map = new HashMap<Integer, Object>();
		Connection cn = null;
		PreparedStatement pstmt = null;
		ResultSet results = null;
		try {
			cn = getDataSource().getConnection();
			pstmt = cn.prepareStatement( "select id, geom from geomtest where type = ? order by id" );
			pstmt.setString( 1, type );
			results = pstmt.executeQuery();
			while ( results.next() ) {
				Integer id = results.getInt( 1 );
				Object obj = results.getObject( 2 );
				map.put( id, obj );
			}

		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if ( results != null ) {
					results.close();
				}
			}
			catch (SQLException e) {
				//nothing to do
			}
			try {
				if ( pstmt != null ) {
					pstmt.close();
				}
			}
			catch (SQLException e) {
				//nothing to do
			}
			try {
				if ( cn != null ) {
					cn.close();
				}
			}
			catch (SQLException e) {
				// nothing we can do.
			}
		}
		return map;

	}

	/**
	 * Returns the JTS geometries that are expected of a decoding of the testsuite-suite object's geometry.
	 * <p/>
	 * <p>This method reads the WKT of the testsuite-suite objects and returns the result.</p>
	 *
	 * @param type type of geometry
	 *
	 * @return map of identifier and JTS geometry
	 */
	public Map<Integer, Geometry> expectedGeoms(String type, TestData testData) {
		Map<Integer, Geometry> result = new HashMap<Integer, Geometry>();
		WktDecoder decoder = Wkt.newDecoder();
		for ( TestDataElement testDataElement : testData ) {
			if ( testDataElement.type.equalsIgnoreCase( type ) ) {
				try {
					result.put( testDataElement.id, decoder.decode( testDataElement.wkt ) );
				}
				catch (WktDecodeException e) {
					System.out
							.println(
									String.format(
											"Parsing WKT fails for case %d : %s",
											testDataElement.id,
											testDataElement.wkt
									)
							);
					throw new RuntimeException( e );
				}
			}
		}
		return result;
	}

}
