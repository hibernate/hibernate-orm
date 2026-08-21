/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Jpa(annotatedClasses = AccessTypeTest.SomeEntity.class)
class AccessTypeTest {
	@Test void test(EntityManagerFactoryScope scope) {
		EntityType<SomeEntity> someEntityType =
				scope.getEntityManagerFactory().getMetamodel()
						.entity( SomeEntity.class );
		assertEquals( 4, someEntityType.getAttributes().size() );
		assertEquals( 2, someEntityType.getDeclaredAttributes().size() );
		assertInstanceOf( Method.class,
				someEntityType.getId( String.class ).getJavaMember() );
		assertInstanceOf( Method.class,
				someEntityType.getSingularAttribute( "created", LocalDateTime.class )
						.getJavaMember() );
		assertInstanceOf( Field.class,
				someEntityType.getDeclaredSingularAttribute( "name", String.class )
						.getJavaMember() );
		assertInstanceOf( Field.class,
				someEntityType.getDeclaredSingularAttribute( "count", Integer.class )
						.getJavaMember() );
	}

	@MappedSuperclass
	@Access(AccessType.PROPERTY)
	static abstract class Base {
		@Id
		@Column(name = "some_entity_id") //optional
		public abstract String getId();
		public abstract void setId(String id);

		public abstract LocalDateTime getCreated();
		public abstract void setCreated(LocalDateTime created);
	}

	@Entity
	static class SomeEntity extends Base {
		String id;
		LocalDateTime created;

		@Basic(optional = false)
		String name;

		int count;

		@Override
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public LocalDateTime getCreated() {
			return created;
		}

		@Override
		public void setCreated(LocalDateTime created) {
			this.created = created;
		}
	}
}
