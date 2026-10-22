package org.hibernate.orm.test.querycache;

import java.util.List;
import java.util.function.Supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cfg.CacheSettings;

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
 * The query cache key contains the filter arguments that a
 * {@linkplain ParamDef#resolver() resolver} supplies, and every
 * element of a multivalued filter argument.
 */
@JiraKey("HHH-21020")
@DomainModel(annotatedClasses = QueryCacheWithFilterResolverTest.Document.class)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = CacheSettings.USE_QUERY_CACHE, value = "true"),
		@Setting(name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true")
})
public class QueryCacheWithFilterResolverTest {
	private static final String HQL = "select d.id from Document d order by d.id";

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Document( 1L, 1L ) );
			session.persist( new Document( 2L, 1L ) );
			session.persist( new Document( 3L, 2L ) );
			session.persist( new Document( 4L, 3L ) );
		} );
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		CurrentOrgResolver.CURRENT.remove();
		scope.dropData();
	}

	@Test
	public void resolverSuppliedArgument(SessionFactoryScope scope) {
		assertThat( idsForCurrentOrg( scope, 1L ) ).containsExactly( 1L, 2L );
		assertThat( idsForCurrentOrg( scope, 2L ) ).containsExactly( 3L );
		assertThat( idsForCurrentOrg( scope, 1L ) ).containsExactly( 1L, 2L );
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	public void multivaluedArgument(SessionFactoryScope scope) {
		assertThat( idsForOrgs( scope, List.of( 1L, 2L ) ) ).containsExactly( 1L, 2L, 3L );
		assertThat( idsForOrgs( scope, List.of( 1L, 3L ) ) ).containsExactly( 1L, 2L, 4L );
		assertThat( idsForOrgs( scope, List.of( 1L, 2L ) ) ).containsExactly( 1L, 2L, 3L );
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
	}

	private static List<Long> idsForCurrentOrg(SessionFactoryScope scope, long orgId) {
		CurrentOrgResolver.CURRENT.set( orgId );
		try {
			return scope.fromTransaction( session -> {
				session.enableFilter( "currentOrg" );
				return session.createSelectionQuery( HQL, Long.class ).setCacheable( true ).list();
			} );
		}
		finally {
			CurrentOrgResolver.CURRENT.remove();
		}
	}

	private static List<Long> idsForOrgs(SessionFactoryScope scope, List<Long> orgIds) {
		return scope.fromTransaction( session -> {
			session.enableFilter( "orgs" ).setParameterList( "orgIds", orgIds );
			return session.createSelectionQuery( HQL, Long.class ).setCacheable( true ).list();
		} );
	}

	public static class CurrentOrgResolver implements Supplier<Long> {
		static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

		@Override
		public Long get() {
			return CURRENT.get();
		}
	}

	@FilterDef(name = "currentOrg", defaultCondition = "org_id = :orgId",
			parameters = @ParamDef(name = "orgId", type = Long.class, resolver = CurrentOrgResolver.class))
	@FilterDef(name = "orgs", defaultCondition = "org_id in (:orgIds)",
			parameters = @ParamDef(name = "orgIds", type = Long.class))
	@Filter(name = "currentOrg")
	@Filter(name = "orgs")
	@Entity(name = "Document")
	@Table(name = "query_cache_filter_document")
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
	}
}
