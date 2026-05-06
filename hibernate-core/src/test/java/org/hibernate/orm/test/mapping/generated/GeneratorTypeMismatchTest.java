/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;

import org.hibernate.MappingException;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BaseUnitTest
@JiraKey("HHH-18210")
public class GeneratorTypeMismatchTest {
	@Test
	public void rejectsMismatchedGeneratedPropertyType() {
		final var exception = assertThrows(
				MappingException.class,
				() -> buildSessionFactory( EntityWithMismatchedGeneratedProperty.class )
		);

		assertThat( exception.getMessage() )
				.contains( StringGenerator.class.getName() )
				.contains( "java.lang.String" )
				.contains( "not assignable" )
				.contains( "generatedNumber" )
				.contains( "java.lang.Integer" );
	}

	@Test
	public void rejectsMismatchedGeneratedIdentifierType() {
		final var exception = assertThrows(
				MappingException.class,
				() -> buildSessionFactory( EntityWithMismatchedGeneratedId.class )
		);

		assertThat( exception.getMessage() )
				.contains( StringGenerator.class.getName() )
				.contains( "java.lang.String" )
				.contains( "not assignable" )
				.contains( "id" )
				.contains( "java.lang.Long" );
	}

	@Test
	public void acceptsMatchingGeneratedType() {
		buildSessionFactory( EntityWithMatchingGeneratedProperty.class );
	}

	private static void buildSessionFactory(Class<?> annotatedClass) {
		try ( var serviceRegistry = ServiceRegistryUtil.serviceRegistry() ) {
			final var sessionFactory = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( annotatedClass )
					.buildMetadata()
					.buildSessionFactory();
			sessionFactory.close();
		}
	}

	@ValueGenerationType(generatedBy = StringGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	public @interface GeneratedString {
	}

	@IdGeneratorType(StringGenerator.class)
	@Retention(RUNTIME)
	@Target({ FIELD, METHOD })
	public @interface GeneratedStringId {
	}

	public static class StringGenerator implements BeforeExecutionGenerator {
		@Override
		public String generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return "generated";
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public Class<?> getGeneratedType() {
			return String.class;
		}
	}

	@Entity(name = "EntityWithMismatchedGeneratedProperty")
	public static class EntityWithMismatchedGeneratedProperty {
		@Id
		private Long id;

		@GeneratedString
		private Integer generatedNumber;
	}

	@Entity(name = "EntityWithMismatchedGeneratedId")
	public static class EntityWithMismatchedGeneratedId {
		@Id
		@GeneratedStringId
		private Long id;
	}

	@Entity(name = "EntityWithMatchingGeneratedProperty")
	public static class EntityWithMatchingGeneratedProperty {
		@Id
		private Long id;

		@GeneratedString
		private String generatedText;
	}
}
