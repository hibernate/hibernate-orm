/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Locking.FollowOn.FORCE;
import static org.hibernate.Locking.Scope.ROOT_ONLY;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		FollowOnLockingTests.Name.class,
		FollowOnLockingTests.Person.class,
		FollowOnLockingTests.Post.class,
		FollowOnLockingTests.Team.class,
		FollowOnLockingTests.Customer.class
} )
@SessionFactory(useCollectingStatementInspector = true)
public class FollowOnLockingTests {

	/**
	 * Performs follow-on locking against an entity (Person) with no associations
	 */
	@Test
	void testSimpleLockScopeCases(SessionFactoryScope factoryScope) {
		createTeamsData( factoryScope );

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// find()

		// 	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Person.class, 1, PESSIMISTIC_WRITE, ROOT_ONLY, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		// 	with INCLUDE_COLLECTIONS
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Person.class, 1, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_COLLECTIONS, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		// 	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Person.class, 1, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Query

		// 	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Person" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( ROOT_ONLY )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		// 	with INCLUDE_COLLECTIONS
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Person" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_COLLECTIONS )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		// 	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Person" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_FETCHES )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );
	}

	private boolean usesTableHints(Dialect dialect) {
		return dialect.getLockingSupport().getMetadata().getPessimisticLockStyle() == PessimisticLockStyle.TABLE_HINT;
	}

	/**
	 * Performs follow-on locking against an entity (Team) with a plural attribute
	 */
	@Test
	void testCollectionLockScopeCases(SessionFactoryScope factoryScope) {
		createTeamsData( factoryScope );

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// find()

		//	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Team.class, 1, PESSIMISTIC_WRITE, ROOT_ONLY, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_COLLECTIONS
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Team.class, 1, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_COLLECTIONS, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Team.class, 1, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Query

		//	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Team" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( ROOT_ONLY )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_COLLECTIONS
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Team" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_COLLECTIONS )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			}
		} );

		//	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Team" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_FETCHES )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Team join fetch members" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_FETCHES )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock teams, lock persons
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			}
		} );
	}

	/**
	 * Performs follow-on locking against an entity (Post) with a to-one (it also has an element-collection)
	 */
	@Test
	void testToOneCases(SessionFactoryScope factoryScope) {
		createPostsData( factoryScope );

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// find()

		//	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Post.class, 1, PESSIMISTIC_WRITE, ROOT_ONLY, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_COLLECTIONS
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Post.class, 1, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_COLLECTIONS, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts, lock tags
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			}
		} );

		//	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Post.class, 1, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Query

		//	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Post" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( ROOT_ONLY )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_COLLECTIONS
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Post" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_COLLECTIONS )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts, lock tags
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			}
		} );

		//	with INCLUDE_FETCHES
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Post" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_FETCHES )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			}
		} );

		//	with INCLUDE_FETCHES (with fetch)
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Post join fetch author" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_FETCHES )
					.setFollowOnStrategy( FORCE )
					.list();
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock posts, lock tags
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			}
		} );
	}

	/**
	 * Performs follow-on locking against an entity (Customer) with secondary tables
	 */
	@Test
	void testSecondaryTables(SessionFactoryScope factoryScope) {
		createCustomersData( factoryScope );

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// #find

		//	with ROOT_ONLY
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			session.find( Customer.class, 1, PESSIMISTIC_WRITE, ROOT_ONLY, FORCE );
			if ( usesTableHints( session.getDialect() ) ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			}
			else {
				// the initial query, lock customers, lock receivables
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			}
		} );
	}

	private void createTeamsData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Person jalen = new Person( 1, "Jalen", "Hurts" );
			session.persist( jalen );

			final Person saquon = new Person( 2, "Saquon", "Barkley" );
			session.persist( saquon );

			final Person zack = new Person( 3, "Zack", "Baun" );
			session.persist( zack );

			final Team team1 = new Team( 1, "Philadelphia Eagles" );
			team1.addMembers( jalen, saquon, zack );
			session.persist( team1 );
		} );
	}

	private void createPostsData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Person camus = new Person( 1, "Albert", "Camus" );
			session.persist( camus );

			final Post post = new Post( 1, "Thoughts on The Stranger", "...", camus );
			post.addTags( "exciting", "riveting" );
			session.persist( post );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	private void createCustomersData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Customer spacely = new Customer( 1, "Spacely Sprokets", "Out there", "1234", "Cosmo Spacely" );
			session.persist( spacely );
		} );
	}

	@Embeddable
	public record Name(String first, String last) {
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		@Embedded
		private Name name;

		public Person() {
		}

		public Person(Integer id, Name name) {
			this.id = id;
			this.name = name;
		}

		public Person(Integer id, String firstName, String lastName) {
			this( id, new Name( firstName, lastName ) );
		}
	}

	@Entity(name="Post")
	@Table(name="posts")
	public static class Post {
		@Id
		private Integer id;
		private String title;
		@Lob
		private String body;
		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "author_fk")
		private Person author;
		@ElementCollection
		@CollectionTable(name = "tags", joinColumns = @JoinColumn(name = "post_fk"))
		private Set<String> tags;

		public Post() {
		}

		public Post(Integer id, String title, String body, Person author) {
			this.id = id;
			this.title = title;
			this.body = body;
			this.author = author;
		}

		public void addTags(String... tags) {
			if ( this.tags == null ) {
				this.tags = new HashSet<>();
			}
			Collections.addAll( this.tags, tags );
		}
	}

	@Entity(name="Team")
	@Table(name="teams")
	public static class Team {
		@Id
		private Integer id;
		private String name;
		@OneToMany
		@JoinColumn(name = "team_fk")
		private Set<Person> members;

		public Team() {
		}

		public Team(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Person> getMembers() {
			return members;
		}

		public void setMembers(Set<Person> members) {
			this.members = members;
		}

		public Team addMember(Person member) {
			if ( members == null ) {
				members = new HashSet<>();
			}
			members.add( member );
			return this;
		}

		public Team addMembers(Person... incoming) {
			if ( members == null ) {
				members = new HashSet<>();
			}
			Collections.addAll( members, incoming );
			return this;
		}
	}

	@Entity(name="Customer")
	@Table(name="customers")
	@SecondaryTable(name = "receivables", pkJoinColumns = @PrimaryKeyJoinColumn(name = "customer_fk"))
	public static class Customer {
		@Id
		private Integer id;
		private String name;
		private String location;
		@Column(table = "receivables")
		private String accountNumber;
		@Column(table = "receivables")
		private String billingEntity;

		public Customer() {
		}

		public Customer(Integer id, String name, String location, String accountNumber, String billingEntity) {
			this.id = id;
			this.name = name;
			this.location = location;
			this.accountNumber = accountNumber;
			this.billingEntity = billingEntity;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public String getAccountNumber() {
			return accountNumber;
		}

		public void setAccountNumber(String accountNumber) {
			this.accountNumber = accountNumber;
		}

		public String getBillingEntity() {
			return billingEntity;
		}

		public void setBillingEntity(String billingEntity) {
			this.billingEntity = billingEntity;
		}
	}
}
