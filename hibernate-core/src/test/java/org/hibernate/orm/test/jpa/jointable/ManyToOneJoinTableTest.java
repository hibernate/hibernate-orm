/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.jointable;

import java.util.LinkedList;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

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
@SessionFactory
public class ManyToOneJoinTableTest implements SessionFactoryProducer {
	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
		sqlStatementInterceptor = new SQLStatementInterceptor( sessionFactoryBuilder );
		return (SessionFactoryImplementor) sessionFactoryBuilder.build();
	}

	@Test
	public void testAvoidJoin(SessionFactoryScope scope) {
		final String queryString = "SELECT e.id FROM Person e";
		scope.inTransaction(
				session -> {
					final LinkedList<String> sqlQueries = sqlStatementInterceptor.getSqlQueries();
					sqlQueries.clear();
					session.createQuery( queryString ).list();
					assertThat( sqlQueries.size(), is( 1 ) );
					// Ideally, we could detect that *ToOne join tables aren't used, but that requires tracking the uses of properties
					// Since *ToOne join tables are treated like secondary or subclass/superclass tables, the proper fix will allow many more optimizations
					String generatedSQl = sqlQueries.getFirst();
					assertFalse(
							"The generated sql contains a useless join: " + generatedSQl,
							generatedSQl.contains( "join" )
					);
				}
		);
	}
}
