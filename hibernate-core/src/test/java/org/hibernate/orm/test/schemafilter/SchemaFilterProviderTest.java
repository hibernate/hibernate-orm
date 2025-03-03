/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemafilter;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17763" )
@RequiresDialect(H2Dialect.class)
public class SchemaFilterProviderTest {
	@Test
	public void testValidationDefaultProvider() {
		testSchemaValidation( false );
	}

	@Test
	public void testValidationCustomProvider() {
		testSchemaValidation( true );
	}

	private void testSchemaValidation(boolean useCustomFilterProvider) {
		final Map<String, Object> options = getFilterProviderConfig( useCustomFilterProvider );
		withServiceRegistry( options, (serviceRegistry, metadata) -> {
			try {
				TransactionUtil.doWithJDBC( serviceRegistry, connection -> {
					try (final Statement statement = connection.createStatement()) {
						statement.executeUpdate( "create table entity_1(id integer not null, primary key (id))" );
						// create entity_2 table with wrong column type for `name`
						statement.executeUpdate(
								"create table entity_2(id integer not null, name integer, primary key (id))"
						);
					}
				} );

				final HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
				tool.injectServices( serviceRegistry );
				try {
					tool.getSchemaValidator( options ).doValidation( metadata, new ExecutionOptions() {
						@Override
						public boolean shouldManageNamespaces() {
							return Helper.interpretNamespaceHandling( options );
						}

						@Override
						public Map<String, Object> getConfigurationValues() {
							return options;
						}

						@Override
						public ExceptionHandler getExceptionHandler() {
							return ExceptionHandlerHaltImpl.INSTANCE;
						}
					}, ContributableMatcher.ALL );
					if ( !useCustomFilterProvider ) {
						fail( "Expected schema validation to fail on Entity2#name field" );
					}
				}
				catch (Exception e) {
					if ( useCustomFilterProvider ) {
						fail( "Unexpected exception when creating session factory", e );
					}
					else {
						assertThat( e ).hasMessageContaining(
								"wrong column type encountered in column [name] in table [entity_2]"
						);
					}
				}

				TransactionUtil.doWithJDBC( serviceRegistry, connection -> {
					try (final Statement statement = connection.createStatement()) {
						statement.executeUpdate( "drop table entity_1" );
						statement.executeUpdate( "drop table entity_2" );
					}
				} );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );
	}

	@Test
	public void testCreationDefaultFilter() {
		testSchemaCreation( false );
	}

	@Test
	public void testCreationCustomFilter() {
		testSchemaCreation( true );
	}

	private void testSchemaCreation(boolean useCustomFilterProvider) {
		final Map<String, Object> options = getFilterProviderConfig( useCustomFilterProvider );
		withServiceRegistry( options, (serviceRegistry, metadata) -> {
			final HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
			tool.injectServices( serviceRegistry );
			final SchemaCreatorImpl schemaCreator = (SchemaCreatorImpl) tool.getSchemaCreator( options );
			final List<String> commands = schemaCreator.generateCreationCommands(
					metadata,
					false
			);
			assertThat( commands ).hasSize( useCustomFilterProvider ? 1 : 2 );
			assertThat( commands ).anyMatch( s -> s.startsWith( "create table entity_1" ) );
			if ( useCustomFilterProvider ) {
				assertThat( commands ).noneMatch( s -> s.startsWith( "create table entity_2" ) );
			}
			else {
				assertThat( commands ).anyMatch( s -> s.startsWith( "create table entity_2" ) );
			}
		} );
	}

	private void withServiceRegistry(
			Map<String, Object> configurationValues,
			BiConsumer<StandardServiceRegistryImpl, Metadata> consumer) {
		final Map<String, Object> environmentProperties = PropertiesHelper.map( Environment.getProperties() );
		final Map<String, Object> settings = new HashMap<>( environmentProperties.size() + configurationValues.size() );
		settings.putAll( environmentProperties );
		settings.putAll( configurationValues );
		try (final StandardServiceRegistryImpl serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( settings )) {
			consumer.accept( serviceRegistry, new MetadataSources( serviceRegistry ).addAnnotatedClasses(
					Entity1.class,
					Entity2.class
			).buildMetadata() );
		}
	}

	private Map<String, Object> getFilterProviderConfig(boolean useCustomFilterProvider) {
		return useCustomFilterProvider
				? Map.of( SchemaToolingSettings.HBM2DDL_FILTER_PROVIDER, CustomFilterProvider.INSTANCE )
				: Map.of();
	}

	@Entity( name = "Entity1" )
	@jakarta.persistence.Table( name = "entity_1" )
	public static class Entity1 {
		@Id
		private Integer id;
	}

	@Entity( name = "Entity2" )
	@jakarta.persistence.Table( name = "entity_2" )
	public static class Entity2 {
		@Id
		private Integer id;

		@Column( length = 255 )
		private String name;
	}

	public static class CustomFilterProvider implements SchemaFilterProvider {
		public static final CustomFilterProvider INSTANCE = new CustomFilterProvider();

		@Override
		public SchemaFilter getCreateFilter() {
			return CustomSchemaFilter.INSTANCE;
		}

		@Override
		public SchemaFilter getDropFilter() {
			return CustomSchemaFilter.INSTANCE;
		}

		@Override
		public SchemaFilter getTruncatorFilter() {
			return CustomSchemaFilter.INSTANCE;
		}

		@Override
		public SchemaFilter getMigrateFilter() {
			return CustomSchemaFilter.INSTANCE;
		}

		@Override
		public SchemaFilter getValidateFilter() {
			return CustomSchemaFilter.INSTANCE;
		}
	}

	public static class CustomSchemaFilter implements SchemaFilter {
		public static CustomSchemaFilter INSTANCE = new CustomSchemaFilter();

		@Override
		public boolean includeNamespace(Namespace namespace) {
			return true;
		}

		@Override
		public boolean includeTable(Table table) {
			return table.getName().equals( "entity_1" );
		}

		@Override
		public boolean includeSequence(Sequence sequence) {
			return true;
		}
	}
}
