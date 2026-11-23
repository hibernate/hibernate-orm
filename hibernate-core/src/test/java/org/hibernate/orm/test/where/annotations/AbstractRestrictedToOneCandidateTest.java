package org.hibernate.orm.test.where.annotations;

import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The join column is intentionally nonunique: the association predicate selects a target.
 * All owners share a physical table so that the fetching strategies use identical data.
 * A separate schema mapping avoids the unique constraint inferred by property-ref binding.
 */
@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = {
		AbstractRestrictedToOneCandidateTest.Label.class, AbstractRestrictedToOneCandidateTest.Translation.class,
		AbstractRestrictedToOneCandidateTest.ManyLazy.class,
		AbstractRestrictedToOneCandidateTest.ManyEager.class,
		AbstractRestrictedToOneCandidateTest.ManySelect.class,
		AbstractRestrictedToOneCandidateTest.ManySubselect.class,
		AbstractRestrictedToOneCandidateTest.OneLazy.class,
		AbstractRestrictedToOneCandidateTest.OneEager.class,
		AbstractRestrictedToOneCandidateTest.OneSelect.class,
		AbstractRestrictedToOneCandidateTest.OneSubselect.class,
		AbstractRestrictedToOneCandidateTest.InverseLazy.class,
		AbstractRestrictedToOneCandidateTest.InverseEager.class,
		AbstractRestrictedToOneCandidateTest.InverseSelect.class,
		AbstractRestrictedToOneCandidateTest.InverseSubselect.class
})
@SessionFactory(exportSchema = false, useCollectingStatementInspector = true, generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "false"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "false")
})
abstract class AbstractRestrictedToOneCandidateTest {
	static Stream<Class<? extends Owner>> representativeMappings() {
		return Stream.of( ManyLazy.class, OneLazy.class, InverseLazy.class );
	}

	private RestrictedToOneCandidateSchema schema;

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
		schema = new RestrictedToOneCandidateSchema( scope,
				RestrictedToOneCandidateSchema.LabelOwner.class, RestrictedToOneCandidateSchema.Translation.class );
		scope.inTransaction( session -> {
			schema.insert( session, "candidate_owner", "id,label_id,name",
					new Object[] { 1, 1, "one" },
					new Object[] { 2, 2, "two" },
					new Object[] { 3, 3, "three" },
					new Object[] { 4, 4, "four" } );
			schema.insert( session, "candidate_translation", "id,label_id,lang_code,target_text",
					new Object[] { 101, 1, "en", "B" },
					new Object[] { 102, 1, "nl", "D" },
					new Object[] { 103, 1, "de", "C" },
					new Object[] { 301, 3, "nl", "E" },
					new Object[] { 401, 4, "en", "A" } );
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void dropSchema() {
		if ( schema != null ) {
			schema.close();
		}
	}

	static void enable(Session session, String language) {
		session.enableFilter( "candidateLanguage" ).setParameter( "language", language );
	}

	static <T extends Owner> T load(Session session, Class<T> type, String loading, long id) {
		return switch ( loading ) {
			case "find" -> session.find( type, id );
			case "native" -> session.createNativeQuery( "select * from candidate_owner where id=:id", type )
					.setParameter( "id", id ).getSingleResult();
			case "graph", "graphLazy" -> {
				final var graph = session.createEntityGraph( type );
				if ( loading.equals( "graph" ) ) {
					graph.addAttributeNodes( "filtered", "english" );
				}
				yield session.createSelectionQuery( "from " + type.getSimpleName() + " o where o.id=:id", type )
						.setParameter( "id", id ).setEntityGraph( graph, GraphSemantic.FETCH ).getSingleResult();
			}
			default -> session.createSelectionQuery( "from " + type.getSimpleName() + " o"
					+ (loading.equals( "fetch" ) ? " left join fetch o.filtered left join fetch o.english" : "")
					+ " where o.id=:id", type ).setParameter( "id", id ).getSingleResult();
		};
	}

	static void assertTarget(Translation target, Long id) {
		if ( id == null ) {
			assertThat( target ).isNull();
		}
		else {
			assertThat( target ).isNotNull();
			assertThat( target.getId() ).isEqualTo( id );
			assertThat( target.getText() ).isNotNull();
		}
	}

	@MappedSuperclass
	public abstract static class Owner {
		@Id Long id;
		String name;
		public abstract Translation getFiltered();
		public abstract void setFiltered(Translation target);
		public abstract Translation getEnglish();
	}

	@Entity(name = "CandidateLabel")
	@Table(name = "candidate_owner")
	public static class Label {
		@Id Long id;
	}

	@Entity(name = "CandidateTranslation")
	@Table(name = "candidate_translation")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@FilterDef(name = "candidateLanguage", parameters = @ParamDef(name = "language", type = String.class))
	public static class Translation {
		@Id Long id;
		@Column(name = "label_id", insertable = false, updatable = false)
		Long labelId;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_id")
		Label label;
		@Column(name = "lang_code") String language;
		@Column(name = "target_text") String text;
		public Long getId() { return id; }
		public String getText() { return text; }
	}

	@Entity(name = "ManyLazy")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class ManyLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "ManyEager")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class ManyEager extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "ManySelect")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.ALL)
	public static class ManySelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "ManySubselect")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class ManySubselect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "OneLazy")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class OneLazy extends Owner {
		@OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "OneEager")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class OneEager extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "OneSelect")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class OneSelect extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "OneSubselect")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class OneSubselect extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SUBSELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id")
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		@JoinColumn(name = "label_id", referencedColumnName = "label_id", insertable = false, updatable = false)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "InverseLazy")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class InverseLazy extends Owner {
		@OneToOne(mappedBy = "label", fetch = FetchType.LAZY, orphanRemoval = true)
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(mappedBy = "label", fetch = FetchType.LAZY)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "InverseEager")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class InverseEager extends Owner {
		@OneToOne(mappedBy = "label", fetch = FetchType.EAGER, orphanRemoval = true)
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(mappedBy = "label", fetch = FetchType.EAGER)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "InverseSelect")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class InverseSelect extends Owner {
		@OneToOne(mappedBy = "label", fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(mappedBy = "label", fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}

	@Entity(name = "InverseSubselect")
	@Table(name = "candidate_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class InverseSubselect extends Owner {
		@OneToOne(mappedBy = "label", fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SUBSELECT)
		@Filter(name = "candidateLanguage", condition = "lang_code = :language")
		Translation filtered;
		@OneToOne(mappedBy = "label", fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
		@SQLRestriction("lang_code = 'en'")
		Translation english;
		@Override public Translation getFiltered() { return filtered; }
		@Override public void setFiltered(Translation target) { filtered = target; }
		@Override public Translation getEnglish() { return english; }
	}
}
