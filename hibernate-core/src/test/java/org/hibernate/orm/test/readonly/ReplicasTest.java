/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@ServiceRegistry(settings = @Setting(name = JdbcSettings.CONNECTION_PROVIDER,
		value = "org.hibernate.orm.test.readonly.ReplicasTest$Provider" ))
@DomainModel(annotatedClasses = ReplicasTest.Thing.class)
class ReplicasTest {
	@Test void testStateful(SessionFactoryScope scope) {
		readOnlyConnectionsOpened = 0;
		readOnlyConnectionsClosed = 0;
		var factory = scope.getSessionFactory();
		factory.getSchemaManager().truncate();
		Session s1 =
				factory.withOptions()
//						.connectionHandling( AS_NEEDED, ON_CLOSE )
						.readOnly(true)
						.openSession();
		s1.inTransaction( tx -> {
			assertThrows( IllegalStateException.class,
					() -> s1.persist( new Thing() ) );
			assertThrows( IllegalStateException.class,
					() -> s1.merge( new Thing() ) );
		} );
		s1.close();
		scope.inTransaction(s -> s.persist(new Thing()));
		Session s2 =
				factory.withOptions().readOnly(true)
						.openSession();
		s2.inTransaction( tx -> {
			Thing thing = s2.find( Thing.class, 2L );
			assertTrue( s2.isReadOnly( thing ) );
			assertThrows( IllegalStateException.class,
					() -> s1.remove( thing ) );
//			s2.doWork( connection -> assertTrue( connection.isReadOnly() ) );
		} );
		s2.close();
		assert readOnlyConnectionsOpened == 1;
		assert readOnlyConnectionsClosed == 1;
	}

	@Test void testStateless(SessionFactoryScope scope) {
		readOnlyConnectionsOpened = 0;
		readOnlyConnectionsClosed = 0;
		var factory = scope.getSessionFactory();
		factory.getSchemaManager().truncate();
		StatelessSession s1 =
				factory.withStatelessOptions()
//						.connectionHandling( AS_NEEDED, ON_CLOSE )
						.readOnly(true)
						.openStatelessSession();
		s1.inTransaction( tx -> {
			assertThrows( IllegalStateException.class,
					() -> s1.insert( new Thing() ) );
		} );
		s1.close();
		scope.inTransaction(s -> s.persist(new Thing()));
		StatelessSession s2 =
				factory.withStatelessOptions().readOnly(true)
						.openStatelessSession();
		s2.inTransaction( tx -> {
			Thing thing = s2.get( Thing.class, 2L );
			assertThrows( IllegalStateException.class,
					() -> s1.update( thing ) );
			assertThrows( IllegalStateException.class,
					() -> s1.delete( thing ) );
		} );
		s2.close();
		assert readOnlyConnectionsOpened == 1;
		assert readOnlyConnectionsClosed == 1;
	}

	static int readOnlyConnectionsOpened;
	static int readOnlyConnectionsClosed;

	public static class Provider extends DriverManagerConnectionProvider {
		@Override
		public Connection getReadOnlyConnection() throws SQLException {
			readOnlyConnectionsOpened++;
			return super.getReadOnlyConnection();
		}

		@Override
		public void closeReadOnlyConnection(Connection connection) throws SQLException {
			super.closeReadOnlyConnection( connection );
			readOnlyConnectionsClosed ++;
		}
	}

	@Entity
	static class Thing {
		@Id
		Long id = 2L;
	}
}
