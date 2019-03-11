/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-13221" )
public class LazyLoadNoTransReadOnlyTransactionTest extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( false, true );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				Item.class,
				CustomerDetails.class,
				Tag.class
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void hibernateInitialize() throws SQLException {
		doInHibernate( this::sessionFactory, session -> {
			session.persist(
					new Customer()
							.setId( 1 )
							.setName( "Acme" )
							.addItem( new Item( 1 ) )
							.addItem( new Item( 2 ) )
							.addDetails( new CustomerDetails() )
							.addTag( new Tag().setName( "Tag1" ) )
							.addTag( new Tag().setName( "Tag2" ) )
			);
		} );

		Item firstItem = doInHibernate( this::sessionFactory, session -> {
			return session.find( Item.class, 1 );
		} );

		connectionProvider.clear();

		assertFalse( Hibernate.isInitialized( firstItem.getCustomer() ) );

		assertEquals( "Acme", firstItem.getCustomer().getName() );

		assertTrue( Hibernate.isInitialized( firstItem.getCustomer() ) );
		assertEquals( 1, connectionProvider.getPreparedSQLStatements().size() );

	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
		private List<Item> boughtItems = new ArrayList<>();

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@Fetch(FetchMode.SELECT)
		private List<Tag> tags = new ArrayList<>();

		@OneToOne(mappedBy = "customer")
		private CustomerDetails details;

		public Integer getId() {
			return id;
		}

		public Customer setId(Integer id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Customer setName(String name) {
			this.name = name;
			return this;
		}

		public Customer addItem(Item item) {
			item.customer = this;
			boughtItems.add( item );
			return this;
		}

		public Customer addTag(Tag tag) {
			tags.add( tag );
			return this;
		}

		public Customer addDetails(CustomerDetails details) {
			this.details = details;
			this.details.customer = this;

			return this;
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Customer customer;

		protected Item() {
		}

		public Item(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public Customer getCustomer() {
			return customer;
		}
	}

	@Entity(name = "CustomerDetails")
	public static class CustomerDetails {
		@Id
		private Integer id;

		@OneToOne
		@MapsId
		private Customer customer;

		public Integer getId() {
			return id;
		}

		public Customer getCustomer() {
			return customer;
		}
	}

	@Entity(name = "Tag")
	public static class Tag {
		@Id
		private String name;

		public String getName() {
			return name;
		}

		public Tag setName(String name) {
			this.name = name;
			return this;
		}
	}
}
