/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * @author Andrea Boriero
 * @author Christian Beikov
 */

@DomainModel(
		annotatedClasses = {
				CollectionMapWithComponentValueTest.BaseTestEntity.class,
				CollectionMapWithComponentValueTest.TestEntity.class,
				CollectionMapWithComponentValueTest.KeyValue.class
		}
)
@SessionFactory
public class CollectionMapWithComponentValueTest {
	private final KeyValue keyValue = new KeyValue( "key1" );
	private final EmbeddableValue embeddableValue = new EmbeddableValue( 3 );


	@BeforeAll
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			keyValue.base = null;
			s.persist( keyValue );

			BaseTestEntity baseTestEntity1 = new BaseTestEntity();
			TestEntity testEntity = new TestEntity();
			Map<KeyValue, EmbeddableValue> map = new HashMap<>();
			map.put( keyValue, embeddableValue );
			testEntity.values = map;
			s.persist( testEntity );
			baseTestEntity1.entities = new HashSet<>();
			baseTestEntity1.entities.add( testEntity );
			s.persist( baseTestEntity1 );

			keyValue.base = baseTestEntity1;

			KeyValue keyValue2 = new KeyValue( "key2" );
			s.persist( keyValue2 );
			BaseTestEntity baseTestEntity2 = new BaseTestEntity();
			s.persist( baseTestEntity2 );
			TestEntity testEntity2 = new TestEntity();
			Map<KeyValue, EmbeddableValue> map2 = new HashMap<>();
			map2.put( keyValue2, embeddableValue );
			testEntity2.values = map2;
			s.persist( testEntity2 );
		} );
	}

	@Test
	public void testMapKeyExpressionInWhere(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// JPA form
			Query query = s.createQuery( "select te from TestEntity te join te.values v where ?1 in (key(v)) " );
			query.setParameter( 1, keyValue );

			assertThat( query.list().size() ).isEqualTo( 1 );

			// Hibernate additional form
			query = s.createQuery( "select te from TestEntity te where ?1 in (key(te.values))" );
			query.setParameter( 1, keyValue );

			assertThat( query.list().size() ).isEqualTo( 1 );

			// Test key property dereference
			query = s.createQuery( "select te from TestEntity te join te.values v where key(v).name in :names" );
			query.setParameterList( "names", Arrays.asList( keyValue.name ) );
			assertThat( query.list().size() ).isEqualTo( 1 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10577")
	public void testMapValueExpressionInWhere(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// JPA form
			try {
				Query query = s.createQuery( "select te from TestEntity te join te.values v where ? in (value(v))" );
				query.setParameter( 0, new EmbeddableValue( 3 ) );
				assertThat( query.list().size() ).isEqualTo( 2 );
				fail( "HibernateException expected - Could not determine type for EmbeddableValue" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}

			// Hibernate additional form
			try {
				Query query = s.createQuery( "select te from TestEntity te where ? in (value(te.values))" );
				query.setParameter( 0, new EmbeddableValue( 3 ) );
				assertThat( query.list().size() ).isEqualTo( 2 );
				fail( "HibernateException expected - Could not determine type for EmbeddableValue" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}

			// Test value property dereference
			Query query = s.createQuery( "select te from TestEntity te join te.values v where value(v).value in :values" );
			query.setParameterList( "values", Arrays.asList( 3 ) );
			assertThat( query.list().size() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testMapKeyExpressionInSelect(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// JPA form
			List results = s.createQuery( "select key(v) from TestEntity te join te.values v" ).list();
			assertThat( results.size() ).isEqualTo( 2 );
			assertTyping( KeyValue.class, results.get( 0 ) );

			// Hibernate additional form
			results = s.createQuery( "select key(te.values) from TestEntity te" ).list();
			assertThat( results.size() ).isEqualTo( 2 );
			assertTyping( KeyValue.class, results.get( 0 ) );
		} );
	}

	@Test
	public void testMapValueExpressionInSelect(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			List addresses = s.createQuery( "select value(v) from TestEntity te join te.values v" ).list();
			assertThat( addresses.size() ).isEqualTo( 2 );
			assertTyping( EmbeddableValue.class, addresses.get( 0 ) );

			addresses = s.createQuery( "select value(te.values) from TestEntity te" ).list();
			assertThat( addresses.size() ).isEqualTo( 2 );
			assertTyping( EmbeddableValue.class, addresses.get( 0 ) );
		} );
	}

	@Test
	public void testMapEntryExpressionInSelect(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			List addresses = s.createQuery( "select entry(v) from TestEntity te join te.values v" ).list();
			assertThat( addresses.size() ).isEqualTo( 2 );
			assertTyping( Map.Entry.class, addresses.get( 0 ) );

			addresses = s.createQuery( "select entry(te.values) from TestEntity te" ).list();
			assertThat( addresses.size() ).isEqualTo( 2 );
			assertTyping( Map.Entry.class, addresses.get( 0 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10577")
	public void testMapKeyExpressionDereferenceInSelect(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			List<String> keyValueNames = s.createQuery(
					"select key(v).name as name from TestEntity te join te.values v order by name",
					String.class
			).getResultList();
			assertThat( keyValueNames.size() ).isEqualTo( 2 );
			assertThat( keyValueNames.get( 0 ) ).isEqualTo( "key1" );
			assertThat( keyValueNames.get( 1 ) ).isEqualTo( "key2" );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10537")
	public void testLeftJoinMapAndUseKeyExpression(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// Assert that a left join is used for joining the map key entity table
			List keyValues = s.createQuery(
					"select key(v) from BaseTestEntity bte left join bte.entities te left join te.values v" ).list();
			System.out.println( keyValues );
			assertThat( keyValues.size() ).isEqualTo( 2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11433")
	public void testJoinMapValue(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// Assert that a left join is used for joining the map key entity table
			List keyValues = s.createQuery(
					"select v from BaseTestEntity bte left join bte.entities te left join te.values v" ).list();
			System.out.println( keyValues );
			assertThat( keyValues.size() ).isEqualTo( 2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11433")
	public void testJoinMapKey(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// Assert that a left join is used for joining the map key entity table
			List keyValues = s.createQuery(
							"select k from BaseTestEntity bte left join bte.entities te left join te.values v left join key(v) k" )
					.list();
			System.out.println( keyValues );
			assertThat( keyValues.size() ).isEqualTo( 2 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11433")
	public void testJoinMapKeyAssociation(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			List keyValues = s.createQuery(
							"select b from BaseTestEntity bte left join bte.entities te left join te.values v left join key(v) k join k.base b" )
					.list();
			System.out.println( keyValues );
			assertThat( keyValues.size() ).isEqualTo( 1 );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11433")
	public void testJoinMapKeyAssociationImplicit(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			List keyValues = s.createQuery(
							"select b from BaseTestEntity bte left join bte.entities te left join te.values v join key(v).base b" )
					.list();
			System.out.println( keyValues );
			assertThat( keyValues.size() ).isEqualTo( 1 );
		} );
	}


	@Entity(name = "BaseTestEntity")
	@Table(name = "BASE_TEST_ENTITY")
	public static class BaseTestEntity {
		@Id
		@GeneratedValue
		Long id;

		@OneToMany
		Set<TestEntity> entities;
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		Long id;

		@ElementCollection
		Map<KeyValue, EmbeddableValue> values;
	}

	@Entity(name = "KeyValue")
	@Table(name = "KEY_VALUE")
	public static class KeyValue {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		BaseTestEntity base;

		public KeyValue() {
		}

		public KeyValue(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddableValue {
		@Column(name = "val")
		Integer value;

		EmbeddableValue() {

		}

		EmbeddableValue(Integer value) {
			this.value = value;
		}
	}
}
