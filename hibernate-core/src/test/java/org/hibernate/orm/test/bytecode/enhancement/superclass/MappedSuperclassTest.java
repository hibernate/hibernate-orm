/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.superclass;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@JiraKey("HHH-17418")
@DomainModel(
		annotatedClasses = {
				MappedSuperclassTest.MyEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MappedSuperclassTest {
	private static final LocalDateTime TEST_DATE_UPDATED_VALUE = LocalDateTime.of( 2023, 11, 10, 0, 0 );
	private static final long TEST_ID = 1L;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			MyEntity testEntity = new MyEntity();
			testEntity.id = TEST_ID;
			s.persist( testEntity );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			MyEntity testEntity = s.get( MyEntity.class, TEST_ID );
			assertThat( testEntity.value() ).isEqualTo( TEST_DATE_UPDATED_VALUE );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}


	@MappedSuperclass
	public static class MappedBase {
		// field is private on purpose so that enhancer will not use field access
		@Column
		private final LocalDateTime updated = TEST_DATE_UPDATED_VALUE;

		public LocalDateTime value() {
			return updated;
		}
	}

	@Entity(name = "MyEntity")
	public static class MyEntity extends MappedBase {
		@Id
		Long id;
	}
}
