/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.engine.jdbc.jdbc4;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

import junit.framework.TestCase;

import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.JdbcSupportLoader;
import org.hibernate.engine.jdbc.ContextualLobCreator;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.WrappedBlob;
import org.hibernate.engine.jdbc.WrappedClob;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class JdbcSupportTest extends TestCase {
	private interface Envionment {
		public String getDriver();
		public String getUrl();
		public String getUser();
		public String getPass();

		public void verifyCreator(LobCreator lobCreator);
		public void verifyBlob(Blob blob);
		public void verifyClob(Clob clob);
		public void verifyNClob(Clob nclob);
	}

	private abstract class ContextualEnvironment implements Envionment {
		public void verifyCreator(LobCreator lobCreator) {
			assertTrue( lobCreator instanceof ContextualLobCreator );
		}

		public void verifyBlob(Blob blob) {
			assertFalse( blob instanceof BlobImplementer );
		}

		public void verifyClob(Clob clob) {
			assertFalse( clob instanceof ClobImplementer );
		}

		public void verifyNClob(Clob nclob) {
			assertFalse( nclob instanceof NClobImplementer );
		}
	}

	private abstract class NonContextualEnvironment implements Envionment {
		public void verifyCreator(LobCreator lobCreator) {
			assertTrue( lobCreator instanceof NonContextualLobCreator );
		}

		public void verifyBlob(Blob blob) {
			assertTrue( blob instanceof BlobImplementer );
		}

		public void verifyClob(Clob clob) {
			assertTrue( clob instanceof ClobImplementer );
		}

		public void verifyNClob(Clob nclob) {
			assertTrue( nclob instanceof NClobImplementer );
		}
	}

	private Envionment POSTGRESQL = new NonContextualEnvironment() {
		public String getDriver() {
			return "org.postgresql.Driver";
		}

		public String getUrl() {
			return "jdbc:postgresql://vmg03.mw.lab.eng.bos.redhat.com:5432:platformae";
		}

		public String getUser() {
			return "sebersole";
		}

		public String getPass() {
			return "sebersole";
		}
	};

	private Envionment MYSQL = new ContextualEnvironment() {
		public String getDriver() {
			return "com.mysql.jdbc.Driver";
		}

		public String getUrl() {
			return "jdbc:mysql://vmg02.mw.lab.eng.bos.redhat.com/sebersole";
		}

		public String getUser() {
			return "sebersole";
		}

		public String getPass() {
			return "sebersole";
		}
	};

	private Envionment ORACLE9i = new ContextualEnvironment() {
		public String getDriver() {
			return "oracle.jdbc.driver.OracleDriver";
		}

		public String getUrl() {
			return "jdbc:oracle:thin:@dev20.qa.atl.jboss.com:1521:qa";
		}

		public String getUser() {
			return "sebersole";
		}

		public String getPass() {
			return "sebersole";
		}
	};

	private Envionment ORACLE10g = new ContextualEnvironment() {
		public String getDriver() {
			return "oracle.jdbc.driver.OracleDriver";
		}

		public String getUrl() {
			return "jdbc:oracle:thin:@vmg05.mw.lab.eng.bos.redhat.com:1521:qaora10";
		}

		public String getUser() {
			return "sebersole";
		}

		public String getPass() {
			return "sebersole";
		}
	};

	private Envionment ORACLE11g = new ContextualEnvironment() {
		public String getDriver() {
			return "oracle.jdbc.driver.OracleDriver";
		}

		public String getUrl() {
			return "jdbc:oracle:thin:@dev04.qa.atl2.redhat.com:1521:qaora11";
		}

		public String getUser() {
			return "sebersole";
		}

		public String getPass() {
			return "sebersole";
		}
	};

	private Envionment ORACLE_RAC = new ContextualEnvironment() {
		public String getDriver() {
			return "oracle.jdbc.driver.OracleDriver";
		}

		public String getUrl() {
			return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(LOAD_BALANCE=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=vmg24-vip.mw.lab.eng.bos.redhat.com)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=vmg25-vip.mw.lab.eng.bos.redhat.com)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=qarac.jboss))";
		}

		public String getUser() {
			return "sebersole";
		}

		public String getPass() {
			return "sebersole";
		}
	};

	private Envionment envionment = ORACLE11g;

	protected void setUp() throws Exception {
		Class.forName( envionment.getDriver() );
	}

	public void testConnectedLobCreator() throws SQLException {
		final Connection connection = DriverManager.getConnection( envionment.getUrl(), envionment.getUser(), envionment.getPass() );
		final LobCreationContext lobCreationContext = new LobCreationContext() {
			public Object execute(Callback callback) {
				try {
					return callback.executeOnConnection( connection );
				}
				catch ( SQLException e ) {
					throw new RuntimeException( "Unexpected sql exception", e );
				}
			}
		};

		LobCreator lobCreator = JdbcSupportLoader.loadJdbcSupport( connection ).getLobCreator( lobCreationContext );
		envionment.verifyCreator( lobCreator );

		Blob blob = lobCreator.createBlob( new byte[] {} );
		envionment.verifyBlob( blob );
		blob = lobCreator.wrap( blob );
		assertTrue( blob instanceof WrappedBlob );

		Clob clob = lobCreator.createClob( "Hi" );
		envionment.verifyClob( clob );
		clob = lobCreator.wrap( clob );
		assertTrue( clob instanceof WrappedClob );

		Clob nclob = lobCreator.createNClob( "Hi" );
		envionment.verifyNClob( nclob );
		assertTrue( NClob.class.isInstance( nclob ) );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
		connection.close();
	}

	public void testLegacyLobCreator() throws SQLException {
		LobCreator lobCreator = JdbcSupportLoader.loadJdbcSupport( null ).getLobCreator();

		Blob blob = lobCreator.createBlob( new byte[] {} );
		assertTrue( blob instanceof BlobImplementer );
		blob = lobCreator.wrap( blob );
		assertTrue( blob instanceof WrappedBlob );

		Clob clob = lobCreator.createClob( "Hi" );
		assertTrue( clob instanceof ClobImplementer );
		clob = lobCreator.wrap( clob );
		assertTrue( clob instanceof WrappedClob );

		Clob nclob = lobCreator.createNClob( "Hi" );
		assertTrue( nclob instanceof NClobImplementer );
		assertTrue( NClob.class.isInstance( nclob ) );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
	}
}
