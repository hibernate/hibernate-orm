/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stefan Würsten
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		GenericAnnotationBasedGeneratorTest.SimpleEntity.class,
		GenericAnnotationBasedGeneratorTest.NestedSimpleEntity.class
})
@Jira("HHH-20614")
public class GenericAnnotationBasedGeneratorTest {

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void test(SessionFactoryScope scope) {
		SimpleEntity entity = new SimpleEntity();
		scope.inTransaction( s -> s.persist( entity ) );
		assertThat( entity.id ).startsWith( "simple-" );
	}

	@Test
	void testNested(SessionFactoryScope scope) {
		NestedSimpleEntity entity = new NestedSimpleEntity();
		scope.inTransaction( s -> s.persist( entity ) );
		assertThat( entity.id ).startsWith( "nested-" );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@PrefixId
		String id;
		String name;
	}

	@Entity(name = "NestedSimpleEntity")
	public static class NestedSimpleEntity {
		@Id
		@PrefixIdNested
		String id;
		String name;
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@IdGeneratorType(PrefixGenerator.class)
	public @interface PrefixId {
		String prefix() default "simple-";
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@IdGeneratorType(NestedPrefixGenerator.class)
	public @interface PrefixIdNested {
		String prefix() default "nested-";
	}

	public static class PrefixGenerator implements BeforeExecutionGenerator, AnnotationBasedGenerator<PrefixId> {

		private String idPrefix;

		@Override
		public void initialize(PrefixId annotation, GeneratorCreationContext context) {
			this.idPrefix = annotation.prefix();
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return idPrefix + UUID.randomUUID().toString().substring( 0, 8 );
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	public static class NestedPrefixGenerator extends AbstractGenerator<PrefixIdNested> {

		private String idPrefix;

		@Override
		public void initialize(PrefixIdNested annotation, GeneratorCreationContext context) {
			this.idPrefix = annotation.prefix();
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return idPrefix + UUID.randomUUID().toString().substring( 0, 8 );
		}

	}

	public static abstract class AbstractGenerator<T extends Annotation>
			implements BeforeExecutionGenerator, AnnotationBasedGenerator<T> {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}
}
