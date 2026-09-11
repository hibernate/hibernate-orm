/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.identity;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.BatchSettings.BATCH_IDENTITY_INSERTS;
import static org.hibernate.cfg.BatchSettings.ORDER_INSERTS;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.JdbcSettings.DIALECT_NATIVE_PARAM_MARKERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ServiceRegistryFunctionalTesting
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-20023")
@DomainModel(
		annotatedClasses = {
				BatchingIdentityGeneratedKeysTest.BatchIdentityEntity.class,
				BatchingIdentityGeneratedKeysTest.BatchIdentityParent.class,
				BatchingIdentityGeneratedKeysTest.BatchIdentityChild.class
		}
)
@SessionFactory
public class BatchingIdentityGeneratedKeysTest implements ServiceRegistryProducer {
	private static final Method ADD_BATCH = resolveMethod( PreparedStatement.class, "addBatch" );
	private static final Method EXECUTE_BATCH = resolveMethod( PreparedStatement.class, "executeBatch" );
	private static final Method GET_GENERATED_KEYS = resolveMethod( Statement.class, "getGeneratedKeys" );

	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
		connectionProvider.clear();
	}

	@AfterAll
	void releaseResources() {
		connectionProvider.stop();
	}

	@Test
	void testIdentityInsertsUseExecuteBatchAndGetGeneratedKeys(SessionFactoryScope scope) {
		connectionProvider.clear();
		scope.inTransaction(
				session -> {
					final var first = new BatchIdentityEntity( "first" );
					final var second = new BatchIdentityEntity( "second" );

					session.persist( first );
					session.persist( second );

					assertNull( first.getId() );
					assertNull( second.getId() );

					session.flush();

					assertNotNull( first.getId() );
					assertNotNull( second.getId() );
				}
		);

		final PreparedStatement statement = findPreparedStatement( "batch_identity_entity" );
		assertEquals( 2, callCount( ADD_BATCH, statement ) );
		assertEquals( 1, callCount( EXECUTE_BATCH, statement ) );
		assertEquals( 1, callCount( GET_GENERATED_KEYS, statement ) );
	}

	@Test
	void testGeneratedParentIdsAreAssignedBeforeDependentChildInserts(SessionFactoryScope scope) {
		connectionProvider.clear();
		scope.inTransaction(
				session -> {
					final var firstParent = new BatchIdentityParent( "first-parent" );
					firstParent.addChild( new BatchIdentityChild( 1L, "first-child" ) );
					final var secondParent = new BatchIdentityParent( "second-parent" );
					secondParent.addChild( new BatchIdentityChild( 2L, "second-child" ) );

					session.persist( firstParent );
					session.persist( secondParent );
				}
		);

		scope.inTransaction(
				session -> {
					final var children = session.createQuery(
							"from BatchIdentityChild c order by c.id",
							BatchIdentityChild.class
					).getResultList();
					assertEquals( 2, children.size() );
					assertNotNull( children.get( 0 ).getParent().getId() );
					assertNotNull( children.get( 1 ).getParent().getId() );
				}
		);

		final var statement = findPreparedStatement( "batch_identity_parent" );
		assertEquals( 2, callCount( ADD_BATCH, statement ) );
		assertEquals( 1, callCount( EXECUTE_BATCH, statement ) );
		assertEquals( 1, callCount( GET_GENERATED_KEYS, statement ) );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder registryBuilder) {
		registryBuilder.applySetting( DIALECT, BatchIdentityH2Dialect.class.getName() );
		registryBuilder.applySetting( DIALECT_NATIVE_PARAM_MARKERS, Boolean.FALSE );
		registryBuilder.applySetting( STATEMENT_BATCH_SIZE, 5 );
		registryBuilder.applySetting( ORDER_INSERTS, Boolean.TRUE );
		registryBuilder.applySetting( BATCH_IDENTITY_INSERTS, Boolean.TRUE );
		final Object configuredConnectionProvider = registryBuilder.getSettings().get( CONNECTION_PROVIDER );
		if ( configuredConnectionProvider != null ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) configuredConnectionProvider );
		}
		registryBuilder.applySetting( CONNECTION_PROVIDER, connectionProvider );
		return registryBuilder.build();
	}

	private PreparedStatement findPreparedStatement(String tableName) {
		for ( var entry : connectionProvider.getPreparedStatementsAndSql().entrySet() ) {
			if ( entry.getValue().toLowerCase( Locale.ROOT ).contains( "insert into " + tableName ) ) {
				return entry.getKey();
			}
		}
		throw new IllegalArgumentException( "No prepared statement found for table `" + tableName + "'" );
	}

	private int callCount(Method method, PreparedStatement statement) {
		return connectionProvider.spyContext.getCalls( method, statement ).size();
	}

	private static Method resolveMethod(Class<?> type, String name) {
		try {
			return type.getMethod( name );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	public static class BatchIdentityH2Dialect extends H2Dialect {
		@Override
		public boolean supportsBatchInsertReturningGeneratedKeys() {
			return true;
		}
	}

	@Entity(name = "BatchIdentityEntity")
	@Table(name = "batch_identity_entity")
	public static class BatchIdentityEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		public BatchIdentityEntity() {
		}

		public BatchIdentityEntity(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "BatchIdentityParent")
	@Table(name = "batch_identity_parent")
	public static class BatchIdentityParent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
		private List<BatchIdentityChild> children = new ArrayList<>();

		public BatchIdentityParent() {
		}

		public BatchIdentityParent(String name) {
			this.name = name;
		}

		public void addChild(BatchIdentityChild child) {
			children.add( child );
			child.parent = this;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "BatchIdentityChild")
	@Table(name = "batch_identity_child")
	public static class BatchIdentityChild {
		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "parent_id", nullable = false)
		private BatchIdentityParent parent;

		public BatchIdentityChild() {
		}

		public BatchIdentityChild(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public BatchIdentityParent getParent() {
			return parent;
		}
	}
}
