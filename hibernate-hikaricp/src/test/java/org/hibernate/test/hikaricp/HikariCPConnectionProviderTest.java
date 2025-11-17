/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Brett Meyer
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "The jTDS driver doesn't implement Connection#isValid so this fails")
@ServiceRegistry
@SessionFactory
public class HikariCPConnectionProviderTest {

	@Test
	public void testHikariCPConnectionProvider(SessionFactoryScope factoryScope) throws Exception {
		var serviceRegistry = factoryScope.getSessionFactory().getServiceRegistry();
		var jdbcServices = serviceRegistry.requireService( JdbcServices.class );
		var connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		var hikariProvider = assertTyping(
				HikariCPConnectionProvider.class,
				connectionAccess.getConnectionProvider()
		);

		// For simplicity's sake, using the following in hibernate.properties:
		// hibernate.hikari.minimumPoolSize 2
		// hibernate.hikari.maximumPoolSize 2
		final List<Connection> conns = new ArrayList<>();
		for ( int i = 0; i < 2; i++ ) {
			Connection conn = hikariProvider.getConnection();
			assertThat( conn ).isNotNull();
			assertThat( conn.isClosed() ).isFalse();
			conns.add( conn );
		}

		try {
			hikariProvider.getConnection();
			fail( "SQLException expected -- no more connections should have been available in the pool." );
		}
		catch (SQLException e) {
			// expected
			assertThat( e.getMessage() ).contains( "Connection is not available, request timed out after" );
		}

		for ( Connection conn : conns ) {
			hikariProvider.closeConnection( conn );
			assertThat( conn.isClosed() ).isTrue();
		}

		// NOTE : The JUnit 5 infrastructure versus the JUnit 4 infrastructure causes the
		//		StandardServiceRegistry (the SF registry's parent) to be created with auto-closure disabled.
		//		That is not the normal process.
		//		So here we explicitly close the parent.
		factoryScope.getSessionFactory().close();
		serviceRegistry.destroy();
		assert serviceRegistry.getParentServiceRegistry() != null : "Expecting parent service registry";
		( (ServiceRegistryImplementor) serviceRegistry.getParentServiceRegistry() ).destroy();


		try {
			hikariProvider.getConnection();
			fail( "Exception expected -- the pool should have been shutdown." );
		}
		catch (Exception e) {
			// expected
			assertThat( e.getMessage() ).contains( "has been closed" );
		}
	}
}
