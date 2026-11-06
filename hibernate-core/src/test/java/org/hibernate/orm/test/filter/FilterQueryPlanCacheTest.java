package org.hibernate.orm.test.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Select plans are cached while filters are enabled, and filter parameter
 * values are bound from the session that executes the plan.
 */
@JiraKey("HHH-21021")
@DomainModel(annotatedClasses = {
		FilterQueryPlanCacheTest.Item.class,
		FilterQueryPlanCacheTest.Detail.class,
		FilterQueryPlanCacheTest.Document.class
})
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = QuerySettings.CRITERIA_PLAN_CACHE_ENABLED, value = "true"),
		@Setting(name = JdbcSettings.POOL_SIZE, value = "10")
})
public class FilterQueryPlanCacheTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			long detailId = 1;
			for ( long id = 1; id <= 6; id++ ) {
				final Item item = new Item( id, ( id + 1 ) / 2 );
				session.persist( item );
				session.persist( new Detail( detailId++, item ) );
				session.persist( new Detail( detailId++, item ) );
			}
			for ( long id = 1; id <= 4; id++ ) {
				session.persist( new Document( id, ( id + 1 ) / 2 ) );
			}
		} );
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void sameHqlDifferentFilterValuesSharesPlan(SessionFactoryScope scope) {
		final String hql = "from Item i1 order by i1.id";
		final PlanCacheProbe probe = new PlanCacheProbe( scope );

		assertThat( itemIds( scope, hql, 1L ) ).containsExactly( 1L, 2L );
		probe.assertPlanStored( 1 );

		assertThat( itemIds( scope, hql, 2L ) ).containsExactly( 3L, 4L );
		probe.assertPlanReused();

		assertThat( itemIds( scope, hql, 3L ) ).containsExactly( 5L, 6L );
		probe.assertPlanReused();
	}

	@Test
	public void filterEnabledInOneSessionOnly(SessionFactoryScope scope) {
		final String hql = "from Item i2 order by i2.id";
		final PlanCacheProbe probe = new PlanCacheProbe( scope );

		assertThat( itemIds( scope, hql, 2L ) ).containsExactly( 3L, 4L );
		probe.assertPlanStored( 1 );

		final List<Long> unfiltered = scope.fromSession(
				session -> ids( session.createQuery( hql, Item.class ).list(), Item::getId )
		);
		assertThat( unfiltered ).containsExactly( 1L, 2L, 3L, 4L, 5L, 6L );
		probe.assertPlanStored( 1 );

		assertThat( itemIds( scope, hql, 1L ) ).containsExactly( 1L, 2L );
		probe.assertPlanReused();
	}

	@Test
	public void iterableFilterParameterWithDifferentElementCounts(SessionFactoryScope scope) {
		final String hql = "from Item i3 order by i3.id";
		final PlanCacheProbe probe = new PlanCacheProbe( scope );

		assertThat( itemIdsForOrgs( scope, hql, 1L, 2L ) ).containsExactly( 1L, 2L, 3L, 4L );
		probe.assertPlanStored( 1 );

		assertThat( itemIdsForOrgs( scope, hql, 3L ) ).containsExactly( 5L, 6L );
		probe.assertPlanStored( 1 );

		assertThat( itemIdsForOrgs( scope, hql, 2L, 3L ) ).containsExactly( 3L, 4L, 5L, 6L );
		probe.assertPlanReused();

		assertThat( itemIdsForOrgs( scope, hql, 1L ) ).containsExactly( 1L, 2L );
		probe.assertPlanReused();
	}

	@Test
	public void criteriaWithPlanCacheEnabled(SessionFactoryScope scope) {
		final Function<SessionImplementor, List<Item>> criteria = session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Item.class );
			final var root = query.from( Item.class );
			query.select( root ).where( builder.greaterThan( root.get( "id" ), 0L ) ).orderBy( builder.asc( root.get( "id" ) ) );
			return session.createQuery( query ).list();
		};
		final PlanCacheProbe probe = new PlanCacheProbe( scope );

		final List<Long> first = scope.fromSession( session -> {
			session.enableFilter( "org" ).setParameter( "orgId", 1L );
			return ids( criteria.apply( session ), Item::getId );
		} );
		assertThat( first ).containsExactly( 1L, 2L );
		probe.assertPlanStored( 1 );

		final List<Long> second = scope.fromSession( session -> {
			session.enableFilter( "org" ).setParameter( "orgId", 3L );
			return ids( criteria.apply( session ), Item::getId );
		} );
		assertThat( second ).containsExactly( 5L, 6L );
		probe.assertCriteriaPlanReused();
	}

	@Test
	public void resolverSuppliedParameterValue(SessionFactoryScope scope) {
		final String hql = "from Document d1 order by d1.id";
		final PlanCacheProbe probe = new PlanCacheProbe( scope );

		assertThat( documentIds( scope, hql, 1L ) ).containsExactly( 1L, 2L );
		probe.assertPlanStored( 1 );

		assertThat( documentIds( scope, hql, 2L ) ).containsExactly( 3L, 4L );
		probe.assertPlanReused();
	}

	@Test
	public void concurrentExecutionsWithDifferentValues(SessionFactoryScope scope) throws Exception {
		final String itemHql = "from Item i4 order by i4.id";
		final String documentHql = "from Document d2 order by d2.id";
		final int threads = 6;
		final int iterations = 50;
		final ExecutorService executor = Executors.newFixedThreadPool( threads );
		try {
			final List<Future<Integer>> futures = new ArrayList<>();
			for ( int t = 0; t < threads; t++ ) {
				final long itemOrg = t % 3 + 1;
				final long documentOrg = t % 2 + 1;
				futures.add( executor.submit( (Callable<Integer>) () -> {
					int checked = 0;
					for ( int i = 0; i < iterations; i++ ) {
						final List<Long> itemOrgs = scope.fromSession( session -> {
							session.enableFilter( "org" ).setParameter( "orgId", itemOrg );
							return ids( session.createQuery( itemHql, Item.class ).list(), Item::getOrgId );
						} );
						assertThat( itemOrgs ).hasSize( 2 ).containsOnly( itemOrg );
						final List<Long> documentOrgs = withCurrentOrg( documentOrg, () -> scope.fromSession(
								session -> ids( session.createQuery( documentHql, Document.class ).list(), Document::getOrgId )
						) );
						assertThat( documentOrgs ).hasSize( 2 ).containsOnly( documentOrg );
						checked += itemOrgs.size() + documentOrgs.size();
					}
					return checked;
				} ) );
			}
			for ( Future<Integer> future : futures ) {
				assertThat( future.get( 2, TimeUnit.MINUTES ) ).isEqualTo( iterations * 4 );
			}
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void subselectFetchUsesQueryTimeFilterValues(SessionFactoryScope scope) {
		final String hql = "from Item i5 order by i5.id";
		// Prime the plan cache with another value first.
		assertThat( itemIds( scope, hql, 3L ) ).containsExactly( 5L, 6L );

		scope.inSession( session -> {
			final var filter = session.enableFilter( "org" ).setParameter( "orgId", 1L );
			final List<Item> items = session.createQuery( hql, Item.class ).list();
			assertThat( ids( items, Item::getId ) ).containsExactly( 1L, 2L );

			// Subselect fetching repeats the restriction of the original query.
			filter.setParameter( "orgId", 2L );
			assertThat( Hibernate.isInitialized( items.get( 0 ).getDetails() ) ).isFalse();
			assertThat( items.get( 0 ).getDetails() ).hasSize( 2 );
			assertThat( items.get( 1 ).getDetails() ).hasSize( 2 );
		} );

		scope.inSession( session -> {
			session.enableFilter( "org" ).setParameter( "orgId", 2L );
			final List<Item> items = session.createQuery( hql, Item.class ).list();
			assertThat( ids( items, Item::getId ) ).containsExactly( 3L, 4L );

			session.disableFilter( "org" );
			assertThat( items.get( 0 ).getDetails() ).hasSize( 2 );
			assertThat( items.get( 1 ).getDetails() ).hasSize( 2 );
		} );
	}

	private static List<Long> itemIds(SessionFactoryScope scope, String hql, long orgId) {
		return scope.fromSession( session -> {
			session.enableFilter( "org" ).setParameter( "orgId", orgId );
			return ids( session.createQuery( hql, Item.class ).list(), Item::getId );
		} );
	}

	private static List<Long> itemIdsForOrgs(SessionFactoryScope scope, String hql, Long... orgIds) {
		return scope.fromSession( session -> {
			session.enableFilter( "orgs" ).setParameterList( "orgIds", orgIds );
			return ids( session.createQuery( hql, Item.class ).list(), Item::getId );
		} );
	}

	private static List<Long> documentIds(SessionFactoryScope scope, String hql, long orgId) {
		return withCurrentOrg( orgId, () -> scope.fromSession(
				session -> ids( session.createQuery( hql, Document.class ).list(), Document::getId )
		) );
	}

	private static <T> T withCurrentOrg(long orgId, Supplier<T> action) {
		CurrentOrgResolver.CURRENT.set( orgId );
		try {
			return action.get();
		}
		finally {
			CurrentOrgResolver.CURRENT.remove();
		}
	}

	private static <E> List<Long> ids(List<E> entities, Function<E, Long> extractor) {
		return entities.stream().map( extractor ).toList();
	}

	private static class PlanCacheProbe {
		private final QueryInterpretationCache cache;
		private final Statistics statistics;
		private int cachedPlans;
		private long hits;

		PlanCacheProbe(SessionFactoryScope scope) {
			cache = scope.getSessionFactory().getQueryEngine().getInterpretationCache();
			statistics = scope.getSessionFactory().getStatistics();
			mark();
		}

		private void mark() {
			cachedPlans = cache.getNumberOfCachedQueryPlans();
			hits = statistics.getQueryPlanCacheHitCount();
		}

		void assertPlanStored(int newPlans) {
			assertThat( cache.getNumberOfCachedQueryPlans() - cachedPlans ).isEqualTo( newPlans );
			mark();
		}

		/**
		 * One hit for the HQL interpretation and one for the select plan.
		 */
		void assertPlanReused() {
			assertThat( cache.getNumberOfCachedQueryPlans() ).isEqualTo( cachedPlans );
			assertThat( statistics.getQueryPlanCacheHitCount() - hits ).isEqualTo( 2 );
			mark();
		}

		/**
		 * Criteria queries have no HQL interpretation, so only the select plan hits.
		 */
		void assertCriteriaPlanReused() {
			assertThat( cache.getNumberOfCachedQueryPlans() ).isEqualTo( cachedPlans );
			assertThat( statistics.getQueryPlanCacheHitCount() - hits ).isEqualTo( 1 );
			mark();
		}
	}

	public static class CurrentOrgResolver implements Supplier<Long> {
		static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

		@Override
		public Long get() {
			return CURRENT.get();
		}
	}

	@FilterDef(name = "org", defaultCondition = "org_id = :orgId",
			parameters = @ParamDef(name = "orgId", type = Long.class))
	@FilterDef(name = "orgs", defaultCondition = "org_id in (:orgIds)",
			parameters = @ParamDef(name = "orgIds", type = Long.class))
	@Filter(name = "org")
	@Filter(name = "orgs")
	@Entity(name = "Item")
	@Table(name = "filter_plan_item")
	public static class Item {
		@Id
		private Long id;
		@Column(name = "org_id")
		private Long orgId;
		@OneToMany(mappedBy = "item")
		@Fetch(FetchMode.SUBSELECT)
		private List<Detail> details = new ArrayList<>();

		protected Item() {
		}

		public Item(Long id, Long orgId) {
			this.id = id;
			this.orgId = orgId;
		}

		public Long getId() {
			return id;
		}

		public Long getOrgId() {
			return orgId;
		}

		public List<Detail> getDetails() {
			return details;
		}
	}

	@Entity(name = "Detail")
	@Table(name = "filter_plan_detail")
	public static class Detail {
		@Id
		private Long id;
		@ManyToOne
		private Item item;

		protected Detail() {
		}

		public Detail(Long id, Item item) {
			this.id = id;
			this.item = item;
		}
	}

	@FilterDef(name = "currentOrg", defaultCondition = "org_id = :orgId", autoEnabled = true,
			parameters = @ParamDef(name = "orgId", type = Long.class, resolver = CurrentOrgResolver.class))
	@Filter(name = "currentOrg")
	@Entity(name = "Document")
	@Table(name = "filter_plan_document")
	public static class Document {
		@Id
		private Long id;
		@Column(name = "org_id")
		private Long orgId;

		protected Document() {
		}

		public Document(Long id, Long orgId) {
			this.id = id;
			this.orgId = orgId;
		}

		public Long getId() {
			return id;
		}

		public Long getOrgId() {
			return orgId;
		}
	}
}
