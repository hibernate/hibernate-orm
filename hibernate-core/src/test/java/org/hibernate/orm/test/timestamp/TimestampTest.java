/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;

import java.util.Date;

import org.hibernate.cfg.Environment;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/timestamp/User.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.GENERATE_STATISTICS,value = "true")
)
public class TimestampTest {

	@Test
	public void testUpdateFalse(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				session -> {
					User u = new User( "gavin", "secret", new Person( "Gavin King", new Date(), "Karbarook Ave" ) );
					session.persist( u );
					session.flush();
					u.getPerson().setName( "XXXXYYYYY" );
				}
		);

		assertEquals( 1, statistics.getEntityInsertCount() );
		assertEquals( 0, statistics.getEntityUpdateCount() );

		scope.inTransaction(
				session -> {
					User u = session.get( User.class, "gavin" );
					assertEquals( u.getPerson().getName(), "Gavin King" );
					session.remove( u );
				}
		);

		assertEquals( 1, statistics.getEntityDeleteCount() );
	}

	@Test
	public void testComponent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User u = new User( "gavin", "secret", new Person( "Gavin King", new Date(), "Karbarook Ave" ) );
					session.persist( u );
					session.flush();
					u.getPerson().setCurrentAddress( "Peachtree Rd" );
				}
		);

		scope.inTransaction(
				session -> {
					User u = session.get( User.class, "gavin" );
					u.setPassword( "$ecret" );
				}
		);

		scope.inTransaction(
				session -> {
					User u = session.get( User.class, "gavin" );
					assertEquals( u.getPassword(), "$ecret" );
					session.remove( u );
				}
		);
	}

}
