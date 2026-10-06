package org.hibernate.boot.beanvalidation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.NoProviderFoundException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.ValidatorFactory;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.tool.schema.ValidationConstraintDdlInfluence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Provider availability and factory ownership during standalone schema preparation.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-9246")
class StandaloneValidationPreparationTest {

	@Test
	void closesDefaultFactory() {
		var metadata = metadata();
		var factory = mock( ValidatorFactory.class );
		try ( var validation = mockStatic( Validation.class ) ) {
			validation.when( Validation::buildDefaultValidatorFactory ).thenReturn( factory );
			prepare( metadata, registry( Map.of() ), null, ValidationConstraintDdlInfluence.AUTO );
			verify( factory ).close();
		}
	}

	@Test
	void preservesFailureWhenClosingFactoryAlsoFails() {
		var metadata = metadata();
		var failure = new IllegalStateException( "metadata failure" );
		when( metadata.getEntityBindings() ).thenThrow( failure );
		var factory = mock( ValidatorFactory.class );
		var cleanupFailure = new IllegalStateException( "cleanup failure" );
		doThrow( cleanupFailure ).when( factory ).close();
		try ( var validation = mockStatic( Validation.class ) ) {
			validation.when( Validation::buildDefaultValidatorFactory ).thenReturn( factory );
			assertSame( failure, assertThrows( IllegalStateException.class,
					() -> prepare( metadata, registry( Map.of() ), null, ValidationConstraintDdlInfluence.AUTO ) ) );
			assertEquals( 1, failure.getSuppressed().length );
			assertSame( cleanupFailure, failure.getSuppressed()[0] );
			verify( factory ).close();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "jakarta.persistence.validation.factory", "javax.persistence.validation.factory" })
	void suppliedFactoriesRemainCallerOwned(String setting) {
		var factory = mock( ValidatorFactory.class );
		try ( var validation = mockStatic( Validation.class ) ) {
			prepare( metadata(), registry( Map.of( setting, factory ) ), null, ValidationConstraintDdlInfluence.AUTO );
			verify( factory, never() ).close();
			validation.verifyNoInteractions();
		}
	}

	@Test
	void programmaticFactoryTakesPrecedenceAndRemainsCallerOwnedOnFailure() {
		var configured = mock( ValidatorFactory.class );
		var programmatic = mock( ValidatorFactory.class );
		var metadata = metadata();
		var failure = new IllegalStateException( "metadata failure" );
		when( metadata.getEntityBindings() ).thenThrow( failure );
		try ( var validation = mockStatic( Validation.class ) ) {
			assertSame( failure, assertThrows( IllegalStateException.class,
					() -> prepare( metadata, registry( Map.of( AvailableSettings.JAKARTA_VALIDATION_FACTORY, configured ) ),
							programmatic, ValidationConstraintDdlInfluence.AUTO ) ) );
			verifyNoInteractions( configured, programmatic );
			validation.verifyNoInteractions();
		}
	}

	@ParameterizedTest
	@EnumSource(value = ValidationConstraintDdlInfluence.class, names = { "AUTO", "REQUIRED" })
	void missingProvider(ValidationConstraintDdlInfluence influence) {
		try ( var validation = mockStatic( Validation.class ) ) {
			validation.when( Validation::buildDefaultValidatorFactory ).thenThrow( new NoProviderFoundException() );
			var metadata = metadata();
			if ( influence == ValidationConstraintDdlInfluence.REQUIRED ) {
				var failure = assertThrows( IntegrationException.class,
						() -> prepare( metadata, registry( Map.of() ), null, influence ) );
				assertTrue( failure.getMessage().contains( AvailableSettings.APPLY_VALIDATION_CONSTRAINTS ) );
			}
			else {
				prepare( metadata, registry( Map.of() ), null, influence );
			}
			verify( metadata, never() ).getEntityBindings();
		}
	}

	@Test
	void autoPropagatesProviderInitializationFailure() {
		var failure = new ValidationException( "provider initialization failed" );
		try ( var validation = mockStatic( Validation.class ) ) {
			validation.when( Validation::buildDefaultValidatorFactory ).thenThrow( failure );
			var exception = assertThrows( IntegrationException.class,
					() -> prepare( metadata(), registry( Map.of() ), null, ValidationConstraintDdlInfluence.AUTO ) );
			assertSame( failure, exception.getCause() );
		}
	}

	@ParameterizedTest
	@EnumSource(ValidationConstraintDdlInfluence.class)
	void missingApiAndDisabledPreparation(ValidationConstraintDdlInfluence influence) {
		var settings = new HashMap<String, Object>();
		settings.put( AvailableSettings.APPLY_VALIDATION_CONSTRAINTS, influence );
		settings.put( AvailableSettings.JAKARTA_VALIDATION_MODE, "callback" );
		var registry = registry( settings );
		var loader = registry.requireService( ClassLoaderService.class );
		when( loader.classForName( BeanValidationIntegrator.JAKARTA_BV_CHECK_CLASS ) )
				.thenThrow( new ClassLoadingException( "validation API unavailable" ) );
		var metadata = metadata();
		try ( var validation = mockStatic( Validation.class ) ) {
			if ( influence == ValidationConstraintDdlInfluence.REQUIRED ) {
				assertThrows( IntegrationException.class,
						() -> BeanValidationIntegrator.applyRelationalConstraints( metadata, registry, null ) );
			}
			else {
				BeanValidationIntegrator.applyRelationalConstraints( metadata, registry, null );
			}
			if ( influence == ValidationConstraintDdlInfluence.DISABLED ) {
				verifyNoInteractions( loader );
			}
			verify( metadata, never() ).getEntityBindings();
			validation.verifyNoInteractions();
		}
	}

	private static void prepare(Metadata metadata, ServiceRegistry registry, Object factory,
			ValidationConstraintDdlInfluence influence) {
		TypeSafeActivator.applyRelationalConstraints( metadata, registry, factory, influence );
	}

	private static Metadata metadata() {
		var metadata = mock( Metadata.class );
		when( metadata.getEntityBindings() ).thenReturn( List.of() );
		return metadata;
	}

	private static ServiceRegistry registry(Map<String, Object> settings) {
		var registry = mock( ServiceRegistry.class );
		var configuration = new ConfigurationServiceImpl( settings );
		when( registry.requireService( ConfigurationService.class ) ).thenReturn( configuration );
		var jdbc = mock( JdbcServices.class );
		when( jdbc.getDialect() ).thenReturn( new H2Dialect() );
		when( registry.requireService( JdbcServices.class ) ).thenReturn( jdbc );
		when( registry.requireService( ClassLoaderService.class ) ).thenReturn( mock( ClassLoaderService.class ) );
		return registry;
	}
}
