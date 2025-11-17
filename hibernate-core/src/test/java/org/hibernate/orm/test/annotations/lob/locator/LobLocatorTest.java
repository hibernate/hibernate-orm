/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob.locator;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.DataHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.fail;
import static org.hibernate.Hibernate.getLobHelper;

/**
 * @author Lukasz Antoniak
 */
@DomainModel(
		annotatedClasses = {
				LobHolder.class
		}
)
@SessionFactory
public class LobLocatorTest {

	/**
	 * Specific JDBC drivers (e.g. SQL Server) may not automatically rewind bound input stream
	 * during statement execution. Such behavior results in error message similar to:
	 * {@literal The stream value is not the specified length. The specified length was 4, the actual length is 0.}
	 */
	@Test
	@JiraKey(value = "HHH-8193")
	@RequiresDialectFeature(feature = DialectFeatureChecks.UsesInputStreamToInsertBlob.class)
	public void testStreamResetBeforeParameterBinding(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					LobHolder entity = new LobHolder(
							getLobHelper().createBlob( "blob".getBytes() ),
							getLobHelper().createClob( "clob" ), 0
					);
					session.persist( entity );
					session.getTransaction().commit();

					final int updatesLimit = 3;

					for ( int i = 1; i <= updatesLimit; ++i ) {
						session.getTransaction().begin();
						entity = session.find( LobHolder.class, entity.getId() );
						entity.setCounter( i );
						entity = session.merge( entity );
						session.getTransaction().commit();
					}

					session.getTransaction().begin();
					entity = session.find( LobHolder.class, entity.getId() );
					entity.setBlobLocator( getLobHelper().createBlob( "updated blob".getBytes() ) );
					entity.setClobLocator( getLobHelper().createClob( "updated clob" ) );
					entity = session.merge( entity );
					session.getTransaction().commit();

					session.clear();

					session.getTransaction().begin();
					try {
						checkState(
								"updated blob".getBytes(),
								"updated clob",
								updatesLimit,
								session.find( LobHolder.class, entity.getId() )
						);
					}
					catch (Exception e) {
						fail( e );
					}
				}
		);
	}

	private void checkState(byte[] blob, String clob, Integer counter, LobHolder entity) throws Exception {
		assertThat( entity.getCounter() ).isEqualTo( counter );
		assertThat( DataHelper.extractBytes( entity.getBlobLocator().getBinaryStream() ) ).isEqualTo( blob );
		assertThat( DataHelper.extractString( entity.getClobLocator() ) ).isEqualTo( clob );
	}
}
