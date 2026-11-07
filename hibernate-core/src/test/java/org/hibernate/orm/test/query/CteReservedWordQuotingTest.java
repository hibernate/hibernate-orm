package org.hibernate.orm.test.query;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ServiceRegistry(settings = @Setting(name = MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED, value = "true"))
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-20650")
public class CteReservedWordQuotingTest {

	@Test
	public void testReservedWordCteNameIsQuoted(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final String quotedName = dialect.openQuote() + "element" + dialect.closeQuote();
		final CollectingStatementObserver inspector = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			inspector.clear();
			session.createQuery(
					"with element as (" +
							"select c.id id, c.alternativeContact.id altId from Contact c where c.id = 1 " +
							"union all " +
							"select c.id id, c.alternativeContact.id altId from element e join Contact c on e.altId = c.id" +
							") select e.id from element e",
					Tuple.class
			).getResultList();
			final String sql = inspector.getSqlQueries().get( 0 );
			assertTrue(
					sql.contains( quotedName ),
					"CTE name that is a reserved word should be quoted, but was: " + sql
			);
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21088")
	public void testReservedWordCteColumnIsQuoted(SessionFactoryScope scope) {
		assertReservedWordColumnQuoted(
				scope,
				"with cte as (" +
						"select c.id value, c.alternativeContact.id altId from Contact c where c.id = 1 " +
						"union all " +
						"select c.id value, c.alternativeContact.id altId from cte e join Contact c on e.altId = c.id" +
						") select e.value from cte e"
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21088")
	public void testReservedWordNonRecursiveCteColumnIsQuoted(SessionFactoryScope scope) {
		assertReservedWordColumnQuoted(
				scope,
				"with cte as (select c.id value from Contact c) select e.value from cte e"
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21088")
	public void testReservedWordCteSearchColumnIsQuoted(SessionFactoryScope scope) {
		assertReservedWordColumnQuoted(
				scope,
				"with cte as (" +
						"select c.id value, c.alternativeContact.id altId from Contact c where c.id = 1 " +
						"union all " +
						"select c.id value, c.alternativeContact.id altId from cte e join Contact c on e.altId = c.id" +
						") search breadth first by value set orderAttr " +
						"select e.value from cte e order by e.orderAttr"
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21088")
	public void testReservedWordCteCycleColumnIsQuoted(SessionFactoryScope scope) {
		assertReservedWordColumnQuoted(
				scope,
				"with cte as (" +
						"select c.id value, c.alternativeContact.id altId from Contact c where c.id = 1 " +
						"union all " +
						"select c.id value, c.alternativeContact.id altId from cte e join Contact c on e.altId = c.id" +
						") cycle value set isCycle to true default false " +
						"select e.value from cte e"
		);
	}

	private static void assertReservedWordColumnQuoted(SessionFactoryScope scope, String hql) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final String quotedName = dialect.openQuote() + "value" + dialect.closeQuote();
		final CollectingStatementObserver inspector = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			inspector.clear();
			session.createQuery( hql, Tuple.class ).getResultList();
			final String sql = inspector.getSqlQueries().get( 0 );
			assertTrue(
					sql.contains( quotedName ),
					"CTE column that is a reserved word should be quoted, but was: " + sql
			);
		} );
	}
}
