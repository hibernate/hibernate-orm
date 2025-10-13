/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9979")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class, jiraKey = "HHH-9918")
@DomainModel(
		annotatedClasses = {
				MergeListPreAndPostPersistWithIdentityTest.Order.class,
				MergeListPreAndPostPersistWithIdentityTest.Item.class
		}
)
@SessionFactory
public class MergeListPreAndPostPersistWithIdentityTest {

	@Test
	@JiraKey(value = "HHH-9979")
	@FailureExpected(jiraKey = "HHH-9979")
	public void testAllPropertiesCopied(SessionFactoryScope scope) {
		final Order order = new Order();
		order.id = 1L;
		order.name = "order";
		// Item.id is an identity so don't initialize it.
		Item item = new Item();
		item.name = "item";
		order.items.add( item );

		addEntityListeners( scope, order );

		scope.inTransaction( s -> {
			s.merge( order );
		} );

		scope.inTransaction( s -> {
			s.remove( order );
		} );
	}

	@Entity
	static class Order {
		@Id
		public Long id;

		@Basic(optional = false)
		public String name;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		public List<Item> items = new ArrayList<Item>();

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Order order = (Order) o;

			return name.equals( order.name );

		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	@Entity
	static class Item {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		@Basic(optional = false)
		public String name;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Item item = (Item) o;

			return name.equals( item.name );
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	private void addEntityListeners(SessionFactoryScope scope, final Order order) {

		EventListenerRegistry registry = scope.getSessionFactory().getEventListenerRegistry();
		registry.setListeners(
				EventType.PRE_INSERT,
				new PreInsertEventListener() {
					@Override
					public boolean onPreInsert(PreInsertEvent event) {
						if ( event.getEntity() instanceof Order ) {
							assertEquals( order, event.getEntity() );
							assertEquals( order.items, ((Order) event.getEntity()).items );
						}
						return false;
					}
				}
		);

		registry.setListeners(
				EventType.POST_INSERT,
				new PostInsertEventListener() {
					public void onPostInsert(PostInsertEvent event) {
						if ( event.getEntity() instanceof Order ) {
							assertEquals( order, event.getEntity() );
							assertEquals( order.items, ((Order) event.getEntity()).items );
						}
					}

					public boolean requiresPostCommitHandling(EntityPersister persister) {
						return false;
					}
				}
		);
	}
}
