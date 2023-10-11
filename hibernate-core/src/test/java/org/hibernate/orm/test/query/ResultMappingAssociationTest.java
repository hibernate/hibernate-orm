/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLSelect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;


/**
 * @author Guillaume Toison
 */
@SessionFactory
@DomainModel(
	annotatedClasses = {
		ResultMappingAssociationTest.Item.class,
		ResultMappingAssociationTest.ItemOrder.class,
		ResultMappingAssociationTest.Account.class,
	},
	xmlMappings = "org/hibernate/orm/test/query/ResultMappingAssociationTest.hbm.xml"
)
@Jira("HHH-17308")
public class ResultMappingAssociationTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Item item = new Item("Cheese");
					session.persist( item );
					
					Account account = new Account("Account #1");
					session.persist( account );
					
					ItemOrder order = new ItemOrder(account, item, 100);
					session.persist( order );
				} );
	}
	
	@Test
	public void loadAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Account account = session.getReference(Account.class, 1L);

					assertEquals( account.getInventories().size(), 1 );
					
					for (ItemInventory inventory : account.getInventories()) {
						assertEquals( inventory.account, account );
					}
				} );
	}

	@Entity
	public static class Item {
		@Id
		@GeneratedValue
		Long id;

		String name;

		public Item() {
		}

		public Item(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "item_orders")
	public static class ItemOrder {
		@Id
		@GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "account_id")
		Account account;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "item_id")
		Item item;

		int quantity;

		public ItemOrder() {
		}

		public ItemOrder(Account account, Item item, int quantity) {
			this.account = account;
			this.item = item;
			this.quantity = quantity;
		}
	}

	// Mapped in ResultMappingAssociationTest.hbm.xml
	public static class ItemInventory {
		Account account;

		Item item;

		int quantity;

		public ItemInventory() {
		}

		public ItemInventory(Account account, Item item, int quantity) {
			this.account = account;
			this.item = item;
			this.quantity = quantity;
		}

		public Account getAccount() {
			return account;
		}

		public void setAccount(Account account) {
			this.account = account;
		}

		public Item getItem() {
			return item;
		}

		public void setItem(Item item) {
			this.item = item;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
	}

	// Mapped in ResultMappingAssociationTest.hbm.xml
	public static class ItemInventoryKey {
		Account account;
		
		Item item;

		public Account getAccount() {
			return account;
		}

		public void setAccount(Account account) {
			this.account = account;
		}

		public Item getItem() {
			return item;
		}

		public void setItem(Item item) {
			this.item = item;
		}
	}

	@Entity
	@SqlResultSetMapping(name = "inventories", entities = {
			@EntityResult(entityClass = ItemInventory.class, fields = {
					@FieldResult(name = "account", column = "account_id"),
					@FieldResult(name = "item", column = "item_id"),
					@FieldResult(name = "quantity", column = "quantity") }) })
	public static class Account {
		@Id
		@GeneratedValue
		Long id;

		String name;

		public Account() {
		}

		public Account(String name) {
			this.name = name;
			this.inventories = new HashSet<>();
		}

		@OneToMany(targetEntity = ItemInventory.class)
		@SQLSelect(sql = "SELECT account_id, item_id, sum(quantity) AS quantity	FROM item_orders WHERE account_id = :code GROUP BY account_id, item_id HAVING quantity > 0.000001 or quantity < -0.000001", 
			resultSetMapping = @SqlResultSetMapping(name = "inventories", entities = {
				@EntityResult(entityClass = ItemInventory.class, fields = {
						@FieldResult(name = "account", column = "account_id"),
						@FieldResult(name = "item", column = "item_id"),
						@FieldResult(name = "quantity", column = "quantity") }) }))
		Set<ItemInventory> inventories;
		public Set<ItemInventory> getInventories() {
			return inventories;
		}

		public void setInventories(Set<ItemInventory> inventories) {
			this.inventories = inventories;
		}
	}
}
