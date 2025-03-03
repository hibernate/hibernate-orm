/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.sequence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.PropertiesHelper;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@BaseUnitTest
@RequiresDialect( H2Dialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18337" )
public class PooledWithCustomNamingStrategyTest {
	@Test
	public void testWrongIncrementSize() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( AvailableSettings.PHYSICAL_NAMING_STRATEGY, MyPhysicalNamingStrategy.INSTANCE );
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( WrongEntity.class )
				.buildMetadata();
		try (final SessionFactory sf = metadata.buildSessionFactory()) {
			fail( "Default increment size of [50] should not work with the database sequence increment size [1]." );
		}
		catch (Exception e) {
			assertThat( e.getMessage() ).isEqualTo(
					"The increment size of the [MY_SEQ] sequence is set to [50] in the entity " +
							"mapping while the associated database sequence increment size is [5]."
			);
		}
	}

	@Test
	public void testRightIncrementSize() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( AvailableSettings.PHYSICAL_NAMING_STRATEGY, MyPhysicalNamingStrategy.INSTANCE );
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( RightEntity.class )
				.buildMetadata();
		try (final SessionFactoryImplementor sf = (SessionFactoryImplementor) metadata.buildSessionFactory()) {
			// session factory should be created correctly
			inTransaction( sf, session -> session.persist( new RightEntity() ) );
			inTransaction( sf, session -> {
				final RightEntity result = session.createQuery( "from RightEntity", RightEntity.class )
						.getSingleResult();
				assertThat( result.id ).isNotNull();
			} );
		}
		catch (Exception e) {
			fail( "Expected configured increment size of [1] to be compatible with the existing sequence" );
		}
	}

	ConnectionProvider connectionProvider;

	@BeforeAll
	public void setUp() {
		final DriverManagerConnectionProviderImpl provider = new DriverManagerConnectionProviderImpl();
		provider.configure( PropertiesHelper.map( Environment.getProperties() ) );
		connectionProvider = provider;
		try (final Connection connection = connectionProvider.getConnection();
			final Statement statement = connection.createStatement()) {
			statement.execute( "create sequence MY_SEQ start with 1 increment by 5" );
			statement.execute( "create table RightEntity(id bigint not null, primary key (id))" );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Failed to setup the test", e );
		}
	}

	@AfterAll
	public void tearDown() {
		try (final Connection connection = connectionProvider.getConnection();
			final Statement statement = connection.createStatement()) {
			statement.execute( "drop sequence MY_SEQ" );
			statement.execute( "drop table RightEntity" );
		}
		catch (SQLException e) {
		}
	}

	@Entity( name = "WrongEntity" )
	static class WrongEntity {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my-sequence-generator" )
		@SequenceGenerator( name = "my-sequence-generator", sequenceName = "REPLACE_SEQ" )
		private Long id;
	}

	@Entity( name = "RightEntity" )
	static class RightEntity {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my-sequence-generator" )
		@SequenceGenerator( name = "my-sequence-generator", sequenceName = "REPLACE_SEQ", allocationSize = 5 )
		private Long id;
	}

	static class MyPhysicalNamingStrategy implements PhysicalNamingStrategy {
		static MyPhysicalNamingStrategy INSTANCE = new MyPhysicalNamingStrategy();

		@Override
		public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName;
		}

		@Override
		public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName;
		}

		@Override
		public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName;
		}

		@Override
		public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return Identifier.toIdentifier(
					logicalName.getText().replaceAll( "REPLACE_", "MY_" ),
					logicalName.isQuoted()
			);
		}

		@Override
		public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName;
		}
	}
}
