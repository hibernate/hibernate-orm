package org.hibernate.orm.test.jpa.beanvalidation;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import jakarta.validation.Validation;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.tool.schema.ValidationConstraintDdlInfluence;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

/// Standalone JPA schema generation must apply Bean Validation constraints to DDL.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-9246")
public class StandaloneSchemaGenerationValidationTest {
	@Test
	void testStandaloneSchemaGeneration() {
		StringWriter script = new StringWriter();
		var builder = builder( script );
		BootstrapPipeline.generateSchema( builder );
		assertColumnLengths( script.toString() );
	}

	@ParameterizedTest
	@EnumSource(ValidationConstraintDdlInfluence.class)
	void testDdlInfluenceWithRuntimeValidationDisabled(ValidationConstraintDdlInfluence influence) {
		StringWriter script = new StringWriter();
		var settings = settings( script );
		settings.put( AvailableSettings.JAKARTA_VALIDATION_MODE, "none" );
		settings.put( AvailableSettings.APPLY_VALIDATION_CONSTRAINTS, influence );
		var builder = builder( settings );
		BootstrapPipeline.generateSchema( builder );
		boolean disabled = influence == ValidationConstraintDdlInfluence.DISABLED;
		assertEquals( disabled ? 255 : 32, columnLength( script.toString(), "validated_name" ) );
		assertEquals( disabled ? 255 : 48, columnLength( script.toString(), "sized_name" ) );
	}

	@Test
	void testProgrammaticFactoryOverridesConfiguredFactory() {
		try ( var configured = Validation.buildDefaultValidatorFactory();
				var programmatic = Validation.buildDefaultValidatorFactory() ) {
			var configuredSpy = spy( configured );
			var programmaticSpy = spy( programmatic );
			StringWriter script = new StringWriter();
			var settings = settings( script );
			settings.put( AvailableSettings.JAKARTA_VALIDATION_FACTORY, configuredSpy );
			var builder = builder( settings );
			builder.property( AvailableSettings.JAKARTA_VALIDATION_FACTORY, programmaticSpy );
			BootstrapPipeline.generateSchema( builder );
			assertColumnLengths( script.toString() );
			verify( programmaticSpy, atLeastOnce() ).getValidator();
			verify( configuredSpy, never() ).getValidator();
			verify( programmaticSpy, never() ).close();
			verify( configuredSpy, never() ).close();
		}
	}

	@Test
	void testSchemaGenerationWithEntityManagerFactory() {
		StringWriter script = new StringWriter();
		var builder = builder( script );
		try ( var factory = BootstrapPipeline.build( builder ) ) {
			assertColumnLengths( script.toString() );
		}
	}

	private PersistenceConfiguration builder(StringWriter script) {
		return builder( settings( script ) );
	}

	private Map<String, Object> settings(StringWriter script) {
		Map<String, Object> settings = new HashMap<>();
		settings.put( AvailableSettings.DIALECT, H2Dialect.class.getName() );
		settings.put( AvailableSettings.ALLOW_METADATA_ON_BOOT, false );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, script );
		settings.put( AvailableSettings.JAKARTA_VALIDATION_MODE, "auto" );
		settings.put( AvailableSettings.APPLY_VALIDATION_CONSTRAINTS, "REQUIRED" );
		return settings;
	}

	private PersistenceConfiguration builder(Map<String, Object> settings) {
		return new PersistenceConfiguration( "standalone-validation-schema" )
				.managedClass( ValidatedEntity.class ).properties( settings );
	}

	private void assertColumnLengths(String script) {
		assertTrue( script.contains( "standalone_validation_entity" ), script );
		assertEquals( 255, columnLength( script, "unconstrained_name" ), script );
		assertEquals( 32, columnLength( script, "validated_name" ), script );
		assertEquals( 48, columnLength( script, "sized_name" ), script );
	}

	private int columnLength(String script, String column) {
		var matcher = Pattern.compile( "\\b" + column + "\\s+varchar\\((\\d+)\\)", Pattern.CASE_INSENSITIVE )
				.matcher( script );
		assertTrue( matcher.find(), script );
		return Integer.parseInt( matcher.group( 1 ) );
	}

	@Entity
	@Table(name = "standalone_validation_entity")
	public static class ValidatedEntity {
		@Id
		private Long id;

		@Length(max = 32)
		@Column(name = "validated_name")
		private String validatedName;

		@Size(max = 48)
		@Column(name = "sized_name")
		private String sizedName;

		@Column(name = "unconstrained_name")
		private String unconstrainedName;
	}
}
