package org.hibernate.orm.test.jpa.beanvalidation;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
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
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
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
		try {
			builder.generateSchema();
			assertColumnLengths( script.toString() );
		}
		finally {
			builder.cancel();
		}
	}

	@ParameterizedTest
	@EnumSource(ValidationConstraintDdlInfluence.class)
	void testDdlInfluenceWithRuntimeValidationDisabled(ValidationConstraintDdlInfluence influence) {
		StringWriter script = new StringWriter();
		var settings = settings( script );
		settings.put( AvailableSettings.JAKARTA_VALIDATION_MODE, "none" );
		settings.put( AvailableSettings.APPLY_VALIDATION_CONSTRAINTS, influence );
		var builder = builder( settings );
		try {
			builder.generateSchema();
			boolean disabled = influence == ValidationConstraintDdlInfluence.DISABLED;
			assertEquals( disabled ? 255 : 32, columnLength( script.toString(), "validated_name" ) );
			assertEquals( disabled ? 255 : 48, columnLength( script.toString(), "sized_name" ) );
		}
		finally {
			builder.cancel();
		}
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
			builder.withValidatorFactory( programmaticSpy );
			try {
				builder.generateSchema();
				assertColumnLengths( script.toString() );
				verify( programmaticSpy, atLeastOnce() ).getValidator();
				verify( configuredSpy, never() ).getValidator();
				verify( programmaticSpy, never() ).close();
				verify( configuredSpy, never() ).close();
			}
			finally {
				builder.cancel();
			}
		}
	}

	@Test
	void testSchemaGenerationWithEntityManagerFactory() {
		StringWriter script = new StringWriter();
		var builder = builder( script );
		try {
			try ( var factory = builder.build() ) {
				assertColumnLengths( script.toString() );
			}
		}
		finally {
			builder.cancel();
		}
	}

	private EntityManagerFactoryBuilderImpl builder(StringWriter script) {
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

	private EntityManagerFactoryBuilderImpl builder(Map<String, Object> settings) {
		return new EntityManagerFactoryBuilderImpl( new PersistenceUnitDescriptorAdapter() {
			@Override
			public String getName() {
				return "standalone-validation-schema";
			}

			@Override
			public List<String> getManagedClassNames() {
				return List.of( ValidatedEntity.class.getName() );
			}
		}, settings );
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
