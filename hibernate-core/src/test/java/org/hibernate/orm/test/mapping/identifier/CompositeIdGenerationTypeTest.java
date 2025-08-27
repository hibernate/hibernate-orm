/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		CompositeIdGenerationTypeTest.SingleIdClass.class,
		CompositeIdGenerationTypeTest.MultipleIdClass.class,
		CompositeIdGenerationTypeTest.IdClassPK.class,
		CompositeIdGenerationTypeTest.SingleEmbeddedId.class,
		CompositeIdGenerationTypeTest.SingleEmbeddedPK.class,
		CompositeIdGenerationTypeTest.MultipleEmbeddedId.class,
		CompositeIdGenerationTypeTest.MultipleEmbeddedPK.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18524" )
public class CompositeIdGenerationTypeTest {
	@Test
	public void testSingleIdClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SingleIdClass( 1L, "id_class_1" ) ) );
		scope.inSession( session -> {
			final SingleIdClass result = session.createQuery( "from SingleIdClass", SingleIdClass.class )
					.getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
			assertDoesNotThrow( () -> UUID.fromString( result.getUuid() ) );
		} );
	}

	@Test
	public void testMultipleIdClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new MultipleIdClass( "id_class_2" ) ) );
		scope.inSession( session -> {
			final MultipleIdClass result = session.createQuery( "from MultipleIdClass", MultipleIdClass.class )
					.getSingleResult();
			assertThat( result.getId() ).isGreaterThan( 0 );
			assertDoesNotThrow( () -> UUID.fromString( result.getUuid() ) );
		} );
	}

	@Test
	public void testSingleEmbeddedId(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SingleEmbeddedId(
				new SingleEmbeddedPK( 1L ),
				"embedded_id_1"
		) ) );
		scope.inSession( session -> {
			final SingleEmbeddedId result = session.createQuery( "from SingleEmbeddedId", SingleEmbeddedId.class )
					.getSingleResult();
			final SingleEmbeddedPK embeddedId = result.getEmbeddedId();
			assertThat( embeddedId.getId() ).isEqualTo( 1L );
			assertDoesNotThrow( () -> UUID.fromString( embeddedId.getUuid() ) );
		} );
	}

	@Test
	public void testMultipleEmbeddedId(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new MultipleEmbeddedId(
				new MultipleEmbeddedPK(),
				"embedded_id_2"
		) ) );

		scope.inTransaction( session -> {
			final MultipleEmbeddedId result = session.createQuery( "from MultipleEmbeddedId", MultipleEmbeddedId.class )
					.getSingleResult();
			final MultipleEmbeddedPK embeddedId = result.getEmbeddedId();
			assertThat( embeddedId.getId() ).isGreaterThan( 0 );
			assertDoesNotThrow( () -> UUID.fromString( embeddedId.getUuid() ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "SingleIdClass" )
	@IdClass( IdClassPK.class )
	static class SingleIdClass {
		@Id
		private Long id;

		@Id
		@UuidGenerator
		private String uuid;

		private String name;

		public SingleIdClass() {
		}

		public SingleIdClass(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getUuid() {
			return uuid;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	static class IdClassPK {
		private Long id;
		private String uuid;

		public IdClassPK() {
		}

		public IdClassPK(Long id, String uuid) {
			this.id = id;
			this.uuid = uuid;
		}
	}

	@Entity( name = "MultipleIdClass" )
	@IdClass( IdClassPK.class )
	static class MultipleIdClass {
		@Id
		@GeneratedValue
		private Long id;

		@Id
		@UuidGenerator
		private String uuid;

		private String name;

		public MultipleIdClass() {
		}

		public MultipleIdClass(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getUuid() {
			return uuid;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "SingleEmbeddedId" )
	static class SingleEmbeddedId {
		@EmbeddedId
		private SingleEmbeddedPK embeddedId;

		private String name;

		public SingleEmbeddedId() {
		}

		public SingleEmbeddedId(SingleEmbeddedPK embeddedId, String name) {
			this.embeddedId = embeddedId;
			this.name = name;
		}

		public SingleEmbeddedPK getEmbeddedId() {
			return embeddedId;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	static class SingleEmbeddedPK {
		private Long id;

		@UuidGenerator
		private String uuid;

		public SingleEmbeddedPK() {
		}

		public SingleEmbeddedPK(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public String getUuid() {
			return uuid;
		}
	}

	@Entity( name = "MultipleEmbeddedId" )
	static class MultipleEmbeddedId {
		@EmbeddedId
		private MultipleEmbeddedPK embeddedId;

		private String name;

		public MultipleEmbeddedId() {
		}

		public MultipleEmbeddedId(MultipleEmbeddedPK embeddedId, String name) {
			this.embeddedId = embeddedId;
			this.name = name;
		}

		public MultipleEmbeddedPK getEmbeddedId() {
			return embeddedId;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	static class MultipleEmbeddedPK {
		@GeneratedValue
		private Long id;

		@UuidGenerator
		private String uuid;

		public Long getId() {
			return id;
		}

		public String getUuid() {
			return uuid;
		}
	}
}
