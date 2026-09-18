/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Period;
import java.util.Properties;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.AnnotationBasedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserTypeCreationContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the custom-type context through live resolution and archive restoration.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = UserTypeCreationContextTest.Event.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.METADATA_SERIALIZATION_ENABLED, value = "true"))
class UserTypeCreationContextTest {

	@Test
	void contextSurvivesTheResolutionAndRestorationLifecycle(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		final var original = configuredType( metadata );
		assertTrue( original.initialized );
		assertTrue( original.parametersInjected );
		assertTrue( original.days );

		final var bytes = new ByteArrayOutputStream();
		MetadataSerialization.serialize( metadata ).writeTo( bytes );
		final var restored = MetadataSerialization.read( new ByteArrayInputStream( bytes.toByteArray() ) )
				.restore( metadata.getMappingResolutionOptions().getServiceRegistry() );
		final var recreated = configuredType( restored.getMetadata() );
		assertNotSame( original, recreated );
		assertTrue( recreated.initialized );
		assertTrue( recreated.parametersInjected );
		assertTrue( recreated.days );
	}

	private static ConfiguredPeriodType configuredType(Metadata metadata) {
		final var property = metadata.getEntityBinding( Event.class.getName() ).getProperty( "duration" );
		return (ConfiguredPeriodType) ( (CustomType<?>) property.getType() ).getUserType();
	}

	@Entity(name = "ContextEvent")
	static class Event {
		@Id
		long id;

		@ConfiguredPeriod(days = true)
		Period duration;
	}

	@Type(value = ConfiguredPeriodType.class, parameters = @Parameter(name = "unit", value = "days"))
	@Target(FIELD)
	@Retention(RUNTIME)
	public @interface ConfiguredPeriod {
		boolean days();
	}

	public static class ConfiguredPeriodType extends MetaUserTypeTest.AbstractPeriodType
			implements AnnotationBasedUserType<ConfiguredPeriod, Period>, ParameterizedType {
		private final UserTypeCreationContext constructorContext;
		private boolean initialized;
		private boolean parametersInjected;

		public ConfiguredPeriodType(ConfiguredPeriod annotation, UserTypeCreationContext context) {
			super( annotation.days() );
			constructorContext = context;
			assertEquals( "duration", context.getMemberDetails().getName() );
			assertEquals( "days", context.getParameters().getProperty( "unit" ) );
		}

		public ConfiguredPeriodType(ConfiguredPeriod annotation) {
			super( false );
			throw new AssertionError( "The annotation-and-context constructor must take precedence" );
		}

		public ConfiguredPeriodType(UserTypeCreationContext context) {
			super( false );
			throw new AssertionError( "The annotation-and-context constructor must take precedence" );
		}

		public ConfiguredPeriodType() {
			super( false );
			throw new AssertionError( "The annotation-and-context constructor must take precedence" );
		}

		@Override
		public void initialize(ConfiguredPeriod annotation, UserTypeCreationContext context) {
			assertSame( constructorContext, context );
			assertEquals( annotation.days(), days );
			assertEquals( "duration", context.getMemberDetails().getName() );
			assertEquals( "days", context.getParameters().getProperty( "unit" ) );
			initialized = true;
		}

		@Override
		public void setParameterValues(Properties parameters) {
			assertTrue( initialized );
			assertSame( constructorContext.getParameters(), parameters );
			assertEquals( "days", parameters.getProperty( "unit" ) );
			parametersInjected = true;
		}
	}
}
