/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12018")
@EnversTest
@Jpa(annotatedClasses = {
		EntitySameMapKeyMultiMapValueTest.SomeEntity.class,
		EntitySameMapKeyMultiMapValueTest.OtherEntity.class
})
public class EntitySameMapKeyMultiMapValueTest {

	private Integer otherEntityId;
	private Integer someEntityId;

	@Entity(name = "SomeEntity")
	@Audited
	public static class SomeEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@ElementCollection
		private Map<OtherEntity, Status> map = new HashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Map<OtherEntity, Status> getMap() {
			return map;
		}

		public void setMap(Map<OtherEntity, Status> map) {
			this.map = map;
		}

		enum Status {
			A, B
		};
	}

	@Entity(name = "OtherEntity")
	@Audited
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final SomeEntity someEntity = new SomeEntity();
			final OtherEntity otherEntity = new OtherEntity();
			entityManager.persist( otherEntity );

			someEntity.getMap().put( otherEntity, SomeEntity.Status.A );
			entityManager.persist( someEntity );

			this.otherEntityId = otherEntity.getId();
			this.someEntityId = someEntity.getId();
		} );

		scope.inTransaction( entityManager -> {
			final SomeEntity someEntity = entityManager.find( SomeEntity.class, someEntityId );
			final OtherEntity otherEntity = entityManager.find( OtherEntity.class, otherEntityId );
			someEntity.getMap().put( otherEntity, SomeEntity.Status.B );
			entityManager.merge( someEntity );
		} );

		scope.inTransaction( entityManager -> {
			final SomeEntity someEntity = entityManager.find( SomeEntity.class, someEntityId );
			someEntity.getMap().clear();
			entityManager.merge( someEntity );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( OtherEntity.class, otherEntityId ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( SomeEntity.class, someEntityId ) );
		} );
	}

	@Test
	public void blockTest(EntityManagerFactoryScope scope) {
		scope.fromEntityManager( AuditReaderFactory::get );
		System.out.println( "Halt" );
	}

	@Test
	public void testRevisionOne(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final SomeEntity someEntity = auditReader.find( SomeEntity.class, someEntityId, 1 );
			assertNotNull( someEntity );
			assertFalse( someEntity.getMap().isEmpty() );
			assertEquals( 1, someEntity.getMap().size() );

			final OtherEntity otherEntity = auditReader.find( OtherEntity.class, otherEntityId, 1 );
			assertNotNull( otherEntity );

			final Map.Entry<OtherEntity, SomeEntity.Status> entry = someEntity.getMap().entrySet().iterator().next();
			assertEquals( otherEntity, entry.getKey() );
			assertEquals( SomeEntity.Status.A, entry.getValue() );
		} );
	}

	@Test
	public void testRevisionTwo(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final SomeEntity someEntity = auditReader.find( SomeEntity.class, someEntityId, 2 );
			assertNotNull( someEntity );
			assertFalse( someEntity.getMap().isEmpty() );
			assertEquals( 1, someEntity.getMap().size() );

			final OtherEntity otherEntity = auditReader.find( OtherEntity.class, otherEntityId, 2 );
			assertNotNull( otherEntity );

			final Map.Entry<OtherEntity, SomeEntity.Status> entry = someEntity.getMap().entrySet().iterator().next();
			assertEquals( otherEntity, entry.getKey() );
			assertEquals( SomeEntity.Status.B, entry.getValue() );
		} );
	}

	@Test
	public void testRevisionThree(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final SomeEntity someEntity = auditReader.find( SomeEntity.class, someEntityId, 3 );
			assertNotNull( someEntity );
			assertTrue( someEntity.getMap().isEmpty() );
		} );
	}
}
