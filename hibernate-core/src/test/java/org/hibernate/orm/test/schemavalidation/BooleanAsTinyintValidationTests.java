/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.schemavalidation;

import java.util.EnumSet;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = @Setting( name = MappingSettings.PREFERRED_BOOLEAN_JDBC_TYPE, value = "TINYINT"))
@DomainModel( annotatedClasses = BooleanAsTinyintValidationTests.Client.class )
public class BooleanAsTinyintValidationTests {

	@Test
	void testValidation(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		final ExecutionOptions executionOptions = new ExecutionOptions() {
			@Override
			public Map<String, Object> getConfigurationValues() {
				return registryScope.getRegistry().requireService( ConfigurationService.class ).getSettings();
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};
		registryScope.getRegistry()
				.requireService( SchemaManagementTool.class )
				.getSchemaValidator( null )
				.doValidation( domainModelScope.getDomainModel(), executionOptions, ContributableMatcher.ALL );
	}

	@Test
	void testRuntimeUsage(DomainModelScope domainModelScope) {
		try (SessionFactory sessionFactory = domainModelScope.getDomainModel().buildSessionFactory()) {
			sessionFactory.inTransaction( (session) -> {
				session.persist( new Client( 1, "stuff", true ) );
			} );
			sessionFactory.inTransaction( (session) -> {
				session.find( Client.class, 1 );
			} );
			sessionFactory.inTransaction( (session) -> {
				session.createQuery( "from Client", Client.class ).list();
			} );
			sessionFactory.inTransaction( (session) -> {
				session.createNativeQuery( "select * from Client", Client.class ).list();
			} );
		}
	}

	@BeforeEach
	public void setUp(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		final MetadataImplementor domainModel = domainModelScope.getDomainModel();
		domainModel.orderColumns( false );
		domainModel.validate();


		dropSchema( domainModel );
		// create the schema
		createSchema( registryScope, domainModel );
	}

	@AfterEach
	public void tearDown(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		dropSchema( domainModelScope.getDomainModel() );
	}

	private void createSchema(ServiceRegistryScope registryScope, MetadataImplementor domainModel) {
		final ExecutionOptions executionOptions = new ExecutionOptions() {
			@Override
			public Map<String, Object> getConfigurationValues() {
				return registryScope.getRegistry().requireService( ConfigurationService.class ).getSettings();
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};
		registryScope.getRegistry()
				.requireService( SchemaManagementTool.class )
				.getSchemaCreator( null )
				.doCreation(
						domainModel,
						executionOptions,
						ContributableMatcher.ALL,
						new SourceDescriptor() {
							@Override
							public SourceType getSourceType() {
								return SourceType.METADATA;
							}

							@Override
							public ScriptSourceInput getScriptSourceInput() {
								return null;
							}
						},
						new TargetDescriptor() {
							@Override
							public EnumSet<TargetType> getTargetTypes() {
								return EnumSet.of( TargetType.DATABASE );
							}

							@Override
							public ScriptTargetOutput getScriptTargetOutput() {
								return null;
							}
						}
				);
	}

	private void dropSchema(MetadataImplementor domainModel) {
		new SchemaExport()
				.drop( EnumSet.of( TargetType.DATABASE ), domainModel );
	}


	@Entity(name="Client")
	@Table(name="Client")
	public static class Client {
		@Id
		private Integer id;
		private String name;
		private boolean active;

		public Client() {
		}

		public Client(Integer id, String name, boolean active) {
			this.id = id;
			this.name = name;
			this.active = active;
		}
	}

}
