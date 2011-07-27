package org.hibernate.spatial.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 28, 2010
 */
public class MetadataInspector {


	static String driver;
	static String dbURI;
	static String userName;
	static String passWord;
	static String table;

	public static void main(String[] args) throws Exception {


		readArgs( args );

		// Connection reference
		Connection conn = null;
		try {

			// Load database driver
			try {
				Class.forName( driver );
			}
			catch ( Exception e ) {
				System.err.println( e );
				System.exit( 1 );
			}

			// Make connection
			conn = DriverManager.getConnection( dbURI, userName, passWord );

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT * from " + table );

			// Get the ResultSet meta data
			ResultSetMetaData rmd = rs.getMetaData();
			rs.next();
			if ( rmd == null ) {

				System.out.println( "ResultSet meta data not available" );

			}
			else {

				int columnCount = rmd.getColumnCount();

				// Display number of Columns in the ResultSet
				System.out.println( "Number of Columns in the table : " + columnCount );


				for ( int i = 1; i <= columnCount; i++ ) {

					// Display number of Column name
					System.out.println( "Column Name : " + rmd.getColumnName( i ) );

					// Display number of Column Type
					System.out.println( "Column TypeName : " + rmd.getColumnTypeName( i ) );


					System.out.println( "Column type : " + rmd.getColumnType( i ) );

					Object o = rs.getObject( i );
					System.out.println( "Column object class: " + o.getClass().getName() );


					// Display if Column can be NOT NULL
					switch ( rmd.isNullable( i ) ) {

						case ResultSetMetaData.columnNoNulls:
							System.out.println( "  NOT NULL" );
							break;
						case ResultSetMetaData.columnNullable:
							System.out.println( "  NULLABLE" );
							break;
						case ResultSetMetaData.columnNullableUnknown:
							System.out.println( "  NULLABLE Unkown" );
					}
					System.out.println();
				}
			}
		}
		finally {

			// Close connection
			if ( conn != null ) {
				try {
					conn.close();
				}
				catch ( SQLException ex ) {
					System.out.println( "Error in closing Conection" );
				}
			}
		}
	}

//    private static String getJavaJDBCTypeName(int type){
//
//    }


	private static void readArgs(String[] args) {
		try {
			driver = args[0];
			dbURI = args[1];
			userName = args[2];
			passWord = args[3];
			table = args[4];

		}
		catch ( Exception e ) {
			System.out.printf( "Usage: metadataInspector <driver> <dbUri> <userName> <passWord> <table>" );
			System.exit( 1 );
		}

	}

}
