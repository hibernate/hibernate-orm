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
import jakarta.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests an entity mapping using an entity as a map-key and map-value.
 *
 * This only fails on {@code DefaultAuditStrategy} because the {@code ValidityAuditStrategy} does
 * not make use of the related-id data of the middle table like the default audit strategy.
 *
 * This test verifies both strategies work, but the failure is only applicable for the default strategy.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11892")
@EnversTest
@Jpa(annotatedClasses = {
		EntityMapTest.A.class,
		EntityMapTest.B.class,
		EntityMapTest.C.class
})
public class EntityMapTest {

	private Integer aId;
	private B b1;
	private B b2;
	private C c1;
	private C c2;

	@MappedSuperclass
	public static abstract class AbstractEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			AbstractEntity that = (AbstractEntity) o;

			return id != null ? id.equals( that.id ) : that.id == null;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	@Entity(name = "A")
	@Audited
	public static class A extends AbstractEntity {
		@ElementCollection
		private Map<B, C> map = new HashMap<>();

		public Map<B, C> getMap() {
			return map;
		}

		public void setMap(Map<B, C> map) {
			this.map = map;
		}
	}

	@Entity(name = "B")
	@Audited
	public static class B extends AbstractEntity {

	}

	@Entity(name = "C")
	@Audited
	public static class C extends AbstractEntity {

	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// add b/c key-pair to the map and save a entity.
		scope.inTransaction( entityManager -> {
			final A a = new A();

			final B b = new B();
			final C c = new C();
			entityManager.persist( b );
			entityManager.persist( c );

			a.getMap().put( b, c );
			entityManager.persist( a );

			this.aId = a.getId();
			this.b1 = b;
			this.c1 = c;
		} );

		// add a new b/c key-pair to the map
		scope.inTransaction( entityManager -> {
			final A a = entityManager.find( A.class, this.aId );

			final B b = new B();
			final C c = new C();
			entityManager.persist( b );
			entityManager.persist( c );

			a.getMap().put( b, c );
			entityManager.merge( a );

			this.b2 = b;
			this.c2 = c;
		} );

		// Remove b1 from the map
		scope.inTransaction( entityManager -> {
			final A a = entityManager.find( A.class, this.aId );
			a.getMap().remove( this.b1 );
			entityManager.merge( a );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var auditReader = AuditReaderFactory.get( entityManager );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( A.class, aId ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( B.class, b1.getId() ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( C.class, c1.getId() ) );

			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( B.class, b2.getId() ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( C.class, c2.getId() ) );
		} );
	}

	@Test
	public void testRevision1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final A rev1 = auditReader.find( A.class, this.aId, 1 );
			assertEquals( 1, rev1.getMap().size() );
			assertEquals( TestTools.makeMap( this.b1, this.c1 ), rev1.getMap() );
		} );
	}

	@Test
	public void testRevision2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final A rev2 = auditReader.find( A.class, this.aId, 2 );
			assertEquals( 2, rev2.getMap().size() );
			assertEquals( TestTools.makeMap( this.b1, this.c1, this.b2, this.c2 ), rev2.getMap() );
		} );
	}

	@Test
	public void testRevision3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final A rev3 = auditReader.find( A.class, this.aId, 3 );
			assertEquals( 1, rev3.getMap().size() );
			assertEquals( TestTools.makeMap( this.b2, this.c2 ), rev3.getMap() );
		} );
	}
}
