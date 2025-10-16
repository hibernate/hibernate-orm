/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test the use of the {@link OrderBy} annotation on a map-based element-collection
 * that uses entities for the key and value.
 * <p>
 * This mapping and association invokes the use of the ThreeEntityQueryGenerator which
 * we want to verify orders the collection results properly.
 * <p>
 * It's worth noting that a mapping like this orders the collection based on the value
 * and not the key.
 *
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		OrderByThreeEntityTest.Container.class,
		OrderByThreeEntityTest.Key.class,
		OrderByThreeEntityTest.Item.class
})
@EnversTest
@JiraKey(value = "HHH-12992")
public class OrderByThreeEntityTest {
	@Entity(name = "Container")
	@Audited
	public static class Container {
		@Id
		@GeneratedValue
		private Integer id;
		@ElementCollection
		@OrderBy("value desc")
		private Map<Key, Item> data = new HashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Map<Key, Item> getData() {
			return data;
		}

		public void setData(Map<Key, Item> data) {
			this.data = data;
		}
	}

	@Entity(name = "MapKey")
	@Audited
	public static class Key {
		@Id
		private Integer id;
		@Column(name = "val")
		private String value;

		public Key() {

		}

		public Key(Integer id, String value) {
			this.id = id;
			this.value = value;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
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
			Key key = (Key) o;
			return Objects.equals( id, key.id ) &&
				Objects.equals( value, key.value );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, value );
		}

		@Override
		public String toString() {
			return "Key{" +
				"id=" + id +
				", value='" + value + '\'' +
				'}';
		}
	}

	@Entity(name = "Item")
	@Audited
	public static class Item {
		@Id
		private Integer id;
		@Column(name = "val")
		private String value;

		public Item() {

		}

		public Item(Integer id, String value) {
			this.id = id;
			this.value = value;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
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
			Item item = (Item) o;
			return Objects.equals( id, item.id ) &&
				Objects.equals( value, item.value );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, value );
		}

		@Override
		public String toString() {
			return "Item{" +
				"id=" + id +
				", value='" + value + '\'' +
				'}';
		}
	}

	private Integer containerId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Rev 1
		this.containerId = scope.fromTransaction( entityManager -> {
			final Container container = new Container();

			final Key key1 = new Key( 1, "A" );
			final Key key2 = new Key( 2, "B" );

			final Item item1 = new Item( 1, "I1" );
			final Item item2 = new Item( 2, "I2" );

			entityManager.persist( item1 );
			entityManager.persist( item2 );
			entityManager.persist( key1 );
			entityManager.persist( key2 );

			container.getData().put( key1, item2 );
			container.getData().put( key2, item1 );
			entityManager.persist( container );

			return container.getId();
		} );

		// Rev 2
		scope.inTransaction( entityManager -> {
			final Container container = entityManager.find( Container.class, containerId );

			final Key key = new Key( 3, "C" );
			final Item item = new Item( 3, "I3" );

			entityManager.persist( key );
			entityManager.persist( item );

			container.getData().put( key, item );
			entityManager.merge( container );
		} );

		// Rev 3
		scope.inTransaction( entityManager -> {
			final Container container = entityManager.find( Container.class, containerId );
			container.getData().keySet().forEach(
					key -> {
						if ( "B".equals( key.getValue() ) ) {
							final Item item = container.getData().get( key );
							container.getData().remove( key );
							entityManager.remove( key );
							entityManager.remove( item );
						}
					}
			);
			entityManager.merge( container );
		} );

		// Rev 4
		scope.inTransaction( entityManager -> {
			final Container container = entityManager.find( Container.class, containerId );
			container.getData().entrySet().forEach(
					entry -> {
						entityManager.remove( entry.getKey() );
						entityManager.remove( entry.getValue() );
					}
			);
			container.getData().clear();
			entityManager.merge( container );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2, 3, 4 ),
					AuditReaderFactory.get( em ).getRevisions( Container.class, this.containerId ) );

			assertEquals( Arrays.asList( 1, 4 ), AuditReaderFactory.get( em ).getRevisions( Key.class, 1 ) );
			assertEquals( Arrays.asList( 1, 3 ), AuditReaderFactory.get( em ).getRevisions( Key.class, 2 ) );
			assertEquals( Arrays.asList( 2, 4 ), AuditReaderFactory.get( em ).getRevisions( Key.class, 3 ) );

			assertEquals( Arrays.asList( 1, 3 ), AuditReaderFactory.get( em ).getRevisions( Item.class, 1 ) );
			assertEquals( Arrays.asList( 1, 4 ), AuditReaderFactory.get( em ).getRevisions( Item.class, 2 ) );
			assertEquals( Arrays.asList( 2, 4 ), AuditReaderFactory.get( em ).getRevisions( Item.class, 3 ) );
		} );
	}

	@Test
	public void testRevision1History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Container container = AuditReaderFactory.get( em ).find( Container.class, this.containerId, 1 );

			assertNotNull( container );
			assertFalse( container.getData().isEmpty() );
			assertEquals( 2, container.getData().size() );

			final Iterator<Map.Entry<Key, Item>> iterator = container.getData().entrySet().iterator();

			final Map.Entry<Key, Item> first = iterator.next();
			assertEquals( new Key( 1, "A" ), first.getKey() );
			assertEquals( new Item( 2, "I2" ), first.getValue() );

			final Map.Entry<Key, Item> second = iterator.next();
			assertEquals( new Key( 2, "B" ), second.getKey() );
			assertEquals( new Item( 1, "I1" ), second.getValue() );
		} );
	}
}
