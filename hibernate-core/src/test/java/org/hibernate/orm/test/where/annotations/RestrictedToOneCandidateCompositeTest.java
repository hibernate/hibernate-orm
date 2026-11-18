package org.hibernate.orm.test.where.annotations;

import java.io.Serializable;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercises a string association key whose target has a composite identifier. */
@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = { RestrictedToOneCandidateCompositeTest.Target.class,
		RestrictedToOneCandidateCompositeTest.Owner.class, RestrictedToOneCandidateCompositeTest.JoinOwner.class })
@SessionFactory(exportSchema = false)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = {
		RestrictedToOneCandidateCompositeTest.Owner.class, RestrictedToOneCandidateCompositeTest.Target.class })
class RestrictedToOneCandidateCompositeTest {
	private RestrictedToOneCandidateSchema schema;

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		schema = new RestrictedToOneCandidateSchema( scope,
				RestrictedToOneCandidateSchema.CompositeOwner.class, RestrictedToOneCandidateSchema.CompositeTarget.class,
				RestrictedToOneCandidateSchema.CompositeLink.class, RestrictedToOneCandidateSchema.CompositeEnglish.class );
		scope.inTransaction( session -> {
			schema.insert( session, "candidate_composite_target", "tenant,target_number,label_code,lang_code,active,target_text",
					new Object[] { "t", 101, "a", "en", 1, "A" },
					new Object[] { "t", 102, "a", "nl", 1, "B" },
					new Object[] { "t", 201, "b", "en", 0, "C" },
					new Object[] { "t", 202, "b", "nl", 1, "D" } );
			schema.insert( session, "candidate_composite_owner", "id,label_code,name",
					new Object[] { 1, "a", "one" },
					new Object[] { 2, "b", "two" },
					new Object[] { 3, null, "three" },
					new Object[] { 4, "a", "four" } );
			schema.insert( session, "candidate_composite_link", "owner_id,label_code,enabled",
					new Object[] { 1, "a", 1 },
					new Object[] { 2, "b", 1 },
					new Object[] { 4, "a", 0 } );
			schema.insert( session, "candidate_composite_english", "owner_id,label_code,enabled",
					new Object[] { 1, "a", 1 },
					new Object[] { 2, "b", 1 },
					new Object[] { 4, "a", 0 } );
		} );
	}

	@AfterEach
	void dropSchema() {
		if ( schema != null ) {
			schema.close();
		}
	}
	@Test
	void lazyCompositeIdentifier(SessionFactoryScope scope) {
		resolvesCompositeIdentifierFromStringKey( Owner.class, "find", "nl", scope );
	}

	@Test
	void nativeCompositeIdentifier(SessionFactoryScope scope) {
		resolvesCompositeIdentifierFromStringKey( Owner.class, "native", "nl", scope );
	}

	@Test
	void hiddenCompositeIdentifierRetainsStringKey(SessionFactoryScope scope) {
		resolvesCompositeIdentifierFromStringKey( Owner.class, "find", "fr", scope );
	}

	@Test
	void subselectCombinesTargetAndJoinTableRestrictions(SessionFactoryScope scope) {
		resolvesCompositeIdentifierFromStringKey( JoinOwner.class, "hql", "nl", scope );
	}

	@Test
	void joinFetchPreservesNullCompositeTarget(SessionFactoryScope scope) {
		resolvesCompositeIdentifierFromStringKey( JoinOwner.class, "fetch", "fr", scope );
	}

	void resolvesCompositeIdentifierFromStringKey(Class<? extends BaseOwner> type, String loading, String language,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "compositeLanguage" ).setParameter( "language", language );
			final var owner = load( session, type, loading, 1 );
			assertTarget( owner.getFiltered(), language.equals( "nl" ) ? 102 : null );
			assertTarget( owner.getEnglish(), 101 );
			final var hidden = load( session, type, loading, 2 );
			assertTarget( hidden.getFiltered(), language.equals( "nl" ) ? 202 : null );
			assertTarget( hidden.getEnglish(), null );
			final var absent = load( session, type, loading, 3 );
			assertTarget( absent.getFiltered(), null );
			assertTarget( absent.getEnglish(), null );
			if ( type == JoinOwner.class ) {
				final var noLink = load( session, type, loading, 4 );
				assertTarget( noLink.getFiltered(), null );
				assertTarget( noLink.getEnglish(), null );
			}
			hidden.name = "updated";
		} );
		scope.inTransaction( session -> {
			final String table = type == JoinOwner.class ? "candidate_composite_link" : "candidate_composite_owner";
			final String id = type == JoinOwner.class ? "owner_id" : "id";
			assertThat( session.createNativeQuery( "select label_code from " + table + " where " + id + "=2", String.class ).getSingleResult() )
					.isEqualTo( "b" );
		} );
	}
	static <T extends BaseOwner> T load(Session session, Class<T> type, String loading, long id) {
		return switch ( loading ) {
			case "find" -> session.find( type, id );
			case "native" -> session.createNativeQuery( type == JoinOwner.class
					? "select o.id, o.name, l.label_code from candidate_composite_owner o left join candidate_composite_link l on l.owner_id=o.id and l.enabled=1 where o.id=:id"
					: "select * from candidate_composite_owner where id=:id", type )
					.setParameter( "id", id ).getSingleResult();
			default -> session.createSelectionQuery( "from " + type.getSimpleName() + " o"
					+ (loading.equals( "fetch" ) ? " left join fetch o.filtered left join fetch o.english" : "")
					+ " where o.id=:id", type ).setParameter( "id", id ).getSingleResult();
		};
	}
	static void assertTarget(Target target, Integer number) {
		if ( number == null ) { assertThat( target ).isNull(); }
		else {
			assertThat( target ).isNotNull();
			assertThat( target.getId() ).isEqualTo( new Key( "t", number ) );
			assertThat( target.getText() ).isNotNull();
		}
	}
	@Embeddable
	public static class Key implements Serializable {
		@Column(length = 10) String tenant;
		@Column(name = "target_number") int number;
		public Key() {}
		Key(String tenant, int number) { this.tenant = tenant; this.number = number; }
		@Override public boolean equals(Object other) {
			return other instanceof Key key && number == key.number && Objects.equals( tenant, key.tenant );
		}
		@Override public int hashCode() { return Objects.hash( tenant, number ); }
	}
	@Entity(name = "CompositeCandidate")
	@Table(name = "candidate_composite_target")
	@SQLRestriction("active = 1")
	@FilterDef(name = "compositeLanguage", parameters = @ParamDef(name = "language", type = String.class))
	public static class Target {
		@EmbeddedId Key id;
		@Column(name = "label_code") String labelCode;
		@Column(name = "lang_code") String language;
		int active;
		@Column(name = "target_text") String text;
		public Key getId() { return id; }
		public String getText() { return text; }
	}
	@MappedSuperclass
	public abstract static class BaseOwner {
		@Id Long id;
		String name;
		public abstract Target getFiltered();
		public abstract Target getEnglish();
	}
	@Entity(name = "Owner")
	@Table(name = "candidate_composite_owner")
	public static class Owner extends BaseOwner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_code", referencedColumnName = "label_code")
		@Filter(name = "compositeLanguage", condition = "lang_code = :language")
		Target filtered;
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_code", referencedColumnName = "label_code", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Target english;
		@Override public Target getFiltered() { return filtered; }
		@Override public Target getEnglish() { return english; }
	}
	@Entity(name = "JoinOwner")
	@Table(name = "candidate_composite_owner")
	public static class JoinOwner extends BaseOwner {
		@ManyToOne
		@Fetch(FetchMode.SUBSELECT)
		@JoinTable(name = "candidate_composite_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "label_code", referencedColumnName = "label_code"))
		@SQLJoinTableRestriction("enabled = 1")
		@Filter(name = "compositeLanguage", condition = "lang_code = :language")
		Target filtered;
		@OneToOne
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "candidate_composite_english", joinColumns = @JoinColumn(name = "owner_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(name = "label_code", referencedColumnName = "label_code", insertable = false, updatable = false))
		@SQLJoinTableRestriction("enabled = 1")
		@SQLRestriction("lang_code = 'en'")
		Target english;
		@Override public Target getFiltered() { return filtered; }
		@Override public Target getEnglish() { return english; }
	}
}
