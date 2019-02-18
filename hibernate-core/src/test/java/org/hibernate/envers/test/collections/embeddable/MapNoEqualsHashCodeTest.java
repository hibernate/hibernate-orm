/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections.embeddable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.junit5.envers.ExcludeAuditStrategy;
import org.hibernate.testing.junit5.envers.RequiresAuditStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test verifies that when a map-based {@link ElementCollection} of {@link Embeddable} objects
 * are audited that the same number of audit rows are generated regardless whether the embeddable
 * implements proper {@code equals} and {@code hashCode} methods.
 * 
 * The {@link ValidityAuditStrategy} with equals/hashcode.
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
 * The {@link DefaultAuditStrategy} with equals/hashcode.
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
public class MapNoEqualsHashCodeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					TestEntity e = new TestEntity( 1 );
					e.setEmbs1( new HashMap<>() );
					e.getEmbs1().put( "a", new Emb( "value1" ) );
					e.getEmbs1().put( "b", new Emb( "value2" ) );
					entityManager.persist( e );
				},

				entityManager -> {
					TestEntity e = entityManager.find( TestEntity.class, 1 );
					e.getEmbs1().put( "a", new Emb( "value3" ) );
				}
		);
	}

	@DynamicTest
	@RequiresAuditStrategy(ValidityAuditStrategy.class)
	public void testAuditRowsForValidityAuditStrategy() {
		inTransactions(
				entityManager -> {
					Long results = entityManager
							.createQuery( "SELECT COUNT(1) FROM TestEntity_embs1_AUD WHERE REVEND IS NULL", Long.class )
							.getSingleResult();

					assertThat( results, notNullValue() );
					assertThat( results, equalTo( 3L ) );
				},

				entityManager -> {
					Long results = entityManager
							.createQuery( "SELECT COUNT(1) FROM TestEntity_embs1_AUD", Long.class )
							.getSingleResult();

					assertThat( results, notNullValue() );
					assertThat( results, equalTo( 4L ) );
				}
		);
	}

	@DynamicTest
	@ExcludeAuditStrategy(ValidityAuditStrategy.class)
	public void testAuditRowsForDefaultAuditStrategy() {
		inTransaction(
				entityManager -> {
					Long results = entityManager
							.createQuery( "SELECT COUNT(1) FROM TestEntity_embs1_AUD", Long.class )
							.getSingleResult();

					assertThat( results, notNullValue() );
					assertThat( results, equalTo( 4L ) );
				}
		);
	}

	@DynamicTest
	public void testRevisionHistory1() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 1 );
		assertThatMapContains( e.getEmbs1(), "a", "value1", "b", "value2" );
	}

	@DynamicTest
	public void testRevisionHistory2() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 2 );
		assertThatMapContains( e.getEmbs1(), "a", "value3", "b", "value2" );
	}

	@SuppressWarnings("unchecked")
	private static void assertThatMapContains(Map<String, Emb> map, Object... values) {
		// assert that the map size is correct.
		assertThat( map.entrySet(), CollectionMatchers.hasSize( values.length / 2 ) );

		// assert that map contains the specified keys and Emb holding the specified string value.
		for ( int i = 0; i < values.length; i = i + 2 ) {
			final String key = (String) values[ i ];
			assertThat( map, hasKey( key ) );

			final Emb value = map.get( key );
			assertThat( value.getValue(), equalTo( (String) values[ i + 1 ] ) );
		}
	}

	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {
		@Id
		private Integer id;

		@ElementCollection
		private Map<String, Emb> embs1;

		public TestEntity() {

		}

		public TestEntity(Integer id) {
			this.id = id;
		}

		public Map<String, Emb> getEmbs1() {
			return embs1;
		}

		public void setEmbs1(Map<String, Emb> embs1) {
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
