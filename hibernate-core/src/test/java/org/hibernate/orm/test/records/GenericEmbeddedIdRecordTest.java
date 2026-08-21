/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = {GenericEmbeddedIdRecordTest.MainEntity.class, GenericEmbeddedIdRecordTest.ReferencedEntity.class})
class GenericEmbeddedIdRecordTest {

	@Test
	void testOverrideJoinColumn(SessionFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			MainEntity mainEntity = new MainEntity();
			ReferencedEntity referencedEntity = new ReferencedEntity();
			mainEntity.id = new GenericEmbeddedId<>(referencedEntity);
			referencedEntity.entities = Collections.singleton(mainEntity);
			entityManager.persist(referencedEntity);
			assertNotNull(mainEntity);
		});
	}

	@Embeddable
	public record GenericEmbeddedId<T>(@ManyToOne(fetch = FetchType.EAGER)
									@JoinColumn(name = "REFERENCED_ENTITY_ID")
									T referencedEntity) {
	}

	@Entity
	public static class MainEntity {

		@EmbeddedId
		private GenericEmbeddedId<ReferencedEntity> id;

		@Column(name = "DESCRIPTION")
		private String description;

	}

	@Entity
	public static class ReferencedEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE,
				generator = "REFERENCED_ENTITY_ID_GEN")
		@SequenceGenerator(name = "REFERENCED_ENTITY_ID_GEN")
		@Column(name = "REFERENCED_ENTITY_ID")
		private Long id;

		@OneToMany(mappedBy = "id.referencedEntity", cascade = CascadeType.ALL)
		private Collection<MainEntity> entities;

	}
}
