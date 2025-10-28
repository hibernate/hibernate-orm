/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob.locator;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.descriptor.java.DataHelper;

import static org.hibernate.Hibernate.getLobHelper;

/**
 * @author Lukasz Antoniak
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
	@JiraKey(value = "HHH-8193")
	@RequiresDialectFeature(DialectChecks.UsesInputStreamToInsertBlob.class)
	public void testStreamResetBeforeParameterBinding() throws SQLException {
		final Session session = openSession();

		session.getTransaction().begin();
		LobHolder entity = new LobHolder(
				getLobHelper().createBlob( "blob".getBytes() ),
				getLobHelper().createClob( "clob" ), 0
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
		entity.setBlobLocator( getLobHelper().createBlob( "updated blob".getBytes() ) );
		entity.setClobLocator( getLobHelper().createClob( "updated clob" ) );
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
