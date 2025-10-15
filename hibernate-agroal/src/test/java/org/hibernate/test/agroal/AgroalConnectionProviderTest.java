/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.agroal;

import org.hibernate.agroal.internal.AgroalConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Brett Meyer
 */
public class AgroalConnectionProviderTest {

	@Test
	@ServiceRegistry
	@DomainModel
	@SessionFactory
	public void testAgroalConnectionProvider(ServiceRegistryScope registryScope, SessionFactoryScope factoryScope) throws Exception {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		var serviceRegistry = registryScope.getRegistry();

		JdbcServices jdbcServices = serviceRegistry.requireService( JdbcServices.class );
		var connectionAccess = assertTyping( ConnectionProviderJdbcConnectionAccess.class, jdbcServices.getBootstrapJdbcConnectionAccess() );
		assertThat( connectionAccess ).isInstanceOf( ConnectionProviderJdbcConnectionAccess.class );
		var agroalConnectionProvider = assertTyping( AgroalConnectionProvider.class, connectionAccess.getConnectionProvider() );

		// For simplicity's sake, using the following in hibernate.properties:
		// hibernate.agroal.maxSize 2
		// hibernate.agroal.minSize 2
		List<Connection> conns = new ArrayList<>();
		for ( int i = 0; i < 2; i++ ) {
			Connection conn = agroalConnectionProvider.getConnection();
			assertThat( conn ).isNotNull();
			assertThat( conn.isClosed() ).isFalse();
			conns.add( conn );
		}

		try {
			agroalConnectionProvider.getConnection();
			fail( "SQLException expected -- no more connections should have been available in the pool." );
		}
		catch (SQLException e) {
			// expected
			assertThat( e.getMessage() ).contains( "timeout" );
		}

		for ( Connection conn : conns ) {
			agroalConnectionProvider.closeConnection( conn );
			assertThat( conn.isClosed() ).isTrue();
		}

		// NOTE : The JUnit 5 infrastructure versus the JUnit 4 infrastructure causes the
		//		StandardServiceRegistry (the SF registry's parent) to be created with auto-closure disabled.
		//		That is not the normal process.
		//		So here we explicitly close the parent.
		sessionFactory.close();
		( (ServiceRegistryImplementor) serviceRegistry ).destroy();
		assert serviceRegistry.getParentServiceRegistry() != null : "Expecting parent service registry";
		( (ServiceRegistryImplementor) serviceRegistry.getParentServiceRegistry() ).destroy();

		try {
			agroalConnectionProvider.getConnection();
			fail( "Exception expected -- the pool should have been shutdown." );
		}
		catch (Exception e) {
			// expected
			assertThat( e.getMessage() ).containsAnyOf( "closed", "shutting down" );
		}
	}
}
