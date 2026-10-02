package org.hibernate.orm.test.bytecode.enhancement.refresh;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@DomainModel(
		annotatedClasses = {
				MergeAndRefreshTest.Phase.class,
				MergeAndRefreshTest.PhaseDescription.class
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-17668")
public class MergeAndRefreshTest {

	@Test
	public void testRefresh(SessionFactoryScope scope) {
		Long phaseId = 1L;
		scope.inTransaction(
				session -> {
					PhaseDescription description = new PhaseDescription("phase 1");
					Phase phase = new Phase( phaseId, description );
					session.persist( phase );
				}
		);

		Phase phase = scope.fromTransaction( session -> session.find( Phase.class, phaseId ) );

		scope.inTransaction(
				session -> {
					Phase merged = session.merge( phase );
					session.refresh( merged );
				}
		);
	}

	@Entity(name = "Phase")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Phase {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "phase_description_id")
		private PhaseDescription description;

		private String name;

		public Phase() {
		}

		public Phase(Long id, PhaseDescription description) {
			this.id = id;
			this.description = description;
			this.description.phase = this;
		}

		public Long getId() {
			return id;
		}

		public PhaseDescription getDescription() {
			return description;
		}
	}

	@Entity(name = "PhaseDescription")
	public static class PhaseDescription {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public PhaseDescription() {
		}

		public PhaseDescription(String name) {
			this.name = name;
		}

		@OneToOne(mappedBy = "description")
		@Fetch(value = FetchMode.SELECT)
		private Phase phase;

		public Long getId() {
			return id;
		}

		public Phase getPhase() {
			return phase;
		}
	}
}
