/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-20650")
public class CteReservedWordQuotingTest {

	@Test
	public void testReservedWordCteNameIsQuoted(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final String quotedName = dialect.openQuote() + "element" + dialect.closeQuote();
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
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
}
