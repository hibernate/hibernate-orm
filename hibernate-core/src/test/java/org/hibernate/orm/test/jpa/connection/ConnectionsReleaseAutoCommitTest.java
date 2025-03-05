/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.connection;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.util.connections.ConnectionCheckingConnectionProvider;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12197")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = { ConnectionsReleaseAutoCommitTest.Thing.class },
		integrationSettings = @Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.orm.test.util.connections.ConnectionCheckingConnectionProvider")
)
public class ConnectionsReleaseAutoCommitTest {

	@Test
	public void testConnectionAcquisitionCount(EntityManagerFactoryScope scope) {
		ConnectionCheckingConnectionProvider connectionProvider = getConnectionProvider( scope );
		assertTrue( connectionProvider.areAllConnectionClosed() );
		connectionProvider.clear();

		scope.inTransaction( entityManager -> {
			// Force connection acquisition
			entityManager.createQuery( "select 1" ).getResultList();
			assertEquals( 1, connectionProvider.getTotalOpenedConnectionCount() );
			Thing thing = new Thing();
			thing.setId( 1 );
			entityManager.persist( thing );
			assertEquals( 1, connectionProvider.getTotalOpenedConnectionCount() );
		} );

		assertEquals( 1, connectionProvider.getTotalOpenedConnectionCount() );
		assertTrue( connectionProvider.areAllConnectionClosed() );
	}

	private ConnectionCheckingConnectionProvider getConnectionProvider(EntityManagerFactoryScope scope) {
		return (ConnectionCheckingConnectionProvider) ( (SessionFactoryImplementor) ( scope
				.getEntityManagerFactory() ) ).getServiceRegistry().getService( ConnectionProvider.class );
	}

	@Entity(name = "Thing")
	@Table(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
