/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.mapkey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11797")
@EnversTest
@Jpa(annotatedClasses = {MapKeyEnumeratedTest.TestEntity.class, MapKeyEnumeratedTest.MapEntity.class})
public class MapKeyEnumeratedTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( entityManager -> {
			final MapEntity map = new MapEntity( "Map1" );
			map.setId( 1 );

			final TestEntity test = new TestEntity();
			test.setId( 1 );
			test.addMapKeyAssociation( TestEnum.ONE, map );

			entityManager.persist( test );
			entityManager.persist( map );
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			final MapEntity map = new MapEntity( "Map2" );
			map.setId( 2 );

			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			test.addMapKeyAssociation( TestEnum.TWO, map );

			entityManager.persist( map );
			entityManager.merge( test );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			final MapEntity map = test.removeMapKeyAssociation( TestEnum.ONE );
			entityManager.remove( map );
			entityManager.merge( test );
		} );

		// Revision 4
		scope.inTransaction( entityManager -> {
			final TestEntity test = entityManager.find( TestEntity.class, 1 );
			final MapEntity map = test.removeMapKeyAssociation( TestEnum.TWO );
			entityManager.remove( map );
			entityManager.merge( test );
		} );
	}

	@Test
	public void testRevisionNumberHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( TestEntity.class, 1 ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( MapEntity.class, 1 ) );
			assertEquals( Arrays.asList( 2, 4 ), auditReader.getRevisions( MapEntity.class, 2 ) );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );

			final TestEntity rev1 = auditReader.find( TestEntity.class, 1, 1 );
			assertEquals( 1, rev1.getMapEntityMap().size() );
			assertEquals( TestEnum.ONE, rev1.getMapEntityMap().keySet().iterator().next() );

			final TestEntity rev2 = auditReader.find( TestEntity.class, 1, 2 );
			assertEquals( 2, rev2.getMapEntityMap().size() );
			assertEquals( TestTools.makeSet( TestEnum.ONE, TestEnum.TWO ), rev2.getMapEntityMap().keySet() );
			assertEquals( TestTools.makeSet( 1, 2 ), rev2.getMapEntityMap().values().stream().map( MapEntity::getId ).collect( Collectors.toSet() ) );

			final TestEntity rev3 = auditReader.find( TestEntity.class, 1, 3 );
			assertEquals( 1, rev3.getMapEntityMap().size() );
			assertEquals( TestTools.makeSet( TestEnum.TWO ), rev3.getMapEntityMap().keySet() );
			assertEquals( TestTools.makeSet( 2 ), rev3.getMapEntityMap().values().stream().map( MapEntity::getId ).collect( Collectors.toSet() ) );

			final TestEntity rev4 = auditReader.find( TestEntity.class, 1, 4 );
			assertEquals( 0, rev4.getMapEntityMap().size() );
		} );
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
