/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.internal.ConstraintValidationType;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.cfg.SchemaToolingSettings.INDEX_VALIDATION;
import static org.hibernate.cfg.SchemaToolingSettings.UNIQUE_KEY_VALIDATION;

/**
 * @author Steve Ebersole
 */
@ParameterizedClass
@MethodSource("validationTypes")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ServiceRegistryFunctionalTesting
public class ValidateConstraintTests implements ServiceRegistryProducer {
	public static List<ConstraintValidationType> validationTypes() {
		return List.of( ConstraintValidationType.NONE, ConstraintValidationType.ALL );
	}

	private final ConstraintValidationType validationType;
	private Metadata schemaToDrop;

	public ValidateConstraintTests(ConstraintValidationType validationType) {
		this.validationType = validationType;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( INDEX_VALIDATION, validationType )
				.applySetting( UNIQUE_KEY_VALIDATION, validationType )
				.build();
	}

	@AfterEach
	void tearDown(ServiceRegistryScope registryScope) {
		if ( schemaToDrop == null ) {
			return;
		}
		final ServiceRegistryImplementor registry = (ServiceRegistryImplementor) registryScope.getRegistry();

		final HibernateSchemaManagementTool schemaTooling = new HibernateSchemaManagementTool();
		schemaTooling.injectServices( registry );

		dropSchema( schemaToDrop, schemaTooling, registry );
	}

	@Test
	void testValidationOfConstraints(ServiceRegistryScope registryScope) {
		final ServiceRegistryImplementor registry = (ServiceRegistryImplementor) registryScope.getRegistry();

		final Metadata schema1 = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Gender.class, Person1.class )
				.buildMetadata();
		final Metadata schema2 = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Gender.class, Person2.class )
				.buildMetadata();
		final Metadata schema3 = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Gender.class, Person3.class )
				.buildMetadata();
		final Metadata schema4 = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Gender.class, Person4.class )
				.buildMetadata();
		final Metadata schema5 = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Gender.class, Person5.class )
				.buildMetadata();

		final HibernateSchemaManagementTool schemaTooling = new HibernateSchemaManagementTool();
		schemaTooling.injectServices( registry );

		// create the initial schema
		System.out.println( "> Creation" );
		createSchema( schema1, schemaTooling, registry );
		schemaToDrop = schema1;

		// validate the first schema change
		System.out.println( "> Migration #1" );
		try {
			validateSchema( schema2, schemaTooling, registry );
			if ( validationType == ConstraintValidationType.ALL ) {
				fail( "Expecting an exception" );
			}
		}
		catch (SchemaManagementException expected) {
			if ( validationType == ConstraintValidationType.NONE ) {
				fail( "Not expecting an exception" );
			}
		}
		updateSchema( schema2, schemaTooling, registry );
		schemaToDrop = schema2;

		// validate the second schema change
		System.out.println( "> Migration #2" );
		try {
			validateSchema( schema3, schemaTooling, registry );
			if ( validationType == ConstraintValidationType.ALL ) {
				fail( "Expecting an exception" );
			}
		}
		catch (SchemaManagementException expected) {
			if ( validationType == ConstraintValidationType.NONE ) {
				fail( "Not expecting an exception" );
			}
		}
		updateSchema( schema3, schemaTooling, registry );
		schemaToDrop = schema3;

		// validate the 3rd schema change
		System.out.println( "> Migration #3" );
		try {
			validateSchema( schema4, schemaTooling, registry );
			if ( validationType == ConstraintValidationType.ALL ) {
				fail( "Expecting an exception" );
			}
		}
		catch (SchemaManagementException expected) {
			if ( validationType == ConstraintValidationType.NONE ) {
				fail( "Not expecting an exception" );
			}
		}
		updateSchema( schema4, schemaTooling, registry );
		schemaToDrop = schema4;

		// validate the 4th schema change
		System.out.println( "> Migration #4" );
		try {
			validateSchema( schema5, schemaTooling, registry );
			if ( validationType == ConstraintValidationType.ALL ) {
				fail( "Expecting an exception" );
			}
		}
		catch (SchemaManagementException expected) {
			if ( validationType == ConstraintValidationType.NONE ) {
				fail( "Not expecting an exception" );
			}
		}
		updateSchema( schema5, schemaTooling, registry );
		schemaToDrop = schema5;

	}

	private void validateSchema(
			Metadata schema,
			HibernateSchemaManagementTool schemaTooling,
			ServiceRegistryImplementor registry) {
		SchemaManagementToolCoordinator.performDatabaseAction(
				Action.VALIDATE,
				schema,
				schemaTooling,
				registry,
				new Options( registry ),
				ContributableMatcher.ALL
		);
	}

	private void updateSchema(
			Metadata schema,
			HibernateSchemaManagementTool schemaTooling,
			ServiceRegistryImplementor registry) {
		SchemaManagementToolCoordinator.performDatabaseAction(
				Action.UPDATE,
				schema,
				schemaTooling,
				registry,
				new Options( registry ),
				ContributableMatcher.ALL
		);
	}

	private static void createSchema(
			Metadata schema,
			HibernateSchemaManagementTool schemaTooling,
			ServiceRegistryImplementor registry) {
		SchemaManagementToolCoordinator.performDatabaseAction(
				Action.CREATE_ONLY,
				schema,
				schemaTooling,
				registry,
				new Options( registry ),
				ContributableMatcher.ALL
		);
	}

	private static void dropSchema(
			Metadata metadata,
			HibernateSchemaManagementTool schemaTooling,
			ServiceRegistryImplementor registry) {
		SchemaManagementToolCoordinator.performDatabaseAction(
				Action.DROP,
				metadata,
				schemaTooling,
				registry,
				new Options( registry ),
				ContributableMatcher.ALL
		);
	}

	enum Gender { MALE, FEMALE }

	@Entity(name="Person1")
	@Table(name="persons")
	public static class Person1 {
		@Id
		private Integer id;
		private String ssn;
		private String name;
		private Gender gender;
		private Instant dob;
	}

	@Entity(name="Person2")
	@Table(name="persons",
			indexes = @Index(columnList = "name"))
	public static class Person2 {
		@Id
		private Integer id;
		private String ssn;
		private String name;
		private Gender gender;
		private Instant dob;
	}

	@Entity(name="Person3")
	@Table(name="persons",
			indexes = @Index(columnList = "name"))
	public static class Person3 {
		@Id
		private Integer id;
		@NaturalId
		private String ssn;
		private String name;
		private Gender gender;
		private Instant dob;
	}

	@Entity(name="Person4")
	@Table(name="persons",
			indexes = {
				@Index(columnList = "name"),
				@Index(columnList = "gender")
			}
	)
	public static class Person4 {
		@Id
		private Integer id;
		@NaturalId
		private String ssn;
		private String name;
		private Gender gender;
		private Instant dob;
	}

	@Entity(name="Person5")
	@Table(name="persons",
			indexes = {
				@Index(name = "person_name", columnList = "name"),
				@Index(name = "person_gender", columnList = "gender")
			}
	)
	public static class Person5 {
		@Id
		private Integer id;
		@NaturalId
		private String ssn;
		private String name;
		private Gender gender;
		private Instant dob;
	}

	private static class Options implements ExecutionOptions, ExceptionHandler {
		private final Map<String, Object> settings;

		public Options(Map<String, Object> settings) {
			this.settings = settings;
		}

		public Options(ServiceRegistryImplementor registry) {
			this( registry.requireService( ConfigurationService.class ).getSettings() );
		}

		@Override
		public void handleException(CommandAcceptanceException exception) {
			throw exception;
		}

		@Override
		public Map<String, Object> getConfigurationValues() {
			return settings;
		}

		@Override
		public boolean shouldManageNamespaces() {
			return false;
		}

		@Override
		public ExceptionHandler getExceptionHandler() {
			return this;
		}
	}
}
