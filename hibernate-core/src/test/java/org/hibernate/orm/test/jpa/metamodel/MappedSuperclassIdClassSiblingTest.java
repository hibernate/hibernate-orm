/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa( annotatedClasses = {
		MappedSuperclassIdClassSiblingTest.SingleIdEntity.class,
		MappedSuperclassIdClassSiblingTest.CompositeIdEntity.class
} )
@JiraKey( "HHH-20606" )
public class MappedSuperclassIdClassSiblingTest {
	@Test
	void singleIdEntityKeepsInheritedIdWhenIdClassSiblingIsPresent(EntityManagerFactoryScope scope) {
		final EntityType<SingleIdEntity> entityType = scope.getEntityManagerFactory()
				.getMetamodel()
				.entity( SingleIdEntity.class );

		assertTrue( entityType.hasSingleIdAttribute() );
		assertEquals( Long.class, entityType.getIdType().getJavaType() );
		assertTrue( entityType.getSingularAttribute( "id" ).isId() );

		final EntityType<CompositeIdEntity> compositeEntityType = scope.getEntityManagerFactory()
				.getMetamodel()
				.entity( CompositeIdEntity.class );
		assertFalse( compositeEntityType.hasSingleIdAttribute() );
		assertEquals( 2, compositeEntityType.getIdClassAttributes().size() );
	}

	@MappedSuperclass
	public abstract static class BaseEntity {
		@Id
		Long id;
	}

	@Entity( name = "SingleIdEntity" )
	public static class SingleIdEntity extends BaseEntity {
		String name;
	}

	@Entity( name = "CompositeIdEntity" )
	@IdClass( CompositeId.class )
	public static class CompositeIdEntity extends BaseEntity {
		@Id
		Long otherId;
	}

	public static class CompositeId implements Serializable {
		Long id;
		Long otherId;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof CompositeId that ) ) {
				return false;
			}
			return Objects.equals( id, that.id ) && Objects.equals( otherId, that.otherId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, otherId );
		}
	}
}
