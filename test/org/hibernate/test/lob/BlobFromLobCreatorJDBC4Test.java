//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.lob;

import java.sql.SQLException;
import java.sql.Blob;

import junit.framework.Test;

import org.hibernate.lob.LobCreatorImplJDBC4;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.Session;
import org.hibernate.HibernateException;

/**
 * This class extends AbstractBlobFromLobCreatorTest to determine if the
 * correct LobCreator impl is used when Environment.USE_CONNECTION_FOR_LOB_CREATION
 * is set to true. The actual impl should be the JDBC4 LobCreator, even when
 * the JVM and/or JDBC driver do not support JDBC4. The JDBC4 LobCreator uses the
 * Connection to create LOBs.
 *
 * @author Gail Badner
 */
public class BlobFromLobCreatorJDBC4Test extends AbstractBlobFromLobCreatorTest {

	public BlobFromLobCreatorJDBC4Test(String name) {
		super( name );
	}

	protected Boolean getUseConnectionForLobCreationPropertyValue() {
		return Boolean.TRUE;
	}

	protected Class getExpectedLobCreatorClass() {
		return LobCreatorImplJDBC4.class;
	}
	
	public static Test suite() {
		return new FunctionalTestClassTestSuite( BlobFromLobCreatorJDBC4Test.class );
	}

	protected boolean skipLobLocatorTests()  throws SQLException {
		if ( ! Environment.jvmSupportsJDBC4() ) {
			reportSkip( "JVM does not support creating LOBs using the connection", "LOB support" );
			return true;
		}
		if ( ! jvmAndDriverSupportUseConnectionForLobCreation() ) {
			reportSkip( "Driver does not support creating LOBs using the connection", "LOB support" );
			return true;
		}
		return false;
	}

	public void testCreateBlobLocatorSupport() throws Throwable {
		Session s = openSession();
		s.getTransaction().begin();
		byte[] bytes = buildRecursively( BLOB_SIZE, true );
		try {
			Blob blob = createBlobLocator( s, bytes );
			if ( ! Environment.jvmSupportsJDBC4() ) {
				fail( "should have thrown NoSuchMethodException" );
			}
			else if ( ! jvmAndDriverSupportUseConnectionForLobCreation() ) {
				fail( "should have thrown AbstractMethodException" );
			}
			assertEquals( bytes, extractData( blob ) );
			s.getTransaction().commit();
		}
		catch( HibernateException e ) {
			s.getTransaction().rollback();
			if ( e.getCause() instanceof NoSuchMethodException ) {
				assertTrue( ! Environment.jvmSupportsJDBC4() &&
						isUseConnectionForLobCreationEnabled() );
			}
			else if ( e.getCause() instanceof AbstractMethodError ) {
				assertTrue( Environment.jvmSupportsJDBC4() &&
						! jvmAndDriverSupportUseConnectionForLobCreation() &&
						isUseConnectionForLobCreationEnabled() );
			}
			else {
				throw e;
			}
		}
		finally {
			s.close();
		}
	}
}