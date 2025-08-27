/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = {
				DeleteSharedOneToManyOrphansTest.Item.class,
				DeleteSharedOneToManyOrphansTest.ItemRelation.class
		},
		integrationSettings = { @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "2") }
)
public class DeleteSharedOneToManyOrphansTest {

	/*
	A value of DEFAULT_BATCH_FETCH_SIZE > 1 along with the initialization of the Item#higherItemRelations
	collection causes the issue
	 */

	@BeforeEach
	public void prepareTest(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction(
				entityManager -> {
					final Item item1 = new Item( "first" );
					entityManager.persist( item1 );

					final Item item2 = new Item( "second" );
					entityManager.persist( item2 );

					final ItemRelation rel = new ItemRelation();
					item1.addLowerItemRelations( rel );
					item2.addHigherItemRelations( rel );

					entityManager.persist( rel );
				}
		);
	}

	@AfterEach
	public void cleanupTest(EntityManagerFactoryScope scope) throws Exception {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-11144")
	@FailureExpected( jiraKey = "HHH-11144" )
	public void testInitializingSecondCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Item item = entityManager.createQuery( "select x from Item x where x.code = 'first'", Item.class )
							.getSingleResult();

					Set<ItemRelation> lowerItemRelations = item.getLowerItemRelations();
					Hibernate.initialize( lowerItemRelations );

					Set<ItemRelation> higherItemRelations = item.getHigherItemRelations();
					Hibernate.initialize( higherItemRelations );

					Assertions.assertEquals( 1, lowerItemRelations.size() );

					lowerItemRelations.clear();
				}
		);
		checkLowerItemRelationsAreDeleted(scope);
	}

	private void checkLowerItemRelationsAreDeleted(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Item item = entityManager.createQuery( "select x from Item x where x.code = 'first'", Item.class )
							.getSingleResult();

					Set<ItemRelation> lowerItemRelations = item.getLowerItemRelations();
					Hibernate.initialize( lowerItemRelations );

					Assertions.assertEquals( 0, lowerItemRelations.size(), "The collection should be empty" );
				}
		);
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		@GeneratedValue
		protected Long id;

		@Column
		protected String code;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		protected Set<ItemRelation> lowerItemRelations = new LinkedHashSet<>();

		@OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
		protected Set<ItemRelation> higherItemRelations = new LinkedHashSet<>();

		public Item() {
		}

		public Item(String code) {
			this.code = code;
		}

		public Set<ItemRelation> getLowerItemRelations() {
			return lowerItemRelations;
		}

		public Set<ItemRelation> getHigherItemRelations() {
			return higherItemRelations;
		}

		public void addHigherItemRelations(ItemRelation itemRelation) {
			higherItemRelations.add( itemRelation );
			itemRelation.setChild( this );
		}

		public void addLowerItemRelations(ItemRelation itemRelation) {
			lowerItemRelations.add( itemRelation );
			itemRelation.setParent( this );
		}
	}

	@Entity(name = "ItemRelation")
	public static class ItemRelation {
		@Id
		@GeneratedValue
		protected Long id;

		@ManyToOne(optional = false)
		@JoinColumn(name = "PARENT_ID")
		private Item parent;

		@ManyToOne(optional = false)
		@JoinColumn(name = "CHILD_ID")
		private Item child;

		public Item getParent() {
			return parent;
		}

		public void setParent(Item parent) {
			this.parent = parent;
		}

		public Item getChild() {
			return child;
		}

		public void setChild(Item child) {
			this.child = child;
		}
	}
}
