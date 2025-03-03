/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(annotatedClasses = {
		ElementCollectionPerfTest.Element.class,
		ElementCollectionPerfTest.KeyValue.class,
		ElementCollectionPerfTest.Association.class,
})
@Jira("https://hibernate.atlassian.net/browse/HHH-18375")
public class ElementCollectionPerfTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( int i = 0; i < 100; i++ ) {
				final String id = UUID.randomUUID().toString();
				final Element element = new Element( id );
				element.setKeyValueEmbeddable( new KeyValue( "embeddable", "_" + id ) );
				element.setAssociation1( new Association( (long) i, "assoc_" + id ) );

				final Set<KeyValue> key1Values = new HashSet<>();
				key1Values.add( new KeyValue( "key1_1", "_" + id ) );
				key1Values.add( new KeyValue( "key1_2", "_" + id ) );
				key1Values.add( new KeyValue( "key1_3", "_" + id ) );
				element.setKeyValues1( key1Values );

				element.association1.keyValues1 = new HashSet<>( key1Values);

				final Set<KeyValue> key2Values = new HashSet<>();
				key2Values.add( new KeyValue( "key2_1", "_" + id ) );
				key2Values.add( new KeyValue( "key2_2", "_" + id ) );
				element.setKeyValues2( key2Values );

				final Map<String, KeyValue> map = new HashMap<>();
				map.put( "k1", new KeyValue( "k1", "_" + id ) );
				map.put( "k2", new KeyValue( "k2", "_" + id ) );
				element.setMap( map );

				entityManager.persist( element );
			}
		} );
	}

	@Test
	public void testSelect0(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Element> result = entityManager.createQuery(
							"select e from Element e join fetch e.association1 a join fetch a.keyValues1 join fetch e.keyValues1 join fetch e.keyValues2 join fetch e.map",
							Element.class
					).getResultList();

					assertResults( result );
				} );
	}

	@Test
	public void testSelect1(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Element> result = entityManager.createQuery(
							"select e from Element e join fetch e.association1 join fetch e.keyValues1 join fetch e.keyValues2 join fetch e.map",
							Element.class
					).getResultList();

					assertResults( result );
				} );
	}

	@Test
	public void testSelect2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Element> result = entityManager.createQuery(
							"select e from Element e join fetch e.association1 join fetch e.map join fetch e.keyValues1 join fetch e.keyValues2",
							Element.class
					).getResultList();

					assertResults( result );
				} );
	}

	private static void assertResults(List<Element> result) {
		for ( Element element : result ) {
			final String id = element.getId();
			assertThat( element.getAssociation1().getName() ).isEqualTo( "assoc_" + id );
			assertThat( element.getAssociation1().getKeyValues1().size() ).isEqualTo( 3 );
			assertThat( element.getKeyValueEmbeddable().getK() ).isEqualTo( "embeddable" );
			assertThat( element.getKeyValueEmbeddable().getV() ).isEqualTo( "_" + id );
			assertThat( element.getKeyValues1().size() ).isEqualTo( 3 );
			assertThat( element.getKeyValues2().size() ).isEqualTo( 2 );
			assertThat( element.getMap().size() ).isEqualTo( 2 );
			assertThat( element.getAssociation1().getKeyValues1() ).containsExactlyInAnyOrder(
					new KeyValue( "key1_1", "_" + id ),
					new KeyValue( "key1_2", "_" + id ),
					new KeyValue( "key1_3", "_" + id )
			);
			assertThat( element.getKeyValues1() ).containsExactlyInAnyOrder(
					new KeyValue( "key1_1", "_" + id ),
					new KeyValue( "key1_2", "_" + id ),
					new KeyValue( "key1_3", "_" + id )
			);
			assertThat( element.getKeyValues2() ).containsExactlyInAnyOrder(
					new KeyValue( "key2_1", "_" + id ),
					new KeyValue( "key2_2", "_" + id )
			);
			assertThat( element.getMap() ).containsExactly(
					new AbstractMap.SimpleEntry<>( "k1", new KeyValue( "k1", "_" + id ) ),
					new AbstractMap.SimpleEntry<>( "k2", new KeyValue( "k2", "_" + id ) )
			);
		}
	}

	@Entity(name = "Element")
	public static class Element {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private Association association1;

		@Embedded
		KeyValue keyValueEmbeddable;

		@ElementCollection
		private Set<KeyValue> keyValues1;

		@ElementCollection
		private Set<KeyValue> keyValues2;

		@ElementCollection
		private Map<String, KeyValue> map;

		protected Element() {
		}

		public Element(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Association getAssociation1() {
			return association1;
		}

		public void setAssociation1(Association association1) {
			this.association1 = association1;
		}

		public KeyValue getKeyValueEmbeddable() {
			return keyValueEmbeddable;
		}

		public void setKeyValueEmbeddable(KeyValue keyValueEmbeddable) {
			this.keyValueEmbeddable = keyValueEmbeddable;
		}

		public Set<KeyValue> getKeyValues1() {
			return keyValues1;
		}

		public void setKeyValues1(Set<KeyValue> keyValues) {
			this.keyValues1 = keyValues;
		}

		public Set<KeyValue> getKeyValues2() {
			return keyValues2;
		}

		public void setKeyValues2(Set<KeyValue> keyValues2) {
			this.keyValues2 = keyValues2;
		}

		public Map<String, KeyValue> getMap() {
			return map;
		}

		public void setMap(Map<String, KeyValue> map) {
			this.map = map;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Element element = (Element) o;
			return Objects.equals( id, element.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}

		@Override
		public String toString() {
			return "Element{" +
					"id=" + id +
					", keyValues=" + keyValues1 +
					'}';
		}
	}

	@Embeddable
	public static class KeyValue {
		private String k;
		private String v;

		public KeyValue() {
		}

		public KeyValue(String k, String v) {
			this.k = k;
			this.v = v;
		}

		public String getK() {
			return k;
		}

		public void setK(String key) {
			this.k = key;
		}

		public String getV() {
			return v;
		}

		public void setV(String value) {
			this.v = value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			KeyValue keyValue = (KeyValue) o;
			return Objects.equals( k, keyValue.k ) && Objects.equals( v, keyValue.v );
		}

		@Override
		public int hashCode() {
			return Objects.hash( k, v );
		}

		@Override
		public String toString() {
			return "KeyValue{" +
					"key='" + k + '\'' +
					", value='" + v + '\'' +
					'}';
		}
	}

	@Entity(name = "Association")
	public static class Association {
		@Id
		private Long id;
		private String name;

		@ElementCollection
		private Set<KeyValue> keyValues1;

		public Association() {
		}

		public Association(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<KeyValue> getKeyValues1() {
			return keyValues1;
		}

		public void setKeyValues1(Set<KeyValue> keyValues1) {
			this.keyValues1 = keyValues1;
		}
	}
}
