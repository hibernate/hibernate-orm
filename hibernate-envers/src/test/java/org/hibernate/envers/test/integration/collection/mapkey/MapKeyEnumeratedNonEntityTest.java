/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.mapkey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.envers.test.tools.TestTools.checkCollection;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-13655")
public class MapKeyEnumeratedNonEntityTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		doInJPA( this::entityManagerFactory, entityManager -> {

			final TestEntity test = new TestEntity();
			test.setId( 1 );
			test.addMapKeyAssociation( TestEnum.ONE, 1 );

			entityManager.persist( test );
		} );

		// Revision 2
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			test.addMapKeyAssociation( TestEnum.TWO, 2 );

			entityManager.merge( test );
		} );

		// Revision 3
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			test.removeMapKeyAssociation( TestEnum.ONE );
			entityManager.merge( test );
		} );

		// Revision 4
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			test.removeMapKeyAssociation( TestEnum.TWO );
			entityManager.merge( test );
		} );
	}

	@Test
	public void testRevisionNumberHistory() {
		assertEquals( Arrays.asList( 1, 2, 3, 4 ), getAuditReader().getRevisions( TestEntity.class, 1 ) );
	}

	@Test
	public void testRevisionHistory() {

		final TestEntity rev1 = getAuditReader().find( TestEntity.class, 1, 1 );
		assertEquals( 1, rev1.getMapEnumMap().size() );
		assertEquals( TestEnum.ONE, rev1.getMapEnumMap().keySet().iterator().next() );

		final TestEntity rev2 = getAuditReader().find( TestEntity.class, 1, 2 );
		assertEquals( 2, rev2.getMapEnumMap().size() );
		assertEquals( TestTools.makeSet( TestEnum.ONE, TestEnum.TWO ), rev2.getMapEnumMap().keySet() );
		checkCollection( rev2.getMapEnumMap().values(), 1, 2 );

		final TestEntity rev3 = getAuditReader().find( TestEntity.class, 1, 3 );
		assertEquals( 1, rev3.getMapEnumMap().size() );
		assertEquals( TestTools.makeSet( TestEnum.TWO ), rev3.getMapEnumMap().keySet() );
		checkCollection( rev2.getMapEnumMap().values(), 2 );

		final TestEntity rev4 = getAuditReader().find( TestEntity.class, 1, 4 );
		assertEquals( 0, rev4.getMapEnumMap().size() );
	}

	public enum TestEnum {
		ONE,
		TWO
	}

	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {
		@Id
		private Integer id;

		@MapKeyEnumerated(EnumType.STRING)
		@ElementCollection
		@CollectionTable(name = "test_Entity_enum_items")
		@MapKeyColumn(name = "type", length = 20, nullable = false)
		private Map<TestEnum, Integer> mapEnumMap = new HashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Map<TestEnum, Integer> getMapEnumMap() {
			return mapEnumMap;
		}

		public void setMapEnumMap(Map<TestEnum, Integer> mapEnumMap) {
			this.mapEnumMap = mapEnumMap;
		}

		@Override
		public String toString() {
			return "TestEntity{" +
					"id=" + id +
					", mapEnumMap=" + mapEnumMap +
					'}';
		}

		public void addMapKeyAssociation(TestEnum key, Integer value) {
			mapEnumMap.put( key, value );
		}

		public Integer removeMapKeyAssociation(TestEnum key) {
			final Integer value = mapEnumMap.get( key );
			mapEnumMap.remove( key );
			return value;
		}
	}
}
