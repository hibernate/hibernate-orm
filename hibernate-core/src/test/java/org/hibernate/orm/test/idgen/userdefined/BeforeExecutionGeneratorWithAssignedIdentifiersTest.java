/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.UUID;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = BeforeExecutionGeneratorWithAssignedIdentifiersTest.Book.class)
class BeforeExecutionGeneratorWithAssignedIdentifiersTest {

	@Test
	void testAssignedValueNotOverridden(SessionFactoryScope scope) {
		final String assignedId = "assigned-id";
		final Book book = new Book();
		book.id = assignedId;
		scope.inTransaction( session -> session.persist( book ) );
		assertThat( book.id ).isEqualTo( assignedId );
	}

	@Entity
	@Table(name = "books")
	static class Book {
		@Id @GeneratedValue
		@AssignableIdGenerator
		String id;
	}

	@IdGeneratorType(IdGenerator.class)
	@ValueGenerationType(generatedBy = IdGenerator.class)
	@Retention(RUNTIME)
	@Target({FIELD, METHOD})
	public @interface AssignableIdGenerator {}

	public static class IdGenerator implements BeforeExecutionGenerator {

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			if ( currentValue != null ) {
				return currentValue;
			}
			return UUID.randomUUID().toString();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EnumSet.of( EventType.INSERT );
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}
