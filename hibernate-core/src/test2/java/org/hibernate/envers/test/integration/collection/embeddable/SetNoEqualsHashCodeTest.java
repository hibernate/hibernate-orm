/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * This test verifies that when a set-based {@link ElementCollection} of {@link Embeddable} objects
 * are audited that the same number of audit rows are generated regardless whether the embeddable
 * implements proper {@code equals} and {@code hashCode} methods.
 *
 * The {@link org.hibernate.envers.strategy.ValidityAuditStrategy} with equals/hashcode.
 *
 * +-----+---------+---------------+-----------+--------+--------+
 * | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | REVEND | VALUE  |
 * +-----+---------+---------------+-----------+--------+--------+
 * | 1   | 0       | 1             | a         | 2      | value1 |
 * | 1   | 0       | 1             | b         | null   | value2 |
 * | 2   | 0       | 1             | a         | null   | value3 |
 * | 2   | 2       | 1             | a         | null   | value1 |
 * +-----+---------+---------------+-----------+--------+--------+
 *
 * The {@link org.hibernate.envers.strategy.DefaultAuditStrategy} with equals/hashcode.
 *
 * +-----+---------+---------------+-----------+--------+
 * | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | VALUE  |
 * +-----+---------+---------------+-----------+--------+
 * | 1   | 0       | 1             | a         | value1 |
 * | 1   | 0       | 1             | b         | value2 |
 * | 2   | 0       | 1             | a         | value3 |
 * | 2   | 2       | 1             | a         | value1 |
 * +-----+---------+---------------+-----------+--------+
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12607")
public class SetNoEqualsHashCodeTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		final Emb emb1 = new Emb( "value1" );
		final Emb emb2 = new Emb( "value2" );
		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = new TestEntity( 1 );
			e.setEmbs1( new HashSet<>() );
			e.getEmbs1().add( emb1 );
			e.getEmbs1().add( emb2 );
			entityManager.persist( e );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1 );
			for ( Emb emb : e.getEmbs1() ) {
				if ( emb.getValue().equals( "value1" ) ) {
					e.getEmbs1().remove( emb );
					break;
				}
			}
			e.getEmbs1().add( new Emb( "value3" ) );
		} );
	}

	@Test
	public void testAuditRowsForValidityAuditStrategy() {
		if ( ValidityAuditStrategy.class.getName().equals( getAuditStrategy() ) ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Long results = entityManager
						.createQuery(
								"SELECT COUNT(1) FROM TestEntity_embs1_AUD WHERE REVEND IS NULL",
								Long.class
						)
						.getSingleResult();

				assertNotNull( results );
				assertEquals( Long.valueOf( 3 ), results );
			} );

			doInJPA( this::entityManagerFactory, entityManager -> {
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
	}

	@Test
	public void testAuditRowsForDefaultAuditStrategy() {
		if ( !ValidityAuditStrategy.class.getName().equals( getAuditStrategy() ) ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
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
	}

	@Test
	public void testRevisionHistory1() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 1 );
		assertEquals( 2, e.getEmbs1().size() );
		assertHasEmbeddableWithValue( e, "value1" );
		assertHasEmbeddableWithValue( e, "value2" );
	}

	@Test
	public void testRevisionHistory2() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 2 );
		assertEquals( 2, e.getEmbs1().size() );
		assertHasEmbeddableWithValue( e, "value3" );
		assertHasEmbeddableWithValue( e, "value2" );
	}

	private static void assertHasEmbeddableWithValue(TestEntity entity, String value) {
		for ( Emb embeddable : entity.getEmbs1() ) {
			if ( embeddable.getValue().equals( value ) ) {
				return;
			}
		}
		fail( "Failed to find embeddable with value [" + value + "]" );
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
	}
}
