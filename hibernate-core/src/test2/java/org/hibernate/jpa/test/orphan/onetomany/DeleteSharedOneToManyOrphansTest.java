/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.orphan.onetomany;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Andrea Boriero
 */
public class DeleteSharedOneToManyOrphansTest extends BaseEntityManagerFunctionalTestCase {

	/*
	 A value of BATCH_FETCH_SIZE > 1 along with the initialization of the Item#higherItemRelations
	 collection causes the issue
	 */
	private static final String BATCH_FETCH_SIZE = "2";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Item.class, ItemRelation.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( Environment.DEFAULT_BATCH_FETCH_SIZE, BATCH_FETCH_SIZE );
	}

	@Before
	public void prepareTest() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {

			final Item item1 = new Item( "first" );
			entityManager.persist( item1 );

			final Item item2 = new Item( "second" );
			entityManager.persist( item2 );

			final ItemRelation rel = new ItemRelation();
			item1.addLowerItemRelations( rel );
			item2.addHigherItemRelations( rel );

			entityManager.persist( rel );
		} );
	}

	@After
	public void cleanupTest() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete from ItemRelation" ).executeUpdate();
			entityManager.createQuery( "delete from Item" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11144")
	@FailureExpected( jiraKey = "HHH-11144" )
	public void testInitializingSecondCollection() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			Item item = entityManager.createQuery( "select x from Item x where x.code = 'first'", Item.class )
					.getSingleResult();

			Set<ItemRelation> lowerItemRelations = item.getLowerItemRelations();
			Hibernate.initialize( lowerItemRelations );

			Set<ItemRelation> higherItemRelations = item.getHigherItemRelations();
			Hibernate.initialize( higherItemRelations );

			Assert.assertEquals( 1, lowerItemRelations.size() );

			lowerItemRelations.clear();
		} );
		checkLowerItemRelationsAreDeleted();
	}

	private void checkLowerItemRelationsAreDeleted() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			Item item = entityManager.createQuery( "select x from Item x where x.code = 'first'", Item.class )
					.getSingleResult();

			Set<ItemRelation> lowerItemRelations = item.getLowerItemRelations();
			Hibernate.initialize( lowerItemRelations );

			Assert.assertEquals( "The collection should be empty", 0, lowerItemRelations.size() );
		} );
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
