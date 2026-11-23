package org.hibernate.orm.test.stateless;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import static jakarta.persistence.FetchType.EAGER;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		StatelessEmbeddedCollectionTest.Employee.class,
		StatelessEmbeddedCollectionTest.ProfileOwner.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"))
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-20997")
class StatelessEmbeddedCollectionTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	void testInsert(SessionFactoryScope scope) {
		final var employee = new Employee();
		employee.skillSet.skills.addAll( List.of( new Skill( "Java", 3 ), new Skill( "JPA", 2 ), new Skill( "SQL", 1 ) ) );
		scope.inStatelessTransaction( session -> session.insert( employee ) );
		assertThat( employee.id ).isNotNull();
		assertSkills( scope, employee, "Java", "JPA", "SQL" );
	}

	@Test
	void testUpdate(SessionFactoryScope scope) {
		final var employee = seed( scope );
		employee.skillSet.skills = new ArrayList<>( List.of( new Skill( "SQL", 4 ), new Skill( "Java", 5 ), new Skill( "JPA", 2 ) ) );
		scope.inStatelessTransaction( session -> session.update( employee ) );
		assertSkills( scope, employee, "SQL", "Java", "JPA" );
		scope.inTransaction( session -> assertThat( session.find( Employee.class, employee.id ).skillSet.skills )
				.extracting( skill -> skill.skillLevel ).containsExactly( 4, 5, 2 ) );
	}

	@Test
	void testUpsert(SessionFactoryScope scope) {
		final var employee = seed( scope );
		employee.skillSet.skills = new ArrayList<>( List.of( new Skill( "JPA", 2 ), new Skill( "SQL", 3 ), new Skill( "Java", 4 ) ) );
		scope.inStatelessTransaction( session -> session.upsert( employee ) );
		assertSkills( scope, employee, "JPA", "SQL", "Java" );
	}

	@Test
	void testDelete(SessionFactoryScope scope) {
		final var employee = seed( scope );
		scope.inStatelessTransaction( session -> session.delete( employee ) );
		assertDeleted( scope, employee );
	}

	@Test
	void testDeleteWithNullComponent(SessionFactoryScope scope) {
		final var employee = seed( scope );
		employee.skillSet = null;
		scope.inStatelessTransaction( session -> session.delete( employee ) );
		assertDeleted( scope, employee );
	}

	@Test
	void testUpdateWithNullComponent(SessionFactoryScope scope) {
		final var employee = seed( scope );
		employee.skillSet = null;
		scope.inStatelessTransaction( session -> session.update( employee ) );
		assertCollectionEmpty( scope );
	}

	@Test
	void testUpdateWithEmptyCollection(SessionFactoryScope scope) {
		final var employee = seed( scope );
		employee.skillSet.skills = new ArrayList<>();
		scope.inStatelessTransaction( session -> session.update( employee ) );
		assertCollectionEmpty( scope );
	}

	@Test
	void testNestedAndUninitializedCollections(SessionFactoryScope scope) {
		final var owner = new ProfileOwner();
		owner.profile.skillSet.skills.add( new Skill( "Java", 3 ) );
		owner.profile.tags.add( "original" );
		scope.inStatelessTransaction( session -> session.insert( owner ) );
		scope.inStatelessTransaction( session -> {
			final var loaded = session.get( ProfileOwner.class, owner.id );
			assertThat( loaded.profile.skillSet.skills ).extracting( skill -> skill.name ).containsExactly( "Java" );
			assertThat( Hibernate.isInitialized( loaded.profile.tags ) ).isFalse();
			loaded.profile.description = "updated";
			session.update( loaded );
			assertThat( Hibernate.isInitialized( loaded.profile.tags ) ).isFalse();
		} );
		scope.inTransaction( session -> {
			final var loaded = session.find( ProfileOwner.class, owner.id );
			assertThat( loaded.profile.description ).isEqualTo( "updated" );
			assertThat( loaded.profile.tags ).containsExactly( "original" );
		} );
	}

	@Test
	void testUpdateInvalidatesCollectionCache(SessionFactoryScope scope) {
		final var owner = new ProfileOwner();
		owner.profile.tags.add( "original" );
		scope.inTransaction( session -> session.persist( owner ) );
		scope.inTransaction( session -> assertThat( session.find( ProfileOwner.class, owner.id ).profile.tags )
				.containsExactly( "original" ) );
		final var factory = scope.getSessionFactory();
		assertThat( factory.getCache().containsCollection( ProfileOwner.class.getName() + ".profile.tags", owner.id ) )
				.isTrue();
		factory.getStatistics().clear();
		scope.inTransaction( session -> assertThat( session.find( ProfileOwner.class, owner.id ).profile.tags )
				.containsExactly( "original" ) );
		assertThat( factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
		owner.profile.tags = new ArrayList<>( List.of( "updated" ) );
		scope.inStatelessTransaction( session -> session.update( owner ) );
		scope.inTransaction( session -> assertThat( session.find( ProfileOwner.class, owner.id ).profile.tags )
				.containsExactly( "updated" ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testCollectionEventOwner(boolean nullComponent, SessionFactoryScope scope) {
		final var employee = new Employee();
		if ( nullComponent ) {
			employee.skillSet = null;
		}
		else {
			employee.skillSet.skills.add( new Skill( "Java", 3 ) );
		}
		final var owners = new ArrayList<Object>();
		final var collectionOwners = new ArrayList<Object>();
		final var registry = scope.getSessionFactory().getEventListenerRegistry();
		final var group = registry.getEventListenerGroup( EventType.POST_COLLECTION_RECREATE );
		final var originalListeners = new ArrayList<PostCollectionRecreateEventListener>();
		group.listeners().forEach( originalListeners::add );
		final PostCollectionRecreateEventListener listener = event -> {
			if ( event.getAffectedOwnerOrNull() == employee ) {
				assertThat( event.getCollection().wasInitialized() ).isTrue();
				assertThat( event.getCollection().empty() ).isEqualTo( nullComponent );
				owners.add( event.getAffectedOwnerOrNull() );
				collectionOwners.add( event.getCollection().getOwner() );
			}
		};
		group.appendListener( listener );
		try {
			scope.inStatelessTransaction( session -> session.insert( employee ) );
			assertThat( owners ).containsExactly( employee );
			assertThat( collectionOwners ).containsExactly( employee );
		}
		finally {
			registry.setListeners( EventType.POST_COLLECTION_RECREATE,
					originalListeners.toArray( PostCollectionRecreateEventListener[]::new ) );
		}
	}

	private static Employee seed(SessionFactoryScope scope) {
		final var employee = new Employee();
		employee.skillSet.skills.addAll( List.of( new Skill( "Java", 1 ), new Skill( "SQL", 2 ) ) );
		scope.inTransaction( session -> session.persist( employee ) );
		return employee;
	}

	private static void assertDeleted(SessionFactoryScope scope, Employee employee) {
		scope.inTransaction( session -> assertThat( session.find( Employee.class, employee.id ) ).isNull() );
		assertCollectionEmpty( scope );
	}

	private static void assertCollectionEmpty(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select count(*) from employee_skills", Long.class
		).getSingleResult() ).isZero() );
	}

	private static void assertSkills(SessionFactoryScope scope, Employee employee, String... names) {
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
				"select s.name from Employee e join e.skillSet.skills s where e.id = :id order by index(s)",
				String.class
		).setParameter( "id", employee.id ).getResultList() ).containsExactly( names ) );
	}

	@Entity(name = "Employee")
	static class Employee {
		@Id
		@GeneratedValue
		Long id;

		private String name;

		@Embedded
		SkillSet skillSet = new SkillSet();
	}

	@Entity(name = "ProfileOwner")
	static class ProfileOwner {
		@Id
		@GeneratedValue
		Long id;

		@Embedded
		Profile profile = new Profile();
	}

	@Embeddable
	static class Profile {
		String description = "original";

		@Embedded
		NestedSkillSet skillSet = new NestedSkillSet();

		@ElementCollection
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		List<String> tags = new ArrayList<>();
	}

	@Embeddable
	static class SkillSet {
		@ElementCollection(fetch = EAGER)
		@CollectionTable(name = "employee_skills")
		@OrderColumn(name = "skill_order")
		List<Skill> skills = new ArrayList<>();
	}

	@Embeddable
	static class NestedSkillSet {
		@ElementCollection(fetch = EAGER)
		@CollectionTable(name = "profile_skills")
		@OrderColumn(name = "skill_order")
		List<Skill> skills = new ArrayList<>();
	}

	@Embeddable
	static class Skill {
		String name;
		int skillLevel;

		Skill() {
		}

		Skill(String name, int skillLevel) {
			this.name = name;
			this.skillLevel = skillLevel;
		}
	}
}
