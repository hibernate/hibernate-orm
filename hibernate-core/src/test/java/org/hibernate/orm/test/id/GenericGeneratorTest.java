/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.Configurable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.EnumSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = GenericGeneratorTest.TestEntity.class
)
@SessionFactory
public class GenericGeneratorTest {

	static class ExplicitValueGenerator implements BeforeExecutionGenerator, Configurable {
		Object[] values;
		int counter = 0;
		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner, Object currentValue, EventType eventType) {
			return values[counter++];
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public void configure(GeneratorCreationContext creationContext, Properties parameters) {
			values = new Object[] { parameters.get( "1" ), parameters.get( "2" ) };
		}
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity hello = new TestEntity( "test" );
					session.persist( hello );
					assertEquals( "hello", hello.id );
					TestEntity world = new TestEntity( "test" );
					session.persist( world );
					assertEquals( "world", world.id );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GenericGenerator(type = ExplicitValueGenerator.class,
				parameters = {@Parameter(name = "1", value = "hello"),
							@Parameter(name = "2", value = "world")})
		private String id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(String name) {
			this.name = name;
		}
	}
}
