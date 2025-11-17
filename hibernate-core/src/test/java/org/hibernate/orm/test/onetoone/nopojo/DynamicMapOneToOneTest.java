/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.nopojo;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.stat.EntityStatistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/onetoone/nopojo/Person.hbm.xml"
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false"),
		}
)
public class DynamicMapOneToOneTest {

	@Test
	public void testOneToOneOnSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Map person = new HashMap();
					person.put( "name", "Steve" );
					Map address = new HashMap();
					address.put( "zip", "12345" );
					address.put( "state", "TX" );
					address.put( "street", "123 Main St" );

					person.put( "address", address );
					address.put( "owner", person );
					session.persist( "Person", person );
				}
		);

		EntityStatistics addressStats = scope.fromTransaction(
				session -> {
					EntityStatistics aStats = scope.getSessionFactory().getStatistics()
							.getEntityStatistics( "Address" );

					Map person = (Map) session.createQuery( "from Person p join fetch p.address" ).uniqueResult();
					assertNotNull( person, "could not locate person" );
					assertNotNull( person.get( "address" ), "could not locate persons address" );
					session.clear();

					Object[] tuple = (Object[]) session.createQuery(
							"select p.name, p from Person p join fetch p.address" ).uniqueResult();
					assertEquals( tuple.length, 2 );
					person = (Map) tuple[1];
					assertNotNull( person, "could not locate person" );
					assertNotNull( person.get( "address" ), "could not locate persons address" );

					session.remove( person );

					return aStats;
				}
		);

		assertEquals( addressStats.getFetchCount(), 0 );
	}

}
