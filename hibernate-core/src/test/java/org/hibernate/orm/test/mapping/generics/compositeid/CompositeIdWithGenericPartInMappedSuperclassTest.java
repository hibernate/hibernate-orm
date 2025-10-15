/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generics.compositeid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Objects;

@JiraKey("HHH-19706")
@DomainModel(
		annotatedClasses = {
				CompositeIdWithGenericPartInMappedSuperclassTest.SampleCompositeIdEntity.class,
				CompositeIdWithGenericPartInMappedSuperclassTest.SampleEntity.class,
				CompositeIdWithGenericPartInMappedSuperclassTest.SampleSuperclass.class
		}
)
@SessionFactory
class CompositeIdWithGenericPartInMappedSuperclassTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inSession( s -> s.createQuery( "from SampleEntity", SampleEntity.class ).getResultList() );
	}

	@Entity
	@IdClass(SampleCompositeIdEntity.AdditionalIdEntityId.class)
	static class SampleCompositeIdEntity extends SampleSuperclass<Long> {

		@Id
		@Column(nullable = false, updatable = false)
		private Long additionalId;

		static class AdditionalIdEntityId implements Serializable {

			final Long mainId;
			final Long additionalId;

			public AdditionalIdEntityId() {
				this( 0L, 0L );
			}

			public AdditionalIdEntityId(Long mainId, Long additionalId) {
				this.mainId = mainId;
				this.additionalId = additionalId;
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( !(o instanceof AdditionalIdEntityId) ) {
					return false;
				}
				AdditionalIdEntityId key = (AdditionalIdEntityId) o;
				return Objects.equals( mainId, key.mainId ) && Objects.equals( additionalId, key.additionalId );
			}

			@Override
			public int hashCode() {
				return Objects.hash( mainId, additionalId );
			}

		}

	}

	@Entity(name = "SampleEntity")
	static class SampleEntity extends SampleSuperclass<Long> {

		private String sampleField;

	}

	@MappedSuperclass
	abstract static class SampleSuperclass<K extends Serializable & Comparable<K>> {

		@Id
		private K mainId;

	}
}
