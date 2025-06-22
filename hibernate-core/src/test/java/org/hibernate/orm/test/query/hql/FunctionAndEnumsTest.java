/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;
import java.util.Locale;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = FunctionAndEnumsTest.TestEntity.class
)
@SessionFactory
@JiraKey( value = "HHH-15711")
public class FunctionAndEnumsTest {


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EmbeddableThirdLevel embeddableThirdLevel = new EmbeddableThirdLevel( Level.THIRD );
					EmbeddableSecondLevel embeddableSecondLevel = new EmbeddableSecondLevel(
							Level.SECOND,
							embeddableThirdLevel
					);
					EmbeddableFirstLevel embeddableFirstLevel = new EmbeddableFirstLevel(
							Level.FIRST,
							embeddableSecondLevel
					);
					TestEntity testEntity = new TestEntity( 1l, embeddableFirstLevel );
					session.persist( testEntity );
				}
		);
	}

	@Test
	public void testLowerFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					List<String> results = session.createQuery(
							"select lower(e.embeddableFirstLevel.embeddableSecondLevel.embeddableThirdLevel.lastLevel) from TestEntity e",
							String.class
					).list();

					assertThat( results.size() ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).isEqualTo( Level.THIRD.name().toLowerCase( Locale.ROOT ) );

					results = session.createQuery(
							"select lower(e.embeddableFirstLevel.embeddableSecondLevel.anotherLevel) from TestEntity e",
							String.class
					).list();
					assertThat( results.size() ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).isEqualTo( Level.SECOND.name().toLowerCase( Locale.ROOT ) );

					results = session.createQuery(
									"select lower(e.embeddableFirstLevel.level) from TestEntity e",
									String.class
							)
							.list();
					assertThat( results.size() ).isEqualTo( 1 );
					assertThat( results.get( 0 ) ).isEqualTo( Level.FIRST.name().toLowerCase( Locale.ROOT ) );


				}
		);
	}


	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		private EmbeddableFirstLevel embeddableFirstLevel;

		public TestEntity() {
		}

		public TestEntity(Long id, EmbeddableFirstLevel embeddableFirstLevel) {
			this.id = id;
			this.embeddableFirstLevel = embeddableFirstLevel;
		}

		public Long getId() {
			return id;
		}
	}

	@Embeddable
	public static class EmbeddableFirstLevel {

		@Enumerated(EnumType.STRING)
		@Column(name = "LEVEL_COLUMN")
		private Level level;

		private EmbeddableSecondLevel embeddableSecondLevel;

		public EmbeddableFirstLevel() {
		}

		public EmbeddableFirstLevel(Level level, EmbeddableSecondLevel embeddableSecondLevel) {
			this.level = level;
			this.embeddableSecondLevel = embeddableSecondLevel;
		}
	}

	@Embeddable
	public static class EmbeddableSecondLevel {

		@Enumerated(EnumType.STRING)
		private Level anotherLevel;

		private EmbeddableThirdLevel embeddableThirdLevel;

		public EmbeddableSecondLevel() {
		}

		public EmbeddableSecondLevel(Level anotherLevel, EmbeddableThirdLevel embeddableThirdLevel) {
			this.anotherLevel = anotherLevel;
			this.embeddableThirdLevel = embeddableThirdLevel;
		}
	}

	@Embeddable
	public static class EmbeddableThirdLevel {

		@Enumerated(EnumType.STRING)
		private Level lastLevel;

		public EmbeddableThirdLevel() {
		}

		public EmbeddableThirdLevel(Level lastLevel) {
			this.lastLevel = lastLevel;
		}
	}


	public enum Level {
		FIRST,
		SECOND,
		THIRD
	}
}
