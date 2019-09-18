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
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11797")
public class MapKeyEnumeratedTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class, MapEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		doInJPA( this::entityManagerFactory, entityManager -> {
			final MapEntity map = new MapEntity( "Map1" );
			map.setId( 1 );

			final TestEntity test = new TestEntity();
			test.setId( 1 );
			test.addMapKeyAssociation( TestEnum.ONE, map );

			entityManager.persist( test );
			entityManager.persist( map );
		} );

		// Revision 2
		doInJPA( this::entityManagerFactory, entityManager -> {
			final MapEntity map = new MapEntity( "Map2" );
			map.setId( 2 );

			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			test.addMapKeyAssociation( TestEnum.TWO, map );

			entityManager.persist( map );
			entityManager.merge( test );
		} );

		// Revision 3
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			final MapEntity map = test.removeMapKeyAssociation( TestEnum.ONE );
			entityManager.remove( map );
			entityManager.merge( test );
		} );

		// Revision 4
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			final MapEntity map = test.removeMapKeyAssociation( TestEnum.TWO );
			entityManager.remove( map );
			entityManager.merge( test );
		} );
	}

	@Test
	public void testRevisionNumberHistory() {
		assertEquals( Arrays.asList( 1, 2, 3, 4 ), getAuditReader().getRevisions( TestEntity.class, 1 ) );
		assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( MapEntity.class, 1 ) );
		assertEquals( Arrays.asList( 2, 4 ), getAuditReader().getRevisions( MapEntity.class, 2 ) );
	}

	@Test
	public void testRevisionHistory() {

		final TestEntity rev1 = getAuditReader().find( TestEntity.class, 1, 1 );
		assertEquals( 1, rev1.getMapEntityMap().size() );
		assertEquals( TestEnum.ONE, rev1.getMapEntityMap().keySet().iterator().next() );

		final TestEntity rev2 = getAuditReader().find( TestEntity.class, 1, 2 );
		assertEquals( 2, rev2.getMapEntityMap().size() );
		assertEquals( TestTools.makeSet( TestEnum.ONE, TestEnum.TWO ), rev2.getMapEntityMap().keySet() );
		assertEquals( TestTools.makeSet( 1, 2 ), rev2.getMapEntityMap().values().stream().map( MapEntity::getId ).collect( Collectors.toSet() ) );

		final TestEntity rev3 = getAuditReader().find( TestEntity.class, 1, 3 );
		assertEquals( 1, rev3.getMapEntityMap().size() );
		assertEquals( TestTools.makeSet( TestEnum.TWO ), rev3.getMapEntityMap().keySet() );
		assertEquals( TestTools.makeSet( 2 ), rev3.getMapEntityMap().values().stream().map( MapEntity::getId ).collect( Collectors.toSet() ) );

		final TestEntity rev4 = getAuditReader().find( TestEntity.class, 1, 4 );
		assertEquals( 0, rev4.getMapEntityMap().size() );
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

		@OneToMany(mappedBy = "testEntity")
		@MapKeyEnumerated(EnumType.STRING)
		private Map<TestEnum, MapEntity> mapEntityMap = new HashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Map<TestEnum, MapEntity> getMapEntityMap() {
			return mapEntityMap;
		}

		public void setMapEntityMap(Map<TestEnum, MapEntity> mapEntityMap) {
			this.mapEntityMap = mapEntityMap;
		}

		@Override
		public String toString() {
			return "TestEntity{" +
					"id=" + id +
					", mapEntityMap=" + mapEntityMap +
					'}';
		}

		public void addMapKeyAssociation(TestEnum key, MapEntity value) {
			mapEntityMap.put( key, value );
			value.setTestEntity( this );
		}

		public MapEntity removeMapKeyAssociation(TestEnum key) {
			final MapEntity value = mapEntityMap.get( key );
			value.setTestEntity( null );
			mapEntityMap.remove( key );
			return value;
		}
	}

	@Entity(name = "MapEntity")
	@Audited
	public static class MapEntity {
		@Id
		private Integer id;

		@ManyToOne(optional = false)
		private TestEntity testEntity;

		private String data;

		MapEntity() {

		}

		MapEntity(String data) {
			this.data = data;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public TestEntity getTestEntity() {
			return testEntity;
		}

		public void setTestEntity(TestEntity testEntity) {
			this.testEntity = testEntity;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		@Override
		public String toString() {
			return "MapEntity{" +
					"id=" + id +
					", data=" + data +
					", testEntity=" + ( testEntity == null ? null : testEntity.getId() ) +
					'}';
		}
	}
}
