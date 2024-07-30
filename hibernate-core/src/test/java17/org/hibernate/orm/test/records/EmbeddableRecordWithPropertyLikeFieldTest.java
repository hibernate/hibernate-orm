/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.records;

import org.hibernate.orm.test.records.EmbeddableRecordWithPropertyLikeFieldTest.EmbeddableRecordWithDifferentTypes.DifferentTypes;
import org.hibernate.orm.test.records.EmbeddableRecordWithPropertyLikeFieldTest.EmbeddableRecordWithGet.Get;
import org.hibernate.orm.test.records.EmbeddableRecordWithPropertyLikeFieldTest.EmbeddableRecordWithIs.Is;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * It is important that {@link Is}, {@link Get},
 * {@link DifferentTypes} has fields in non-alphabetical order.
 */
@JiraKey("HHH-18445")
@DomainModel(
		annotatedClasses = {
				EmbeddableRecordWithPropertyLikeFieldTest.EmbeddableRecordWithIs.class,
				EmbeddableRecordWithPropertyLikeFieldTest.EmbeddableRecordWithGet.class,
				EmbeddableRecordWithPropertyLikeFieldTest.EmbeddableRecordWithDifferentTypes.class
		}
)
@SessionFactory
public class EmbeddableRecordWithPropertyLikeFieldTest {

	@BeforeEach
	protected void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new EmbeddableRecordWithIs( 1L, new Is( "Island B", "Island A" ) ) );
			session.persist( new EmbeddableRecordWithGet( 1L, new Get( "Getaway B", "Getaway A" ) ) );
			session.persist( new EmbeddableRecordWithDifferentTypes( 1L, new DifferentTypes( "Issue", 50L ) ) );
		} );
	}

	@AfterEach
	protected void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EmbeddableRecordWithIs" )
					.executeUpdate();
			session.createMutationQuery( "delete from EmbeddableRecordWithGet" )
					.executeUpdate();
			session.createMutationQuery( "delete from EmbeddableRecordWithDifferentTypes" )
					.executeUpdate();
		} );
	}

	@Test
	public void testRecordWithIsField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EmbeddableRecordWithIs entity = session.get( EmbeddableRecordWithIs.class, 1L );
			assertEquals( "Island B", entity.getRecord().islandB() );
			assertEquals( "Island A", entity.getRecord().islandA() );
		} );
	}

	@Test
	public void testRecordWithGetField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EmbeddableRecordWithGet entity = session.get( EmbeddableRecordWithGet.class, 1L );
			assertEquals( "Getaway B", entity.getRecord().getawayB() );
			assertEquals( "Getaway A", entity.getRecord().getawayA() );
		} );
	}

	@Test
	public void testRecordWithDifferentTypes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EmbeddableRecordWithDifferentTypes entity = session.get( EmbeddableRecordWithDifferentTypes.class, 1L );
			assertEquals( "Issue", entity.getRecord().issue() );
			assertEquals( 50L, entity.getRecord().argument() );
		} );
	}

	@Entity
	public static class EmbeddableRecordWithIs {
		@Id
		private Long id;
		@Embedded
		private Is record;

		public EmbeddableRecordWithIs() {
		}

		public EmbeddableRecordWithIs(Long id, Is record) {
			this.id = id;
			this.record = record;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Is getRecord() {
			return record;
		}

		public void setRecord(Is record) {
			this.record = record;
		}

		@Embeddable
		public record Is(String islandB, String islandA) {
		}
	}

	@Entity
	public static class EmbeddableRecordWithGet {
		@Id
		private Long id;
		@Embedded
		private Get record;

		public EmbeddableRecordWithGet() {
		}

		public EmbeddableRecordWithGet(Long id, Get record) {
			this.id = id;
			this.record = record;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Get getRecord() {
			return record;
		}

		public void setRecord(Get record) {
			this.record = record;
		}

		@Embeddable
		public record Get(String getawayB, String getawayA) {
		}
	}

	@Entity
	public static class EmbeddableRecordWithDifferentTypes {
		@Id
		private Long id;
		@Embedded
		DifferentTypes record;

		public EmbeddableRecordWithDifferentTypes() {
		}

		public EmbeddableRecordWithDifferentTypes(Long id, DifferentTypes record) {
			this.id = id;
			this.record = record;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public DifferentTypes getRecord() {
			return record;
		}

		public void setRecord(DifferentTypes record) {
			this.record = record;
		}

		@Embeddable
		public record DifferentTypes(String issue, Long argument) {
		}
	}
}
