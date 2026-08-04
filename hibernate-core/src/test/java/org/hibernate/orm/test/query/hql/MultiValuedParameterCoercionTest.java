/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JavaType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.StringJavaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = MultiValuedParameterCoercionTest.TestEntity.class )
@SessionFactory
@JiraKey( "HHH-20721" )
class MultiValuedParameterCoercionTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void collectionIsNotCoercedToElementType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, "first" ) );
			session.persist( new TestEntity( 2L, "second" ) );
			session.flush();

			TrackingStringJavaType.COLLECTION_COERCIONS.set( 0 );

			final var result = session.createQuery(
						"from TestEntity where name in :names",
						TestEntity.class
				)
					.setParameter( "names", Set.of( "first", "second" ) )
					.getResultList();

			assertThat( result )
					.extracting( TestEntity::getName )
					.containsExactlyInAnyOrder( "first", "second" );
			assertThat( TrackingStringJavaType.COLLECTION_COERCIONS ).hasValue( 0 );
		} );
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@JavaType( TrackingStringJavaType.class )
		private String name;

		TestEntity() {
		}

		TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		String getName() {
			return name;
		}
	}

	public static class TrackingStringJavaType extends StringJavaType {
		private static final AtomicInteger COLLECTION_COERCIONS = new AtomicInteger();

		@Override
		public @Nullable String coerce(@Nonnull Object value) {
			if ( value instanceof Collection<?> ) {
				COLLECTION_COERCIONS.incrementAndGet();
			}
			return super.coerce( value );
		}
	}
}
