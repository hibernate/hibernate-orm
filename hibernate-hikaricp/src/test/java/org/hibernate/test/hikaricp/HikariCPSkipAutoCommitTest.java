/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.hikaricp;

import java.sql.Connection;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.util.PreparedStatementSpyConnectionProvider;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(value = SybaseDialect.class, comment = "The jTDS driver doesn't implement Connection#isValid so this fails")
public class HikariCPSkipAutoCommitTest extends BaseCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider =
			new PreparedStatementSpyConnectionProvider();

	@Override
	protected void configure(Configuration configuration) {
		configuration.getProperties().put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		configuration.getProperties().put( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, Boolean.TRUE );
		configuration.getProperties().put( "hibernate.hikari.autoCommit", Boolean.FALSE.toString() );
	}

	@Override
	public void releaseSessionFactory() {
		super.releaseSessionFactory();
		connectionProvider.stop();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				City.class,
		};
	}

	@Test
	public void test() {
		connectionProvider.clear();
		doInHibernate( this::sessionFactory, session -> {
			City city = new City();
			city.setId( 1L );
			city.setName( "Cluj-Napoca" );
			session.persist( city );

			assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );
			assertTrue( connectionProvider.getReleasedConnections().isEmpty() );
		} );
		verifyConnections();

		connectionProvider.clear();
		doInHibernate( this::sessionFactory, session -> {
			City city = session.find( City.class, 1L );
			assertEquals( "Cluj-Napoca", city.getName() );
		} );
		verifyConnections();
	}

	private void verifyConnections() {
		assertTrue( connectionProvider.getAcquiredConnections().isEmpty() );

		List<Connection> connections = connectionProvider.getReleasedConnections();
		assertEquals( 1, connections.size() );
		try {
			List<Object[]> setAutoCommitCalls = connectionProvider.spyContext.getCalls(
					Connection.class.getMethod( "setAutoCommit", boolean.class ),
					connections.get( 0 )
			);
			assertTrue( "setAutoCommit should never be called", setAutoCommitCalls.isEmpty() );
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
