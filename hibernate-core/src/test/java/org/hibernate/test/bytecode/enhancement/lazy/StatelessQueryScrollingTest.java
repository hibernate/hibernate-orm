/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.query.Query;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RunWith(BytecodeEnhancerRunner.class)
public class StatelessQueryScrollingTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testDynamicFetchScroll() {
		ScrollableResults scrollableResults = null;
		final StatelessSession statelessSession = sessionFactory().openStatelessSession();
		try {
			final Query query = statelessSession.createQuery( "from Task t join fetch t.resource join fetch t.user" );
			scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
			while ( scrollableResults.next() ) {
				Task taskRef = (Task) scrollableResults.get( 0 );
				assertTrue( Hibernate.isInitialized( taskRef ) );
				assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
				assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
				assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
			}
		}
		finally {
			if ( scrollableResults != null ) {
				scrollableResults.close();
			}
			statelessSession.close();
		}
	}

	@Test
	public void testDynamicFetchCollectionScroll() {
		ScrollableResults scrollableResults = null;
		StatelessSession statelessSession = sessionFactory().openStatelessSession();
		statelessSession.beginTransaction();

		try {
			final Query query = statelessSession.createQuery( "select p from Producer p join fetch p.products" );
			if ( getDialect() instanceof DB2Dialect ) {
				/*
					FetchingScrollableResultsImp#next() in order to check if the ResultSet is empty calls ResultSet#isBeforeFirst()
					but the support for ResultSet#isBeforeFirst() is optional for ResultSets with a result
					set type of TYPE_FORWARD_ONLY and db2 does not support it.
			 	*/
				scrollableResults = query.scroll( ScrollMode.SCROLL_INSENSITIVE );
			}
			else {
				scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
			}
			while ( scrollableResults.next() ) {
				Producer producer = (Producer) scrollableResults.get( 0 );
				assertTrue( Hibernate.isInitialized( producer ) );
				assertTrue( Hibernate.isPropertyInitialized( producer, "products" ) );
				assertTrue( Hibernate.isInitialized( producer.getProducts() ) );

				for ( Product product : producer.getProducts() ) {
					assertTrue( Hibernate.isInitialized( product ) );
					assertFalse( Hibernate.isInitialized( product.getVendor() ) );
				}
			}
		}
		finally {
			if ( scrollableResults != null ) {
				scrollableResults.close();
			}
			statelessSession.getTransaction().commit();
			statelessSession.close();
		}
	}


	@Before
	public void createTestData() {
		inTransaction(
				session -> {
					Date now = new Date();
					User me = new User( "me" );
					User you = new User( "you" );
					Resource yourClock = new Resource( "clock", you );
					Task task = new Task( me, "clean", yourClock, now ); // :)

					session.save( me );
					session.save( you );
					session.save( yourClock );
					session.save( task );

					User u3 = new User( "U3" );
					User u4 = new User( "U4" );
					Resource it = new Resource( "it", u4 );
					Task task2 = new Task( u3, "beat", it, now ); // :))

					session.save( u3 );
					session.save( u4 );
					session.save( it );
					session.save( task2 );
				}
		);

		inTransaction(
				session -> {
					Producer p1 = new Producer( 1, "Acme" );
					Producer p2 = new Producer( 2, "ABC" );

					session.save( p1 );
					session.save( p2 );

					Vendor v1 = new Vendor( 1, "v1" );
					Vendor v2 = new Vendor( 2, "v2" );

					session.save( v1 );
					session.save( v2 );

					final Product product1 = new Product( 1, "123", v1, p1 );
					final Product product2 = new Product( 2, "456", v1, p1 );
					final Product product3 = new Product( 3, "789", v1, p2 );

					session.save( product1 );
					session.save( product2 );
					session.save( product3 );
				}
		);
	}

	@After
	public void deleteTestData() {
		inTransaction(
				s -> {
					s.createQuery( "delete Task" ).executeUpdate();
					s.createQuery( "delete Resource" ).executeUpdate();
					s.createQuery( "delete User" ).executeUpdate();

					s.createQuery( "delete Product" ).executeUpdate();
					s.createQuery( "delete Producer" ).executeUpdate();
					s.createQuery( "delete Vendor" ).executeUpdate();
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Task.class );
		sources.addAnnotatedClass( User.class );
		sources.addAnnotatedClass( Resource.class );
		sources.addAnnotatedClass( Product.class );
		sources.addAnnotatedClass( Producer.class );
		sources.addAnnotatedClass( Vendor.class );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection fetch scrolling

	@Entity(name = "Producer")
	public static class Producer {
		@Id
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "producer", fetch = FetchType.LAZY)
		private Set<Product> products = new HashSet<>();

		public Producer() {
		}

		public Producer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Product> getProducts() {
			return products;
		}

		public void setProducts(Set<Product> products) {
			this.products = products;
		}
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		private Integer id;
		private String sku;

		@ManyToOne(fetch = FetchType.LAZY)
		private Vendor vendor;

		@ManyToOne(fetch = FetchType.LAZY)
		private Producer producer;

		public Product() {
		}

		public Product(Integer id, String sku, Vendor vendor, Producer producer) {
			this.id = id;
			this.sku = sku;
			this.vendor = vendor;
			this.producer = producer;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public Vendor getVendor() {
			return vendor;
		}

		public void setVendor(Vendor vendor) {
			this.vendor = vendor;
		}

		public Producer getProducer() {
			return producer;
		}

		public void setProducer(Producer producer) {
			this.producer = producer;
		}
	}

	@Entity(name = "Vendor")
	public static class Vendor {
		@Id
		private Integer id;
		private String name;

		@OneToMany(mappedBy = "vendor", fetch = FetchType.LAZY)
		private Set<Product> products = new HashSet<>();

		public Vendor() {
		}

		public Vendor(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Product> getProducts() {
			return products;
		}

		public void setProducts(Set<Product> products) {
			this.products = products;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity fetch scrolling

	@Entity(name = "Resource")
	@Table(name = "resources")
	public static class Resource {
		@Id
		@GeneratedValue(generator = "increment")
		private Long id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private User owner;

		public Resource() {
		}

		public Resource(String name, User owner) {
			this.name = name;
			this.owner = owner;
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

		public User getOwner() {
			return owner;
		}

		public void setOwner(User owner) {
			this.owner = owner;
		}
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {
		@Id
		@GeneratedValue(generator = "increment")
		private Long id;
		private String name;

		public User() {
		}

		public User(String name) {
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
	}

	@Entity(name = "Task")
	public static class Task {
		@Id
		@GeneratedValue(generator = "increment")
		private Long id;
		private String description;
		@ManyToOne(fetch = FetchType.LAZY)
		private User user;
		@ManyToOne(fetch = FetchType.LAZY)
		private Resource resource;
		private Date dueDate;
		private Date startDate;
		private Date completionDate;

		public Task() {
		}

		public Task(User user, String description, Resource resource, Date dueDate) {
			this( user, description, resource, dueDate, null, null );
		}

		public Task(
				User user,
				String description,
				Resource resource,
				Date dueDate,
				Date startDate,
				Date completionDate) {
			this.user = user;
			this.resource = resource;
			this.description = description;
			this.dueDate = dueDate;
			this.startDate = startDate;
			this.completionDate = completionDate;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public Resource getResource() {
			return resource;
		}

		public void setResource(Resource resource) {
			this.resource = resource;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Date getDueDate() {
			return dueDate;
		}

		public void setDueDate(Date dueDate) {
			this.dueDate = dueDate;
		}

		public Date getStartDate() {
			return startDate;
		}

		public void setStartDate(Date startDate) {
			this.startDate = startDate;
		}

		public Date getCompletionDate() {
			return completionDate;
		}

		public void setCompletionDate(Date completionDate) {
			this.completionDate = completionDate;
		}
	}

}
