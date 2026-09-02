/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.testing.SchemaGenerationResult;
import org.hibernate.dialect.testing.SqlGenerationRequest;
import org.hibernate.dialect.testing.SqlGenerationResult;
import org.hibernate.dialect.testing.spi.DialectContractProfile;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;

/// Standard connectionless implementation of a test-kit context.
///
/// @author Steve Ebersole
public final class StandardDialectTestContext implements DialectTestContextAccess {
	private static final Set<String> CONNECTION_SETTINGS = Set.of(
			"jakarta.persistence.jtaDataSource",
			"jakarta.persistence.nonJtaDataSource",
			"jakarta.persistence.jdbc.url",
			"jakarta.persistence.schema-generation-connection",
			"javax.persistence.jtaDataSource",
			"javax.persistence.nonJtaDataSource",
			"javax.persistence.jdbc.url",
			"javax.persistence.schema-generation-connection",
			"hibernate.connection.datasource",
			"hibernate.connection.provider_class",
			"hibernate.connection.url",
			"hibernate.multi_tenant_connection_provider"
	);
	private static final Set<String> SCHEMA_ACTION_SETTINGS = Set.of(
			"hibernate.hbm2ddl.auto",
			"jakarta.persistence.schema-generation.database.action",
			"jakarta.persistence.schema-generation.scripts.action",
			"javax.persistence.schema-generation.database.action",
			"javax.persistence.schema-generation.scripts.action"
	);

	private final Thread owner = Thread.currentThread();
	private final Dialect dialect;
	private final StandardServiceRegistry serviceRegistry;
	private final MetadataImplementor metadata;
	private final SessionFactoryImplementor sessionFactory;
	private boolean closed;

	public StandardDialectTestContext(DialectContractProfile profile) {
		Objects.requireNonNull( profile, "profile" );
		dialect = Objects.requireNonNull( profile.createDialect(), "profile.createDialect()" );

		final Map<String, Object> settings = new HashMap<>(
				Objects.requireNonNull( profile.settings(), "profile.settings()" )
		);
		validateSettings( settings );
		settings.put( AvailableSettings.DIALECT, dialect );
		settings.put( AvailableSettings.ALLOW_METADATA_ON_BOOT, false );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "none" );

		serviceRegistry = new StandardServiceRegistryBuilder().applySettings( settings ).build();
		try {
			metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
					.addAnnotatedClass( ContractEntity.class )
					.buildMetadata();
			metadata.validate();
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}
		catch (RuntimeException failure) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
			throw failure;
		}
	}

	@Override
	public Dialect getDialect() {
		checkOpen();
		return dialect;
	}

	@Override
	public SqlGenerationResult translate(SqlGenerationRequest request) {
		checkOpen();
		return InternalSqlGenerator.translate( request, sessionFactory );
	}

	@Override
	public SchemaGenerationResult generateSchema() {
		checkOpen();
		final var createCommands = new SchemaCreatorImpl( serviceRegistry )
				.generateCreationCommands( metadata, false );
		final var dropTarget = new CollectingGenerationTarget();
		new SchemaDropperImpl( serviceRegistry ).doDrop(
				metadata,
				serviceRegistry,
				Map.of(),
				false,
				dropTarget
		);
		return new SchemaGenerationResult( createCommands, dropTarget.commands() );
	}

	@Override
	public void close() {
		checkThread();
		if ( !closed ) {
			closed = true;
			try {
				sessionFactory.close();
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}

	private void checkOpen() {
		checkThread();
		if ( closed ) {
			throw new IllegalStateException( "DialectTestContext is closed" );
		}
	}

	private void checkThread() {
		if ( owner != Thread.currentThread() ) {
			throw new IllegalStateException( "DialectTestContext is thread-confined" );
		}
	}

	private static void validateSettings(Map<String, Object> settings) {
		if ( settings.containsKey( AvailableSettings.DIALECT ) ) {
			throw new IllegalArgumentException( "Profile settings must not select an alternate Dialect" );
		}
		if ( Boolean.parseBoolean( String.valueOf( settings.get( AvailableSettings.ALLOW_METADATA_ON_BOOT ) ) ) ) {
			throw new IllegalArgumentException( "Profile settings must not enable JDBC metadata access" );
		}
		for ( String setting : CONNECTION_SETTINGS ) {
			if ( settings.containsKey( setting ) ) {
				throw new IllegalArgumentException( "Profile settings must not provide a connection: " + setting );
			}
		}
		for ( String setting : SCHEMA_ACTION_SETTINGS ) {
			final Object action = settings.get( setting );
			if ( action != null && !"none".equalsIgnoreCase( action.toString() ) ) {
				throw new IllegalArgumentException( "Profile settings must not request schema execution: " + setting );
			}
		}
	}
}
