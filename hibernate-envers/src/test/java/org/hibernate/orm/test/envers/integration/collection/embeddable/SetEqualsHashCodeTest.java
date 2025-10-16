/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.RequiresAuditStrategy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test verifies that when a set-based {@link ElementCollection} of {@link Embeddable} objects
 * are audited that the same number of audit rows are generated regardless whether the embeddable
 * implements proper {@code equals} and {@code hashCode} methods.
 * <p>
 * The {@link ValidityAuditStrategy} with equals/hashcode.
 * <p>
 * +-----+---------+---------------+-----------+--------+--------+
 * | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | REVEND | VALUE  |
 * +-----+---------+---------------+-----------+--------+--------+
 * | 1   | 0       | 1             | a         | 2      | value1 |
 * | 1   | 0       | 1             | b         | null   | value2 |
 * | 2   | 0       | 1             | a         | null   | value3 |
 * | 2   | 2       | 1             | a         | null   | value1 |
 * +-----+---------+---------------+-----------+--------+--------+
 * <p>
 * The {@link org.hibernate.envers.strategy.DefaultAuditStrategy} with equals/hashcode.
 * <p>
 * +-----+---------+---------------+-----------+--------+
 * | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | VALUE  |
 * +-----+---------+---------------+-----------+--------+
 * | 1   | 0       | 1             | a         | value1 |
 * | 1   | 0       | 1             | b         | value2 |
 * | 2   | 0       | 1             | a         | value3 |
 * | 2   | 2       | 1             | a         | value1 |
 * +-----+---------+---------------+-----------+--------+
 * <p>
 * This test uses hashcode and equals as a baseline.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12607")
@EnversTest
@Jpa(annotatedClasses = {SetEqualsHashCodeTest.TestEntity.class})
public class SetEqualsHashCodeTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		final Emb emb1 = new Emb( "value1" );
		final Emb emb2 = new Emb( "value2" );
		scope.inTransaction( entityManager -> {
			TestEntity e = new TestEntity( 1 );
			e.setEmbs1( new HashSet<>() );
			e.getEmbs1().add( emb1 );
			e.getEmbs1().add( emb2 );
			entityManager.persist( e );
		} );

		scope.inTransaction( entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1 );
			e.getEmbs1().remove( emb1 );
			e.getEmbs1().add( new Emb( "value3" ) );
		} );
	}

	@Test
	@RequiresAuditStrategy(ValidityAuditStrategy.class)
	public void testAuditRowsForValidityAuditStrategy(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Long results = entityManager
					.createQuery(
							"SELECT COUNT(1) FROM TestEntity_embs1_AUD WHERE REVEND IS NULL",
							Long.class
					)
					.getSingleResult();

			assertNotNull( results );
			assertEquals( Long.valueOf( 3 ), results );

			results = entityManager
					.createQuery(
							"SELECT COUNT(1) FROM TestEntity_embs1_AUD",
							Long.class
					)
					.getSingleResult();

			assertNotNull( results );
			assertEquals( Long.valueOf( 4 ), results );
		} );
	}

	@Test
	@RequiresAuditStrategy(DefaultAuditStrategy.class)
	public void testAuditRowsForDefaultAuditStrategy(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Long results = entityManager
					.createQuery(
							"SELECT COUNT(1) FROM TestEntity_embs1_AUD",
							Long.class
					)
					.getSingleResult();

			assertNotNull( results );
			assertEquals( Long.valueOf( 4 ), results );
		} );
	}

	@Test
	public void testRevisionHistory1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			TestEntity e = auditReader.find( TestEntity.class, 1, 1 );
			assertEquals( 2, e.getEmbs1().size() );
			assertEquals( e.getEmbs1(), TestTools.makeSet( new Emb( "value1" ), new Emb( "value2" ) ) );
		} );
	}

	@Test
	public void testRevisionHistory2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			TestEntity e = auditReader.find( TestEntity.class, 1, 2 );
			assertEquals( 2, e.getEmbs1().size() );
			assertEquals( e.getEmbs1(), TestTools.makeSet( new Emb( "value3" ), new Emb( "value2" ) ) );
		} );
	}

	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {
		@Id
		private Integer id;

		@ElementCollection
		private Set<Emb> embs1;

		public TestEntity() {

		}

		public TestEntity(Integer id) {
			this.id = id;
		}

		public Set<Emb> getEmbs1() {
			return embs1;
		}

		public void setEmbs1(Set<Emb> embs1) {
			this.embs1 = embs1;
		}
	}

	@Embeddable
	public static class Emb implements Serializable {
		@Column(name = "val")
		private String value;

		public Emb() {

		}

		public Emb(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Emb emb = (Emb) o;
			return Objects.equals( value, emb.value );
		}

		@Override
		public int hashCode() {
			return Objects.hash( value );
		}
	}
}
