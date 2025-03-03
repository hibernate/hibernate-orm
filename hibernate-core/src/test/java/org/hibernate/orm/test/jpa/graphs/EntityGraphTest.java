/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.Hibernate;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Bauer
 * @author Brett Meyer
 */
public class EntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class, Bar.class, Baz.class, Author.class, Book.class, Prize.class, Company.class,
				Employee.class, Manager.class, Location.class, AnimalOwner.class, Animal.class, Dog.class, Cat.class,
				Kennel.class
		};
	}

	@Test
	@JiraKey(value = "HHH-8857")
	public void loadMultipleAssociations() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Bar bar = new Bar();
		em.persist( bar );

		Baz baz = new Baz();
		em.persist( baz );

		Foo foo = new Foo();
		foo.bar = bar;
		foo.baz = baz;
		em.persist( foo );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
		fooGraph.addAttributeNodes( "bar", "baz" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "javax.persistence.loadgraph", fooGraph );

		Foo result = em.find( Foo.class, foo.id, properties );

		assertTrue( Hibernate.isInitialized( result ) );
		assertTrue( Hibernate.isInitialized( result.bar ) );
		assertTrue( Hibernate.isInitialized( result.baz ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void loadCollection() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Bar bar = new Bar();
		em.persist( bar );

		Foo foo = new Foo();
		foo.bar = bar;
		bar.foos.add(foo);
		em.persist( foo );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		EntityGraph<Bar> barGraph = em.createEntityGraph( Bar.class );
		barGraph.addAttributeNodes("foos");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "javax.persistence.loadgraph", barGraph);

		Bar result = em.find( Bar.class, bar.id, properties );

		assertTrue( Hibernate.isInitialized( result ) );
		assertTrue( Hibernate.isInitialized( result.foos ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void loadInverseCollection() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Bar bar = new Bar();
		em.persist( bar );
		Baz baz = new Baz();
		em.persist( baz );

		Foo foo = new Foo();
		foo.bar = bar;
		foo.baz = baz;
		bar.foos.add(foo);
		baz.foos.add(foo);
		em.persist( foo );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
		fooGraph.addAttributeNodes("bar");
		fooGraph.addAttributeNodes("baz");
		Subgraph<Bar> barGraph = fooGraph.addSubgraph("bar", Bar.class);
		barGraph.addAttributeNodes("foos");

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "javax.persistence.loadgraph", fooGraph );

		Foo result = em.find( Foo.class, foo.id, properties );

		assertTrue( Hibernate.isInitialized( result ) );
		assertTrue( Hibernate.isInitialized( result.bar ) );
		assertTrue( Hibernate.isInitialized( result.bar.getFoos()) );
		assertTrue( Hibernate.isInitialized( result.baz ) );
		// sanity check -- ensure the only bi-directional fetch was the one identified by the graph
		assertFalse( Hibernate.isInitialized( result.baz.getFoos()) );

		em.getTransaction().commit();
		em.close();
	}

	/**
	 * JPA 2.1 spec: "Add a node to the graph that corresponds to a managed type with inheritance. This allows for
	 * multiple subclass subgraphs to be defined for this node of the entity graph. Subclass subgraphs will
	 * automatically include the specified attributes of superclass subgraphs."
	 */
	@Test
	@JiraKey(value = "HHH-8640")
	public void inheritanceTest() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Manager manager = new Manager();
		em.persist( manager );
		Employee employee = new Employee();
		employee.friends.add( manager );
		employee.managers.add( manager );
		em.persist( employee );
		Company company = new Company();
		company.employees.add( employee );
		company.employees.add( manager );
		em.persist( company );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		EntityGraph<Company> entityGraph = em.createEntityGraph( Company.class );
		Subgraph<Employee> subgraph = entityGraph.addSubgraph( "employees" );
		subgraph.addAttributeNodes( "managers" );
		subgraph.addAttributeNodes( "friends" );
		Subgraph<Manager> subSubgraph = subgraph.addSubgraph( "managers", Manager.class );
		subSubgraph.addAttributeNodes( "managers" );
		subSubgraph.addAttributeNodes( "friends" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "javax.persistence.loadgraph", entityGraph );

		Company result = em.find( Company.class, company.id, properties );

		assertTrue( Hibernate.isInitialized( result ) );
		assertTrue( Hibernate.isInitialized( result.employees ) );
		assertEquals( 2, result.employees.size() );
		for (Employee resultEmployee : result.employees) {
			assertTrue( Hibernate.isInitialized( resultEmployee.managers ) );
			assertTrue( Hibernate.isInitialized( resultEmployee.friends ) );
		}

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-9080")
	public void attributeNodeInheritanceTest() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Manager manager = new Manager();
		em.persist( manager );
		Employee employee = new Employee();
		manager.friends.add( employee);
		em.persist( employee );
		Manager anotherManager = new Manager();
		manager.managers.add(anotherManager);
		em.persist( anotherManager );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		EntityGraph<Manager> entityGraph = em.createEntityGraph( Manager.class );
		entityGraph.addAttributeNodes( "friends" );
		entityGraph.addAttributeNodes( "managers" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "javax.persistence.loadgraph", entityGraph );

		Manager result = em.find( Manager.class, manager.id, properties );

		assertTrue( Hibernate.isInitialized( result ) );
		assertTrue( Hibernate.isInitialized( result.friends ) );
		assertEquals( 1, result.friends.size() );
		assertTrue( Hibernate.isInitialized( result.managers) );
		assertEquals( 1, result.managers.size() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-9735")
	public void loadIsMemberQueriedCollection() {

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Bar bar = new Bar();
		em.persist( bar );

		Foo foo = new Foo();
		foo.bar = bar;
		bar.foos.add(foo);
		em.persist( foo );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		foo = em.find(Foo.class, foo.id);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Bar> cq = cb.createQuery(Bar.class);
		Root<Bar> from = cq.from(Bar.class);

		Expression<Set<Foo>> foos = from.get("foos");

		cq.where(cb.isMember(foo, foos));

		TypedQuery<Bar> query = em.createQuery(cq);

		EntityGraph<Bar> barGraph = em.createEntityGraph( Bar.class );
		barGraph.addAttributeNodes("foos");
		query.setHint("javax.persistence.loadgraph", barGraph);

		Bar result = query.getSingleResult();

		assertTrue( Hibernate.isInitialized( result ) );
		assertTrue( Hibernate.isInitialized( result.foos ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-15859")
	public void mapAttributeTest() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Author author = new Author();
		em.persist(author);

		Book book = new Book();
		author.books.put(1, book);
		em.persist(author);
		em.persist(book);

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		EntityGraph<Author> entityGraph = em.createEntityGraph(Author.class);
		entityGraph.addAttributeNodes("books");
		Map<String, Object> properties = new HashMap<>();
		properties.put("javax.persistence.loadgraph", entityGraph);

		Author result = em.find(Author.class, author.id, properties);
		assertTrue(Hibernate.isInitialized(result));
		assertTrue(Hibernate.isInitialized(result.books));

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-15964")
	public void paginationOverCollectionFetch() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		String authorName = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
		Set<Integer> authorIds = IntStream.range(0, 3)
				.mapToObj(v -> {
					Author author = new Author(authorName);
					em.persist(author);
					em.persist(new Book(author));
					em.persist(new Book(author));
					return author;
				})
				.map(author -> author.id)
				.collect(Collectors.toSet());

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		EntityGraph<Author> entityGraph = em.createEntityGraph(Author.class);
		entityGraph.addAttributeNodes("books");

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Author> query = criteriaBuilder.createQuery(Author.class);
		Root<Author> root = query.from(Author.class);
		query.where(criteriaBuilder.equal(root.get("name"), authorName));

		List<Integer> fetchedAuthorIds = em.createQuery(query)
				.setFirstResult(0)
				.setMaxResults(4)
				.setHint("jakarta.persistence.loadgraph", entityGraph)
				.getResultList()
				.stream()
				.map(author -> author.id)
				.collect(Collectors.toList());

		assertEquals(3, fetchedAuthorIds.size());
		assertTrue(fetchedAuthorIds.containsAll(authorIds));

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-15964")
	public void paginationOverEagerCollectionWithEmptyEG() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		String authorName = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
		Set<Integer> authorIds = IntStream.range(0, 3)
				.mapToObj(v -> {
					Author author = new Author(authorName);
					em.persist(author);
					em.persist(new Prize(author));
					em.persist(new Prize(author));
					return author;
				})
				.map(author -> author.id)
				.collect(Collectors.toSet());

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		EntityGraph<Author> entityGraph = em.createEntityGraph(Author.class);

		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Author> query = criteriaBuilder.createQuery(Author.class);
		Root<Author> root = query.from(Author.class);
		query.where(criteriaBuilder.equal(root.get("name"), authorName));

		List<Integer> fetchedAuthorIds = em.createQuery(query)
				.setFirstResult(0)
				.setMaxResults(4)
				.setHint("jakarta.persistence.loadgraph", entityGraph)
				.getResultList()
				.stream()
				.map(author -> author.id)
				.collect(Collectors.toList());

		assertEquals(3, fetchedAuthorIds.size());
		assertTrue(fetchedAuthorIds.containsAll(authorIds));

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-15972")
	public void joinedInheritanceWithAttributeConflictTest() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Dog dog = new Dog();
		em.persist( dog );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		Map<String, Object> properties = new HashMap<>();
		properties.put( "jakarta.persistence.loadgraph", em.createEntityGraph( Animal.class ) );

		Animal animal = em.find( Animal.class, dog.id, properties );
		assertTrue( animal instanceof Dog );
		assertEquals( dog.id, animal.id );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey("HHH-17192")
	public void joinedInheritanceWithSubEntityAttributeFiltering() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Dog dog = new Dog();
		em.persist( dog );
		AnimalOwner animalOwner = new AnimalOwner();
		animalOwner.animal = dog;
		em.persist( animalOwner );
		em.flush();
		em.clear();

		EntityGraph<AnimalOwner> entityGraph = em.createEntityGraph( AnimalOwner.class );
		entityGraph.addAttributeNodes( "animal" );
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<AnimalOwner> query = cb.createQuery( AnimalOwner.class );
		Root<AnimalOwner> root = query.from( AnimalOwner.class );
		query.where( cb.equal( root.get( "animal" ).get( "id" ), dog.id ) );
		AnimalOwner owner = em.createQuery( query )
				.setHint( "jakarta.persistence.loadgraph", entityGraph )
				.getResultList()
				.get( 0 );
		assertTrue( Hibernate.isInitialized( owner.animal ) );
		assertTrue( owner.animal instanceof Dog );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTreatedSubgraph() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Kennel kennel = new Kennel();
		em.persist( kennel );
		Dog dog = new Dog();
		dog.kennel = kennel;
		em.persist( dog );
		em.flush();
		em.clear();

		EntityGraph<Dog> graph = em.createEntityGraph( Dog.class );
		graph.addAttributeNode( "kennel" );
		Dog doggie = em.find( graph, dog.id );
		assertTrue( Hibernate.isInitialized( doggie.kennel ) );

		em.clear();

		EntityGraph<Animal> withKennel = em.createEntityGraph( Animal.class );
		withKennel.addTreatedSubgraph( Dog.class ).addAttributeNode( "kennel" );
		Animal animal = em.find( withKennel, doggie.id );
		assertTrue( Hibernate.isInitialized( ( (Dog) animal ).kennel ) );

		em.clear();

		EntityGraph<Animal> withoutKennel = em.createEntityGraph( Animal.class );
		animal = em.find( withoutKennel, doggie.id );
		assertFalse( Hibernate.isInitialized( ( (Dog) animal ).kennel ) );

		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testElementSubgraph() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Bar bar = new Bar();
		Foo foo = new Foo();
		Baz baz = new Baz();
		foo.baz = baz;
		foo.bar = bar;
		bar.foos.add( foo );
		baz.foos.add( foo );
		em.persist( bar );
		em.persist( baz );
		em.persist( foo );
		em.flush();
		em.clear();

		EntityGraph<Bar> graph = em.createEntityGraph( Bar.class );
		Subgraph<Foo> subgraph = graph.addElementSubgraph( "foos", Foo.class );
		subgraph.addAttributeNode( "baz" );
		Bar b = em.find( graph, bar.id );
		assertTrue( Hibernate.isInitialized( b.foos ) );
		assertTrue( Hibernate.isInitialized( b.foos.iterator().next().baz ) );

		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testTreatedElementSubgraph() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		AnimalOwner animalOwner = new AnimalOwner();
		Dog dog = new Dog();
		Cat cat = new Cat();
		Kennel kennel = new Kennel();
		dog.kennel = kennel;
		animalOwner.animals.add( dog );
		animalOwner.animals.add( cat );
		em.persist( animalOwner );
		em.persist( kennel );
		em.persist( dog );
		em.persist( cat );
		em.flush();
		em.clear();

		PluralAttribute<? super AnimalOwner, ?, Animal> animalsAttribute =
				(PluralAttribute<? super AnimalOwner, ?, Animal>)
						em.getEntityManagerFactory().getMetamodel()
								.entity( AnimalOwner.class )
								.getAttribute( "animals" );

		EntityGraph<AnimalOwner> graph = em.createEntityGraph( AnimalOwner.class );
		Subgraph<Animal> subgraph = graph.addElementSubgraph( animalsAttribute );
		AnimalOwner owner = em.find( graph, animalOwner.id );
		assertTrue( Hibernate.isInitialized( owner.animals ) );
		assertEquals( 2, owner.animals.size() );
		owner.animals.forEach( animal -> {
			if (animal instanceof Dog d ) {
				assertFalse( Hibernate.isInitialized( d.kennel ) );
			}
		} );

		em.clear();

		graph = em.createEntityGraph( AnimalOwner.class );
		subgraph = graph.addElementSubgraph( animalsAttribute );
		Subgraph<Dog> treated = graph.addTreatedElementSubgraph( animalsAttribute, Dog.class );
		treated.addAttributeNode( "kennel" );
		owner = em.find( graph, animalOwner.id );
		assertTrue( Hibernate.isInitialized( owner.animals ) );
		assertEquals( 2, owner.animals.size() );
		owner.animals.forEach( animal -> {
			if (animal instanceof Dog d ) {
				assertTrue( Hibernate.isInitialized( d.kennel ) );
			}
		} );

		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testTreatedElementSubgraph2() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		AnimalOwner animalOwner = new AnimalOwner();
		Dog dog = new Dog();
		Cat cat = new Cat();
		Kennel kennel = new Kennel();
		dog.kennel = kennel;
		animalOwner.animals.add( dog );
		animalOwner.animals.add( cat );
		em.persist( animalOwner );
		em.persist( kennel );
		em.persist( dog );
		em.persist( cat );
		em.flush();
		em.clear();

		Attribute<? super Dog, Kennel> kennelAttribute =
				(Attribute<? super Dog, Kennel>)
						em.getEntityManagerFactory().getMetamodel()
								.entity( Dog.class )
								.getAttribute( "kennel" );

		EntityGraph<Animal> graph = em.createEntityGraph( Animal.class );
		Animal animal = em.find( graph, dog.id );
		assertFalse( Hibernate.isInitialized( ((Dog) animal).kennel ) );

		em.clear();

		graph = em.createEntityGraph( Animal.class );
		graph.addTreatedSubgraph( Dog.class ).addAttributeNode( kennelAttribute );
		animal = em.find( graph, dog.id );
		assertTrue( Hibernate.isInitialized( ((Dog) animal).kennel ) );

		em.getTransaction().rollback();
		em.close();
	}

	@Entity(name = "Foo")
	@Table(name = "foo")
	public static class Foo {

		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Bar bar;

		@ManyToOne(fetch = FetchType.LAZY)
		public Baz baz;
	}

	@Entity(name = "Bar")
	@Table(name = "bar")
	public static class Bar {

		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(mappedBy = "bar")
		public Set<Foo> foos = new HashSet<>();

		public Set<Foo> getFoos() {
			return foos;
		}
	}

	@Entity(name = "Baz")
	@Table(name = "baz")
	public static class Baz {

		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(mappedBy = "baz")
		public Set<Foo> foos = new HashSet<>();

		public Set<Foo> getFoos() {
			return foos;
		}
	}

	@Entity(name = "Book")
	@Table(name = "book")
	public static class Book {
		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Author author;

		public Book() {

		}

		public Book(Author author) {
			this.author = author;
		}
	}

	@Entity(name = "Prize")
	public static class Prize {
		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Author author;

		public Prize() {

		}

		public Prize(Author author) {
			this.author = author;
		}

	}

	@Entity(name = "Author")
	@Table(name = "author")
	public static class Author {
		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
		@MapKey
		public Map<Integer, Book> books = new HashMap<>();

		@OneToMany(fetch = FetchType.EAGER, mappedBy = "author")
		public Set<Prize> eagerPrizes = new HashSet<>();

		public String name;

		public Author() {

		}

		public Author(String name) {
			this.name = name;
		}
	}

	@Entity(name = "AnimalOwner")
	public static class AnimalOwner {
		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Animal animal;

		@ManyToMany
		public Set<Animal> animals = new HashSet<>();
	}

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Animal {
		@Id
		@GeneratedValue
		public Integer id;
		public String dtype;
		public String name;
	}

	@Entity(name = "Dog")
	@DiscriminatorValue("DOG")
	public static class Dog extends Animal {

		public Integer numberOfLegs;

		@ManyToOne(fetch = FetchType.LAZY)
		Kennel kennel;

		public Dog() {
			dtype = "DOG";
		}
	}

	@Entity(name = "Cat")
	@DiscriminatorValue("CAT")
	public static class Cat extends Animal {

		public Integer numberOfLegs;

		public Cat() {
			dtype = "CAT";
		}
	}

	@Entity(name = "Kennel")
	public static class Kennel {
		@Id @GeneratedValue
		UUID uuid;
	}
}
