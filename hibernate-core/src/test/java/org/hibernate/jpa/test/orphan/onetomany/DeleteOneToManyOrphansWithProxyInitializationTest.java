/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.orphan.onetomany;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class DeleteOneToManyOrphansWithProxyInitializationTest extends BaseEntityManagerFunctionalTestCase {

	private void createData() {
		doInJPA( this::entityManagerFactory, em -> {
			Item item1 = new Item();
			item1.setCode( "first" );
			em.persist( item1 );

			Item item2 = new Item();
			item2.setCode( "second" );
			em.persist( item2 );

			ItemRelation rel = new ItemRelation();
			rel.setParent( item1 );
			rel.setChild( item2 );
			item1.getLowerItemRelations().add( rel );
			item2.getHigherItemRelations().add( rel );
			em.persist( rel );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11144")
	public void testOrphanedWhileManaged() {
		createData();

		doInJPA( this::entityManagerFactory, em -> {
			Item item = em.createQuery("select x from Item x where x.code = 'first'", Item.class).getSingleResult();

			Set<ItemRelation> lowerItemRelations = item.getLowerItemRelations();
			Hibernate.initialize( lowerItemRelations);

			Set<ItemRelation> higherItemRelations = item.getHigherItemRelations();
			Hibernate.initialize(higherItemRelations);

			Assert.assertEquals( 1, lowerItemRelations.size());

			lowerItemRelations.clear();
		} );

		doInJPA( this::entityManagerFactory, em -> {
			Item item = em.createQuery("select x from Item x where x.code = 'first'", Item.class).getSingleResult();

			Set<ItemRelation> lowerItemRelations = item.getLowerItemRelations();
			Assert.assertEquals( 0, lowerItemRelations.size());
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				ItemRelation.class
		};
	}

	@Entity(name = "Item")
	@Table(name = "ITEM", indexes = @Index(columnList = "CODE", unique = true))
	public static class Item implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		@GeneratedValue
		protected Long id;

		@Column
		protected String code;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		protected Set<ItemRelation> lowerItemRelations = new LinkedHashSet<>();

		@OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
		protected Set<ItemRelation> higherItemRelations = new LinkedHashSet<>();

		@Override
		public int hashCode() {
			return Objects.hash( code );
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}

			if ( obj == null || !( obj instanceof Item ) ) {
				return false;
			}

			Item other = (Item) obj;

			return Objects.equals( code, other.getCode() );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + ": id=[" + id + "] code=[" + code + "]";
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public Set<ItemRelation> getLowerItemRelations() {
			return lowerItemRelations;
		}

		public void setLowerItemRelations(Set<ItemRelation> lowerItemRelations) {
			this.lowerItemRelations = lowerItemRelations;
		}

		public Set<ItemRelation> getHigherItemRelations() {
			return higherItemRelations;
		}

		public void setHigherItemRelations(Set<ItemRelation> higherItemRelations) {
			this.higherItemRelations = higherItemRelations;
		}
	}

	@Entity(name = "ItemRelation")
	@Table(name = "ITEM_RELATION",
			indexes = @Index(columnList = "PARENT_ID, CHILD_ID", unique = true))
	public static class ItemRelation implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		@GeneratedValue
		protected Long id;

		@ManyToOne(optional = false)
		@JoinColumn(name = "PARENT_ID")
		private Item parent;

		@ManyToOne(optional = false)
		@JoinColumn(name = "CHILD_ID")
		private Item child;

		@Column(nullable = false, columnDefinition = "INT DEFAULT 0 NOT NULL")
		private int quantity = 1;

		@Override
		public int hashCode() {
			return Objects.hash( parent, child );
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}

			if ( obj == null || !( obj instanceof ItemRelation ) ) {
				return false;
			}

			ItemRelation other = (ItemRelation) obj;

			return Objects.equals( parent, other.getParent() ) && Objects.equals( child, other.getChild() );
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + ": id=[" + id + "] parent=[" + parent + "] child=[" + child + "]";
		}

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

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
	}
}
