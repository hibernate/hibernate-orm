package org.hibernate.orm.test.where.annotations;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
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
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import static org.assertj.core.api.Assertions.assertThat;

/** The physical join key is an embeddable, whereas the selected identifier is a Long. */
@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = { RestrictedToOneCandidateKeyTest.Owner.class, RestrictedToOneCandidateKeyTest.Target.class })
@SessionFactory(exportSchema = false)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = {
		RestrictedToOneCandidateKeyTest.Owner.class, RestrictedToOneCandidateKeyTest.Target.class })
class RestrictedToOneCandidateKeyTest {
	private RestrictedToOneCandidateSchema schema;

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		schema = new RestrictedToOneCandidateSchema( scope,
				RestrictedToOneCandidateSchema.KeyOwner.class, RestrictedToOneCandidateSchema.KeyTarget.class );
		scope.inTransaction( session -> {
			schema.insert( session, "candidate_key_target", "id,tenant,label_code,lang_code,target_text",
					new Object[] { 101, "t", "a", "en", "A" },
					new Object[] { 102, "t", "a", "nl", "B" },
					new Object[] { 201, "u", "a", "en", "C" },
					new Object[] { 202, "u", "a", "nl", "D" } );
			schema.insert( session, "candidate_key_owner", "id,tenant,label_code,name",
					new Object[] { 1, "t", "a", "one" },
					new Object[] { 2, "u", "a", "two" },
					new Object[] { 3, null, null, "three" },
					new Object[] { 4, "t", "missing", "four" } );
		} );
	}

	@AfterEach
	void dropSchema() {
		if ( schema != null ) {
			schema.close();
		}
	}
	@Test
	void lazyTargetHasScalarIdentifier(SessionFactoryScope scope) {
		separatesCompositeJoinKeyAndTargetIdentifier( "find", "nl", scope );
	}

	@Test
	void nativeQueryRetainsHiddenCompositeKey(SessionFactoryScope scope) {
		separatesCompositeJoinKeyAndTargetIdentifier( "native", "fr", scope );
	}

	@Test
	void joinFetchHasScalarIdentifier(SessionFactoryScope scope) {
		separatesCompositeJoinKeyAndTargetIdentifier( "fetch", "nl", scope );
	}

	private void separatesCompositeJoinKeyAndTargetIdentifier(String loading, String language, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "keyLanguage" ).setParameter( "language", language );
			for ( long id = 1; id <= 4; id++ ) {
				final Owner owner = switch ( loading ) {
					case "find" -> session.find( Owner.class, id );
					case "native" -> session.createNativeQuery( "select * from candidate_key_owner where id=:id", Owner.class )
							.setParameter( "id", id ).getSingleResult();
					default -> session.createSelectionQuery( "from CompositeKeyOwner o"
							+ (loading.equals( "fetch" ) ? " left join fetch o.filtered left join fetch o.english" : "")
							+ " where o.id=:id", Owner.class ).setParameter( "id", id ).getSingleResult();
				};
				if ( id <= 2 ) {
					assertThat( owner.english.getId() ).isEqualTo( id * 100 + 1 );
					if ( language.equals( "fr" ) ) { assertThat( owner.filtered ).isNull(); }
					else {
						assertThat( owner.filtered.getId() ).isEqualTo( id * 100 + 2 );
						assertThat( owner.filtered.getText() ).isNotNull();
					}
				}
				else {
					assertThat( owner.filtered ).isNull();
					assertThat( owner.english ).isNull();
				}
				owner.name = "updated";
			}
		} );
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select tenant, label_code from candidate_key_owner where id=1", Object[].class )
					.getSingleResult() ).containsExactly( "t", "a" );
			assertThat( session.createNativeQuery( "select tenant, label_code from candidate_key_owner where id=4", Object[].class )
					.getSingleResult() ).containsExactly( "t", "missing" );
		} );
	}
	@Embeddable
	public static class LabelKey {
		String tenant;
		@Column(name = "label_code") String label;
	}
	@Entity(name = "CompositeKeyTarget")
	@Table(name = "candidate_key_target")
	@FilterDef(name = "keyLanguage", parameters = @ParamDef(name = "language", type = String.class))
	public static class Target {
		@Id Long id;
		@Embedded LabelKey label;
		@Column(name = "lang_code") String language;
		@Column(name = "target_text") String text;
		public Long getId() { return id; }
		public String getText() { return text; }
	}
	@Entity(name = "CompositeKeyOwner")
	@Table(name = "candidate_key_owner")
	public static class Owner {
		@Id Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({ @JoinColumn(name = "label_code", referencedColumnName = "label_code"),
				@JoinColumn(name = "tenant", referencedColumnName = "tenant") })
		@Filter(name = "keyLanguage", condition = "lang_code = :language")
		Target filtered;
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumns({ @JoinColumn(name = "label_code", referencedColumnName = "label_code", insertable = false, updatable = false),
				@JoinColumn(name = "tenant", referencedColumnName = "tenant", insertable = false, updatable = false) })
		@SQLRestriction("lang_code = 'en'")
		Target english;
	}
}
