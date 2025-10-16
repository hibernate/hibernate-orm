/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.quote.resultsetmappings;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				MyEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
public class ExplicitSqlResultSetMappingTest {
	private String queryString = null;


	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		char open = dialect.openQuote();
		char close = dialect.closeQuote();
		queryString = "select t." + open + "NAME" + close + " as " + open + "QuotEd_nAMe" + close + " from " + open + "MY_ENTITY_TABLE" + close + " t";
		scope.inTransaction(
				s -> s.persist( new MyEntity( "mine" ) )

		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testCompleteScalarAutoDiscovery(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> s.createNativeQuery( queryString ).list()
		);
	}

	@Test
	public void testPartialScalarAutoDiscovery(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> s.createNativeQuery( queryString, "explicitScalarResultSetMapping" ).list()
		);
	}
}
