/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.processing.Exclude;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9979")
@Exclude
@DomainModel(
		annotatedClasses = {
				MergeListPreAndPostPersistTest.Order.class,
				MergeListPreAndPostPersistTest.Item.class
		}
)
@ServiceRegistry(settings = @Setting(name= MappingSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa"))
@SessionFactory
public class MergeListPreAndPostPersistTest {

	@Test
	@JiraKey(value = "HHH-9979")
	public void testAllPropertiesCopied(SessionFactoryScope scope) {
		final Order order = new Order();
		order.id = 1L;
		order.name = "order";
		Item item = new Item();
		item.id = 1L;
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

	@Entity(name = "`Order`")
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

	@Entity(name = "Item")
	static class Item {
		@Id
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
