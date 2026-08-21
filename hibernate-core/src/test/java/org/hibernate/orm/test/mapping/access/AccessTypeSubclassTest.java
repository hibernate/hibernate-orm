/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Jpa(annotatedClasses = {
		AccessTypeSubclassTest.Base.class,
		AccessTypeSubclassTest.SomeEntity.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-20418")
class AccessTypeSubclassTest {
	@Test void test(EntityManagerFactoryScope scope) {
		EntityType<SomeEntity> someEntityType =
				scope.getEntityManagerFactory().getMetamodel()
						.entity( SomeEntity.class );
		assertEquals( 3, someEntityType.getAttributes().size() );
		assertEquals( 1, someEntityType.getDeclaredAttributes().size() );
		assertInstanceOf( Field.class,
				someEntityType.getId( String.class ).getJavaMember() );
		assertInstanceOf( Field.class,
				someEntityType.getSingularAttribute( "created", LocalDateTime.class )
						.getJavaMember() );
		assertInstanceOf( Method.class,
				someEntityType.getDeclaredSingularAttribute( "name", String.class )
						.getJavaMember() );
	}

	@MappedSuperclass
	static abstract class Base {
		@Id
		String id;
		LocalDateTime created;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "CREATEDxx")
		public LocalDateTime getCreated() {
			return created;
		}

		public void setCreated(LocalDateTime created) {
			this.created = created;
		}
	}

	@Entity
	@AttributeOverrides({ @AttributeOverride(name = "id", column = @Column(name = "ID")),
			@AttributeOverride(name = "created", column = @Column(name = "CREATED"))})
	@Access(AccessType.PROPERTY)
	static class SomeEntity extends Base {
		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
