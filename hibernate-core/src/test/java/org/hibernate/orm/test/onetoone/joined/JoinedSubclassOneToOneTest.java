/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.joined;

import java.util.List;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.EntityStatistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/onetoone/joined/Person.hbm.xml"
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = @Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false")
)
public class JoinedSubclassOneToOneTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneOnSubclass(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.name = "Gavin";
					Address a = new Address();
					a.entityName = "Gavin";
					a.zip = "3181";
					a.state = "VIC";
					a.street = "Karbarook Ave";
					p.address = a;
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
					EntityStatistics addressStats = sessionFactory.getStatistics()
							.getEntityStatistics( Address.class.getName() );
					EntityStatistics mailingAddressStats = sessionFactory.getStatistics().getEntityStatistics(
							"MailingAddress" );

					Person p = (Person) session.createQuery(
							"from Person p join fetch p.address left join fetch p.mailingAddress" )
							.uniqueResult();
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					p = (Person) session.createQuery(
							"select p from Person p join fetch p.address left join fetch p.mailingAddress" )
							.uniqueResult();
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					Object[] stuff = (Object[]) session.createQuery(
							"select p.name, p from Person p join fetch p.address left join fetch p.mailingAddress" )
							.uniqueResult();
					assertEquals( 2, stuff.length );
					p = (Person) stuff[1];
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					assertEquals( 0, addressStats.getFetchCount() );
					assertEquals( 0, mailingAddressStats.getFetchCount() );

					p = (Person) session.createQuery( "from Person p join fetch p.address" ).uniqueResult();
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					assertEquals( 0, addressStats.getFetchCount() );
					assertEquals( 1, mailingAddressStats.getFetchCount() );

					p = (Person) session.createQuery( "from Person" ).uniqueResult();
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					assertEquals( 0, addressStats.getFetchCount() );
					assertEquals( 2, mailingAddressStats.getFetchCount() );

					p = (Person) session.createQuery( "from Entity" ).uniqueResult();
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					assertEquals( 0, addressStats.getFetchCount() );
					assertEquals( 3, mailingAddressStats.getFetchCount() );

					//note that in here join fetch is used for the nullable
					//one-to-one, due to a very special case of default
					p = session.get( Person.class, "Gavin" );
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					assertEquals( 0, addressStats.getFetchCount() );
					assertEquals( 3, mailingAddressStats.getFetchCount() );

					p = (Person) session.get( Entity.class, "Gavin" );
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					session.clear();

					assertEquals( 0, addressStats.getFetchCount() );
					assertEquals( 3, mailingAddressStats.getFetchCount() );
				}
		);

		Address a2 = new Address();
		scope.inTransaction(
				session -> {
					Org org = new Org();
					org.name = "IFA";
					a2.entityName = "IFA";
					a2.zip = "3181";
					a2.state = "VIC";
					a2.street = "Orrong Rd";
					session.persist( org );
					session.persist( a2 );
				}
		);

		scope.inTransaction(
				session -> {
					session.get( Entity.class, "IFA" );
					session.clear();

					List list = session.createQuery( "from Entity e order by e.name" ).list();
					Person p = (Person) list.get( 0 );
					assertNotNull( p.address );
					assertNull( p.mailingAddress );
					list.get( 1 );
					session.clear();

					list = session.createQuery(
							"from Entity e left join fetch e.address left join fetch e.mailingAddress order by e.name" )
							.list();
					p = (Person) list.get( 0 );
					Org org = (Org) list.get( 1 );
					assertNotNull( p.address );
					assertNull( p.mailingAddress );

					session.clear();
					session.remove( p );
					session.remove( p.address );
					session.remove( org );
					session.remove( a2 );
					session.flush();
				}
		);
	}

}
