package org.hibernate.orm.test.any.annotations;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SqlFragmentAlias;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vincent Bouthinon
 */
@DomainModel(annotatedClasses = {
		ManyToAnyWithFilterJoinTableTest.Actor.class,
		ManyToAnyWithFilterJoinTableTest.Person.class,
		ManyToAnyWithFilterJoinTableTest.Organization.class
})
@SessionFactory
@JiraKey("HHH-18986")
class ManyToAnyWithFilterJoinTableTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person first = new Person( 1L );
			final Person second = new Person( 2L );
			final Organization organization = new Organization( 3L );
			session.persist( first );
			session.persist( second );
			session.persist( organization );

			final Actor actor = new Actor();
			actor.id = 1L;
			actor.contacts.addAll( Set.of( first, second, organization ) );
			session.persist( actor );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void disabledFilters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Actor actor = session.find( Actor.class, 1L );
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactlyInAnyOrder( 1L, 2L, 3L );
		} );
	}

	@Test
	void defaultCondition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "P" );
			final Actor actor = session.find( Actor.class, 1L );
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactlyInAnyOrder( 1L, 2L );
			assertThat( actor.contacts ).allMatch( Person.class::isInstance );
		} );
	}

	@Test
	void explicitConditionAndAlias(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "minimumContactId" ).setParameter( "minimumId", 2L );
			final Actor actor = session.find( Actor.class, 1L );
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactlyInAnyOrder( 2L, 3L );
		} );
	}

	@Test
	void ordinaryFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "maximumContactId" ).setParameter( "maximumId", 1L );
			final Actor actor = session.find( Actor.class, 1L );
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactly( 1L );
		} );
	}

	@Test
	void combinedFilters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "P" );
			session.enableFilter( "minimumContactId" ).setParameter( "minimumId", 2L );
			session.enableFilter( "maximumContactId" ).setParameter( "maximumId", 2L );
			final Actor actor = session.find( Actor.class, 1L );
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactly( 2L );
		} );
	}

	@Test
	void joinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "O" );
			final Actor actor = session.createQuery( "from Actor a join fetch a.contacts", Actor.class )
					.getSingleResult();
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactly( 3L );
			assertThat( actor.contacts ).allMatch( Organization.class::isInstance );
		} );
	}

	@Test
	void ordinaryFilterJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "maximumContactId" ).setParameter( "maximumId", 1L );
			final Actor actor = session.createQuery( "from Actor a join fetch a.contacts", Actor.class )
					.getSingleResult();
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactly( 1L );
		} );
	}

	@Test
	void combinedFiltersJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "P" );
			session.enableFilter( "minimumContactId" ).setParameter( "minimumId", 2L );
			session.enableFilter( "maximumContactId" ).setParameter( "maximumId", 2L );
			final Actor actor = session.createQuery( "from Actor a join fetch a.contacts", Actor.class )
					.getSingleResult();
			assertThat( actor.contacts ).extracting( Contact::getId ).containsExactly( 2L );
		} );
	}

	@Test
	void innerJoinFetchWithoutMatchingContacts(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "missing" );
			assertThat( session.createQuery( "from Actor a join fetch a.contacts", Actor.class ).getResultList() )
					.isEmpty();
		} );
	}

	@Test
	void leftJoinFetchWithoutMatchingContacts(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "missing" );
			final Actor actor = session.createQuery( "from Actor a left join fetch a.contacts", Actor.class )
					.getSingleResult();
			assertThat( actor.id ).isEqualTo( 1L );
			assertThat( actor.contacts ).isEmpty();
		} );
	}

	@Test
	void joinWithoutFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "contactType" ).setParameter( "type", "O" );
			assertThat( session.createQuery( "select a.id from Actor a join a.contacts c", Long.class ).getResultList() )
					.containsExactly( 1L );
		} );
	}

	@Test
	void changeAndDisableFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final org.hibernate.Filter filter = session.enableFilter( "contactType" ).setParameter( "type", "O" );
			assertThat( session.find( Actor.class, 1L ).contacts ).extracting( Contact::getId ).containsExactly( 3L );

			session.clear();
			filter.setParameter( "type", "missing" );
			assertThat( session.find( Actor.class, 1L ).contacts ).isEmpty();

			session.clear();
			session.disableFilter( "contactType" );
			assertThat( session.find( Actor.class, 1L ).contacts )
					.extracting( Contact::getId ).containsExactlyInAnyOrder( 1L, 2L, 3L );
		} );
	}

	@Entity(name = "Actor")
	@FilterDef(name = "contactType", defaultCondition = "contact_type = :type",
			parameters = @ParamDef(name = "type", type = String.class))
	@FilterDef(name = "minimumContactId", defaultCondition = "1=0",
			parameters = @ParamDef(name = "minimumId", type = Long.class))
	@FilterDef(name = "maximumContactId", parameters = @ParamDef(name = "maximumId", type = Long.class))
	public static class Actor {
		@Id
		private Long id;

		@ManyToAny
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(discriminator = "P", entity = Person.class)
		@AnyDiscriminatorValue(discriminator = "O", entity = Organization.class)
		@Column(name = "contact_type")
		@JoinTable(name = "actor_contact", joinColumns = @JoinColumn(name = "actor_id"),
				inverseJoinColumns = @JoinColumn(name = "contact_id"))
		@FilterJoinTable(name = "contactType")
		@FilterJoinTable(name = "minimumContactId", condition = "{contacts}.contact_id >= :minimumId",
				deduceAliasInjectionPoints = false, aliases = @SqlFragmentAlias(alias = "contacts", table = "actor_contact"))
		@Filter(name = "maximumContactId", condition = "contact_id <= :maximumId")
		private Set<Contact> contacts = new HashSet<>();
	}

	public interface Contact {
		Long getId();
	}

	@Entity(name = "Person")
	public static class Person implements Contact {
		@Id
		private Long id;

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Organization")
	public static class Organization implements Contact {
		@Id
		private Long id;

		public Organization() {
		}

		public Organization(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return id;
		}
	}
}
