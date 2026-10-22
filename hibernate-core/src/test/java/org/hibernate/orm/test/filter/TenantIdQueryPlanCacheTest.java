package org.hibernate.orm.test.filter;

import java.util.List;
import java.util.function.Function;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;

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
 * The {@code _tenantId} filter of {@link TenantId} does not prevent
 * caching of select plans, and each tenant sees only its own rows.
 */
@JiraKey("HHH-21021")
@DomainModel(annotatedClasses = TenantIdQueryPlanCacheTest.Record.class)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = @Setting(name = MultiTenancySettings.MULTI_TENANT_RLS_ENABLED, value = "false"))
public class TenantIdQueryPlanCacheTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		inTenant( scope, "a", session -> {
			session.persist( new Record( 1L, "a1" ) );
			session.persist( new Record( 2L, "a2" ) );
			return null;
		} );
		inTenant( scope, "b", session -> {
			session.persist( new Record( 3L, "b1" ) );
			return null;
		} );
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void tenantsShareThePlan(SessionFactoryScope scope) {
		final String hql = "select r.name from Record r order by r.id";
		final QueryInterpretationCache cache = scope.getSessionFactory().getQueryEngine().getInterpretationCache();
		final var statistics = scope.getSessionFactory().getStatistics();

		final int plansBefore = cache.getNumberOfCachedQueryPlans();
		assertThat( names( scope, "a", hql ) ).containsExactly( "a1", "a2" );
		assertThat( cache.getNumberOfCachedQueryPlans() ).isEqualTo( plansBefore + 1 );

		final long hitsBefore = statistics.getQueryPlanCacheHitCount();
		assertThat( names( scope, "b", hql ) ).containsExactly( "b1" );
		assertThat( cache.getNumberOfCachedQueryPlans() ).isEqualTo( plansBefore + 1 );
		// one hit for the HQL interpretation and one for the select plan
		assertThat( statistics.getQueryPlanCacheHitCount() - hitsBefore ).isEqualTo( 2 );

		assertThat( names( scope, "a", hql ) ).containsExactly( "a1", "a2" );
		assertThat( names( scope, "c", hql ) ).isEmpty();
	}

	private static List<String> names(SessionFactoryScope scope, String tenant, String hql) {
		return inTenant( scope, tenant, session -> session.createQuery( hql, String.class ).list() );
	}

	private static <T> T inTenant(SessionFactoryScope scope, String tenant, Function<SessionImplementor, T> action) {
		try ( var session = (SessionImplementor) scope.getSessionFactory().withOptions().tenantIdentifier( tenant ).openSession() ) {
			session.getTransaction().begin();
			try {
				final T result = action.apply( session );
				session.getTransaction().commit();
				return result;
			}
			catch (RuntimeException e) {
				session.getTransaction().rollback();
				throw e;
			}
		}
	}

	@Entity(name = "Record")
	@Table(name = "tenant_plan_record")
	public static class Record {
		@Id
		private Long id;
		@TenantId
		private String tenant;
		private String name;

		protected Record() {
		}

		public Record(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
