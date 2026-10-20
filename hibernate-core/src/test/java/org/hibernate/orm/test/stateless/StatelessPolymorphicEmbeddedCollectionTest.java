package org.hibernate.orm.test.stateless;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		StatelessPolymorphicEmbeddedCollectionTest.PreferenceOwner.class,
		StatelessPolymorphicEmbeddedCollectionTest.Preferences.class,
		StatelessPolymorphicEmbeddedCollectionTest.SkillPreferences.class,
		StatelessPolymorphicEmbeddedCollectionTest.TagPreferences.class
})
@SessionFactory
@JiraKey("HHH-20997")
class StatelessPolymorphicEmbeddedCollectionTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testInsertSiblingSubtypes(SessionFactoryScope scope) {
		final var skillOwner = skillOwner();
		final var tagOwner = new PreferenceOwner();
		tagOwner.preferences = tagPreferences();
		scope.inStatelessTransaction( session -> {
			session.insert( skillOwner );
			session.insert( tagOwner );
		} );
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select skill_name from preference_skills", String.class )
					.getResultList() ).containsExactly( "Java" );
			assertThat( session.createNativeQuery( "select tag_name from preference_tags", String.class )
					.getResultList() ).containsExactly( "hibernate" );
		} );
	}

	@Test
	void testUpdateChangesSubtype(SessionFactoryScope scope) {
		final var owner = seed( scope );
		owner.preferences = tagPreferences();
		scope.inStatelessTransaction( session -> session.update( owner ) );
		assertReplaced( scope, owner );
	}

	@Test
	void testUpsertChangesSubtype(SessionFactoryScope scope) {
		final var owner = seed( scope );
		owner.preferences = tagPreferences();
		scope.inStatelessTransaction( session -> session.upsert( owner ) );
		assertReplaced( scope, owner );
	}

	@Test
	void testDeleteWithDifferentSubtype(SessionFactoryScope scope) {
		final var owner = seed( scope );
		owner.preferences = tagPreferences();
		scope.inStatelessTransaction( session -> session.delete( owner ) );
		scope.inTransaction( session -> {
			assertThat( session.find( PreferenceOwner.class, owner.id ) ).isNull();
			assertThat( session.createNativeQuery( "select count(*) from preference_skills", Long.class )
					.getSingleResult() ).isZero();
		} );
	}

	private static PreferenceOwner seed(SessionFactoryScope scope) {
		final var owner = skillOwner();
		scope.inTransaction( session -> session.persist( owner ) );
		return owner;
	}

	private static PreferenceOwner skillOwner() {
		final var preferences = new SkillPreferences();
		preferences.skills.add( "Java" );
		final var owner = new PreferenceOwner();
		owner.preferences = preferences;
		return owner;
	}

	private static TagPreferences tagPreferences() {
		final var preferences = new TagPreferences();
		preferences.tags.add( "hibernate" );
		return preferences;
	}

	private static void assertReplaced(SessionFactoryScope scope, PreferenceOwner owner) {
		scope.inTransaction( session -> {
			final var loaded = session.find( PreferenceOwner.class, owner.id );
			assertThat( loaded.preferences ).isExactlyInstanceOf( TagPreferences.class );
			assertThat( ( (TagPreferences) loaded.preferences ).tags ).containsExactly( "hibernate" );
			assertThat( session.createNativeQuery( "select count(*) from preference_skills", Long.class )
					.getSingleResult() ).isZero();
		} );
	}

	@Entity(name = "PreferenceOwner")
	static class PreferenceOwner {
		@Id
		@GeneratedValue
		Long id;

		@Embedded
		Preferences preferences;
	}

	@Embeddable
	static abstract class Preferences {
		String description = "preferences";
	}

	@Embeddable
	@DiscriminatorValue("skills")
	static class SkillPreferences extends Preferences {
		@ElementCollection
		@CollectionTable(name = "preference_skills")
		@Column(name = "skill_name")
		List<String> skills = new ArrayList<>();
	}

	@Embeddable
	@DiscriminatorValue("tags")
	static class TagPreferences extends Preferences {
		@ElementCollection
		@CollectionTable(name = "preference_tags")
		@Column(name = "tag_name")
		List<String> tags = new ArrayList<>();
	}
}
