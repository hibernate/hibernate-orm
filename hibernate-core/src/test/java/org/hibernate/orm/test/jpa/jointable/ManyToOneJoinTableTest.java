/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.jointable;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				Person.class,
				Address.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class ManyToOneJoinTableTest {
	private SQLStatementInspector sqlStatementInspector;

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		sqlStatementInspector = scope.getCollectingStatementInspector();
		sqlStatementInspector.clear();
	}

	@Test
	public void testAvoidJoin(SessionFactoryScope scope) {
		final String queryString = "SELECT e.id FROM Person e";
		scope.inTransaction(
				session -> {
					final List<String> sqlQueries = sqlStatementInspector.getSqlQueries();
					sqlQueries.clear();
					session.createQuery( queryString ).list();
					assertThat( sqlQueries.size(), is( 1 ) );
					// Ideally, we could detect that *ToOne join tables aren't used, but that requires tracking the uses of properties
					// Since *ToOne join tables are treated like secondary or subclass/superclass tables, the proper fix will allow many more optimizations
					String generatedSQl = sqlQueries.get( 0 );
					assertFalse(
							generatedSQl.contains( "join" ),
							"The generated sql contains a useless join: " + generatedSQl
					);
				}
		);
	}
}
