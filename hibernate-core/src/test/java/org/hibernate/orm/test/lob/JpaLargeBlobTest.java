/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.util.Random;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Brett Meyer
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-7698")
@RequiresDialect(value = H2Dialect.class, comment = "HHH-7724")
@DomainModel(annotatedClasses = LobEntity.class)
@SessionFactory
public class JpaLargeBlobTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void jpaBlobStream(SessionFactoryScope scope) throws Exception {
		LobEntity o = new LobEntity();
		LobInputStream inputStream = scope.fromSession(
				session -> {
					LobInputStream lis = new LobInputStream();

					session.getTransaction().begin();
					try {
						Blob blob = getLobHelper().createBlob( lis, LobEntity.BLOB_LENGTH );
						o.setBlob( blob );

						// Regardless if NON_CONTEXTUAL_LOB_CREATION is set to true,
						// ContextualLobCreator should use a NonContextualLobCreator to create
						// a blob Proxy.  If that's the case, the InputStream will not be read
						// until it's persisted with the JDBC driver.
						// Although HHH-7698 was about high memory consumption, this is the best
						// way to test that the high memory use is being prevented.
						assertFalse( lis.wasRead() );

						session.persist( o );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					assertTrue( lis.wasRead() );
					return lis;
				}
		);
		inputStream.close();
	}

	private class LobInputStream extends InputStream {
		private boolean read = false;
		private Long count = (long) 200 * 1024 * 1024;

		@Override
		public int read() throws IOException {
			read = true;
			if ( count > 0 ) {
				count--;
				return new Random().nextInt();
			}
			return -1;
		}

		@Override
		public int available() throws IOException {
			return 1;
		}

		public boolean wasRead() {
			return read;
		}
	}
}
