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
import java.sql.Connection;
import java.sql.Clob;
import java.io.IOException;
import java.io.StringReader;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.AssertionFailure;
import org.hibernate.SessionException;
import org.hibernate.lob.LobCreator;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;

/**
 * This class extends AbstractClobTest to provide additional tests when
 * using a LobCreator to create LOB locators.
 *
 * @author Gail Badner
 */
public abstract class AbstractClobFromLobCreatorTest extends AbstractClobTest {

	public AbstractClobFromLobCreatorTest(String name) {
		super( name );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		if ( getUseConnectionForLobCreationPropertyValue() == null ) {
			cfg.setProperty( Environment.USE_CONNECTION_FOR_LOB_CREATION, "" );
		}
		else {
			cfg.setProperty(
					Environment.USE_CONNECTION_FOR_LOB_CREATION,
					String.valueOf( getUseConnectionForLobCreationPropertyValue().booleanValue() )
			);
		}
	}

	public boolean appliesTo(Dialect dialect) {
		if ( ! dialect.supportsExpectedLobUsagePattern() ) {
			reportSkip( "database/driver does not support expected LOB usage pattern", "LOB support" );
			return false;
		}
		return true;
	}

	protected abstract Boolean getUseConnectionForLobCreationPropertyValue();

	protected Clob createClobLocator(Session s, String str) {
		return Hibernate.getLobCreator( s ).createClob( str );
	}

	protected Clob createClobLocatorFromStreamUsingLength(Session s, String str) throws IOException {
		return Hibernate.getLobCreator( s ).createClob( new StringReader( str ), str.length() );
	}

	protected abstract Class getExpectedLobCreatorClass() throws SQLException ;

	public void testUseConnectionForLobCreationEnabledSetting() throws SQLException {
		Boolean propVal = getUseConnectionForLobCreationPropertyValue();
		if ( propVal == null ) {
			if ( jvmAndDriverSupportUseConnectionForLobCreation() != isUseConnectionForLobCreationEnabled() ) {
				throw new AssertionFailure( "JVM and JDBC driver support is inconsistent with setting for using Connection to create LOBs." );

			}
		}
		else {
			if ( propVal.booleanValue() != isUseConnectionForLobCreationEnabled() ) {
				throw new AssertionFailure( "Non-null Environment.USE_CONNECTION_FOR_LOB_CREATION value is inconstent with setting." );
			}
		}
	}

	public void testCorrectLobCreator() throws Exception {
		Session s = openSession();
		LobCreator lobCreator = Hibernate.getLobCreator( s );
		assertEquals( getExpectedLobCreatorClass(), lobCreator.getClass() );
		s.close();
	}

	public void testGetLobCreatorWithNullSession() {
		try {
			Hibernate.getLobCreator( null );
			fail( "should have failed with null session" );
		}
		catch ( AssertionFailure ex ) {
			// expected
		}
	}

	public void testCreateClobLocatorAfterSessionClose() throws Throwable {
		Session s = openSession();
		s.close();
		String str = buildRecursively( CLOB_SIZE, 'x');
		try {
			createClobLocator( s, str );
			if ( isUseConnectionForLobCreationEnabled() ) {
				fail( "should have failed w/ SessionException" );
			}
		}
		catch ( SessionException e ) {
			if ( ! isUseConnectionForLobCreationEnabled() ) {
				fail( "should not have failed w/ SessionException" );
			}
		}
	}

	protected boolean isUseConnectionForLobCreationEnabled() {
		return ( ( SessionFactoryImplementor ) getSessions() ).getSettings().isUseConnectionForLobCreationEnabled();
	}

	protected boolean jvmAndDriverSupportUseConnectionForLobCreation() throws SQLException {
		if ( ! Environment.jvmSupportsJDBC4() ) {
			return false;
		}
		Class c = ( ( SessionFactoryImplementor ) getSessions() ).getConnectionProvider().getConnection().getClass();
		try {
			return !( Connection.class.equals( c.getMethod( "createBlob", new Class[0] ).getDeclaringClass() ) ||
					Connection.class.equals( c.getMethod( "createClob", new Class[0] ).getDeclaringClass() ) );
		}
		catch ( NoSuchMethodException e ) {
			return false;
		}
	}

}