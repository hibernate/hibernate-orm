/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob.locator;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.descriptor.java.DataHelper;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class LobLocatorTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { LobHolder.class };
	}

	/**
	 * Specific JDBC drivers (e.g. SQL Server) may not automatically rewind bound input stream
	 * during statement execution. Such behavior results in error message similar to:
	 * {@literal The stream value is not the specified length. The specified length was 4, the actual length is 0.}
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8193")
	@RequiresDialectFeature(DialectChecks.UsesInputStreamToInsertBlob.class)
	public void testStreamResetBeforeParameterBinding() throws SQLException {
		final Session session = openSession();

		session.getTransaction().begin();
		LobHolder entity = new LobHolder(
				session.getLobHelper().createBlob( "blob".getBytes() ),
				session.getLobHelper().createClob( "clob" ), 0
		);
		session.persist( entity );
		session.getTransaction().commit();

		final Integer updatesLimit = 3;

		for ( int i = 1; i <= updatesLimit; ++i ) {
			session.getTransaction().begin();
			entity = (LobHolder) session.get( LobHolder.class, entity.getId() );
			entity.setCounter( i );
			entity = (LobHolder) session.merge( entity );
			session.getTransaction().commit();
		}

		session.getTransaction().begin();
		entity = (LobHolder) session.get( LobHolder.class, entity.getId() );
		entity.setBlobLocator( session.getLobHelper().createBlob( "updated blob".getBytes() ) );
		entity.setClobLocator( session.getLobHelper().createClob( "updated clob" ) );
		entity = (LobHolder) session.merge( entity );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		checkState( "updated blob".getBytes(), "updated clob", updatesLimit, (LobHolder) session.get( LobHolder.class, entity.getId() ) );
		session.getTransaction().commit();

		session.close();
	}

	private void checkState(byte[] blob, String clob, Integer counter, LobHolder entity) throws SQLException {
		Assert.assertEquals( counter, entity.getCounter() );
		Assert.assertArrayEquals( blob, DataHelper.extractBytes( entity.getBlobLocator().getBinaryStream() ) );
		Assert.assertEquals( clob, DataHelper.extractString( entity.getClobLocator() ) );
	}
}
