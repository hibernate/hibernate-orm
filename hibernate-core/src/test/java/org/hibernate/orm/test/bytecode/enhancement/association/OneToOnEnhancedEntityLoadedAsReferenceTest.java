/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-17173")
@DomainModel(
		annotatedClasses = {
			OneToOnEnhancedEntityLoadedAsReferenceTest.ContainingEntity.class, OneToOnEnhancedEntityLoadedAsReferenceTest.ContainedEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10"),
				@Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = "0")
		}
)
@SessionFactory
@BytecodeEnhanced
public class OneToOnEnhancedEntityLoadedAsReferenceTest {

	private long entityId;
	private long entityId2;
	private long containedEntityId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			ContainingEntity entity = new ContainingEntity();
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setValue( "value" );
			entity.setContained( containedEntity );
			containedEntity.setContaining( entity );

			em.persist( entity );
			em.persist( containedEntity );
			entityId = entity.getId();
			containedEntityId = containedEntity.getId();

			ContainingEntity entity2 = new ContainingEntity();
			em.persist( entity2 );
			entityId2 = entity2.getId();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			ContainingEntity entity2 = em.getReference( ContainingEntity.class, entityId2 );
			ContainingEntity entity = em.getReference( ContainingEntity.class, entityId );
			ContainedEntity containedEntity = em.getReference( ContainedEntity.class, containedEntityId );
			// We're working on an uninitialized proxy.
			assertThat( entity ).returns( false, Hibernate::isInitialized );
			// The above should have persisted a value that passes the assertion.
			assertThat( entity.getContained() ).isEqualTo( containedEntity );
			assertThat( entity.getContained().getValue() ).isEqualTo( "value" );
			// Accessing the value should trigger initialization of the proxy.
			assertThat( entity ).returns( true, Hibernate::isInitialized );
		} );
	}

	@Entity(name = "ContainingEntity")
	public static class ContainingEntity {

		@Id
		@GeneratedValue
		public long id;

		@OneToOne(mappedBy = "containing")
		private ContainedEntity contained;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(ContainedEntity oneToOneMappedBy) {
			this.contained = oneToOneMappedBy;
		}
	}

	@Entity(name = "ContainedEntity")
	public static class ContainedEntity {

		@Id
		@GeneratedValue
		private long id;

		@Column(name = "value_")
		private String value;

		@OneToOne
		private ContainingEntity containing;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public ContainingEntity getContaining() {
			return containing;
		}

		public void setContaining(ContainingEntity oneToOne) {
			this.containing = oneToOne;
		}

	}
}
