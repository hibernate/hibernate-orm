/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLSelect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Guillaume Toison
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ResultMappingAssociationIdClassTest.ItemEntity.class,
		ResultMappingAssociationIdClassTest.ItemOrder.class,
		ResultMappingAssociationIdClassTest.AccountEntity.class,
		ResultMappingAssociationIdClassTest.ItemInventory.class,
		ResultMappingAssociationIdClassTest.ItemInventoryPK.class,
		ResultMappingAssociationIdClassTest.ItemInventoryIdClass.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17308" )
public class ResultMappingAssociationIdClassTest {
	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ItemEntity item = new ItemEntity( "cheese" );
			session.persist( item );
			final AccountEntity account = new AccountEntity( "account_1" );
			session.persist( account );
			session.persist( new ItemOrder( account, item, 100 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ItemOrder" ).executeUpdate();
			session.createMutationQuery( "delete from ItemEntity" ).executeUpdate();
			session.createMutationQuery( "delete from AccountEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testResultSetMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AccountEntity account = session.getReference( AccountEntity.class, 1L );
			assertThat( account.getInventories() ).hasSize( 1 );
			for ( ItemInventory inventory : account.getInventories() ) {
				assertThat( inventory.getAccount() ).isEqualTo( account );
			}
		} );
	}

	@Test
	public void testResultSetMappingIdClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AccountEntity account = session.getReference( AccountEntity.class, 1L );
			assertThat( account.getIdClassInventories() ).hasSize( 1 );
			for ( ItemInventoryIdClass inventory : account.getIdClassInventories() ) {
				assertThat( inventory.getAccount() ).isEqualTo( account );
			}
		} );
	}

	@Entity( name = "ItemEntity" )
	public static class ItemEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public ItemEntity() {
		}

		public ItemEntity(String name) {
			this.name = name;
		}
	}

	@Entity( name = "ItemOrder" )
	@Table( name = "item_orders" )
	public static class ItemOrder {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "account_id" )
		private AccountEntity account;

		@ManyToOne
		@JoinColumn( name = "item_id" )
		private ItemEntity item;

		private int quantity;

		public ItemOrder() {
		}

		public ItemOrder(AccountEntity account, ItemEntity item, int quantity) {
			this.account = account;
			this.item = item;
			this.quantity = quantity;
		}
	}

	@Entity( name = "ItemInventory" )
	public static class ItemInventory {
		@Id
		@ManyToOne
		@JoinColumn( name = "account_id" )
		private AccountEntity account;

		@Id
		@ManyToOne
		@JoinColumn( name = "item_id" )
		private ItemEntity item;

		private int quantity;

		public ItemInventory() {
		}

		public ItemInventory(AccountEntity account, ItemEntity item, int quantity) {
			this.account = account;
			this.item = item;
			this.quantity = quantity;
		}

		public AccountEntity getAccount() {
			return account;
		}
	}

	@Embeddable
	public static class ItemInventoryPK implements Serializable {
		@ManyToOne
		@JoinColumn( name = "account_id" )
		private AccountEntity account;

		@ManyToOne
		@JoinColumn( name = "item_id" )
		private ItemEntity item;
	}

	@Entity( name = "ItemInventoryIdClass" )
	@IdClass( ItemInventoryPK.class )
	public static class ItemInventoryIdClass {
		@Id
		@ManyToOne
		@JoinColumn( name = "account_id" )
		private AccountEntity account;

		@Id
		@ManyToOne
		@JoinColumn( name = "item_id" )
		private ItemEntity item;

		private int quantity;

		public ItemInventoryIdClass() {
		}

		public ItemInventoryIdClass(AccountEntity account, ItemEntity item, int quantity) {
			this.account = account;
			this.item = item;
			this.quantity = quantity;
		}

		public AccountEntity getAccount() {
			return account;
		}
	}

	@Entity( name = "AccountEntity" )
	public static class AccountEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public AccountEntity() {
		}

		public AccountEntity(String name) {
			this.name = name;
			this.inventories = new HashSet<>();
		}

		@OneToMany( mappedBy = "account" )
		@SQLSelect( sql = "select account_id, item_id, sum(quantity) as quantity from item_orders" +
				" where account_id = :code group by account_id, item_id",
				resultSetMapping = @SqlResultSetMapping( name = "inventories", entities = {
						@EntityResult( entityClass = ItemInventory.class, fields = {
								@FieldResult( name = "account", column = "account_id" ),
								@FieldResult( name = "item", column = "item_id" ),
								@FieldResult( name = "quantity", column = "quantity" )
						} )
				} ) )
		private Set<ItemInventory> inventories;

		@OneToMany( mappedBy = "account" )
		@SQLSelect( sql = "select account_id, item_id, sum(quantity) as quantity from item_orders" +
				" where account_id = :code group by account_id, item_id",
				resultSetMapping = @SqlResultSetMapping( name = "inventories", entities = {
						@EntityResult( entityClass = ItemInventoryIdClass.class, fields = {
								@FieldResult( name = "account", column = "account_id" ),
								@FieldResult( name = "item", column = "item_id" ),
								@FieldResult( name = "quantity", column = "quantity" )
						} )
				} ) )
		private Set<ItemInventoryIdClass> idClassInventories;

		public Set<ItemInventory> getInventories() {
			return inventories;
		}

		public Set<ItemInventoryIdClass> getIdClassInventories() {
			return idClassInventories;
		}
	}
}
