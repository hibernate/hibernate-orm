/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				StatelessQueryScrollingTest.Task.class,
				StatelessQueryScrollingTest.User.class,
				StatelessQueryScrollingTest.Resource.class,
				StatelessQueryScrollingTest.Product.class,
				StatelessQueryScrollingTest.Producer.class,
				StatelessQueryScrollingTest.Vendor.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class StatelessQueryScrollingTest {

	@Test
	public void testDynamicFetchScMapsIdProxyBidirectionalTestroll(SessionFactoryScope scope) {
		ScrollableResults scrollableResults = null;
		final StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession();
		try {
			final Query query = statelessSession.createQuery( "from Task t join fetch t.resource join fetch t.user" );
			scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
			while ( scrollableResults.next() ) {
				Task taskRef = (Task) scrollableResults.get();
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
	public void testDynamicFetchCollectionScroll(SessionFactoryScope scope) {
		ScrollableResults scrollableResults = null;
		StatelessSession statelessSession = scope.getSessionFactory().openStatelessSession();
		statelessSession.beginTransaction();

		try {
			final Query query = statelessSession.createQuery( "select p from Producer p join fetch p.products" );
			scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
			while ( scrollableResults.next() ) {
				Producer producer = (Producer) scrollableResults.get();
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


	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
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

		scope.inTransaction(
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

	@AfterEach
	public void deleteTestData(SessionFactoryScope scope) {
		scope.inTransaction(
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
