/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyonetoone;

import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lazyonetoone/Person.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.MAX_FETCH_DEPTH, value = "2"),
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false")
		}
)
public class LazyOneToOneTest {

	@Test
	public void testLazy(SessionFactoryScope scope) {
		Person person = new Person( "Gavin" );
		Employee e = new Employee( person );
		Employment old = new Employment( e, "IFA" );

		scope.inTransaction(
				session -> {
					Person p2 = new Person( "Emmanuel" );
					new Employment( e, "JBoss" );
					old.setEndDate( new Date() );
					session.persist( person );
					session.persist( p2 );
				}
		);

		scope.inTransaction(
				session -> {
					Person p = (Person) session.createQuery( "from Person where name='Gavin'" ).uniqueResult();
					//assertFalse( Hibernate.isPropertyInitialized(p, "employee") );
					assertSame( p ,p.getEmployee().getPerson() );
					assertTrue( Hibernate.isInitialized( p.getEmployee().getEmployments() ) );
					assertEquals( 1, p.getEmployee().getEmployments().size() );
					Person p2 = (Person) session.createQuery( "from Person where name='Emmanuel'" ).uniqueResult();
					assertNull( p2.getEmployee() );
				}
		);

		scope.inTransaction(
				session -> {
					Person p = session.get( Person.class, "Gavin" );
					//assertFalse( Hibernate.isPropertyInitialized(p, "employee") );
					assertSame( p.getEmployee().getPerson(), p );
					assertTrue( Hibernate.isInitialized( p.getEmployee().getEmployments() ) );
					assertEquals( p.getEmployee().getEmployments().size(), 1 );
					Person p2 =  session.get( Person.class, "Emmanuel" );
					assertNull( p2.getEmployee() );
					session.remove( p2 );
					session.remove( old );
					session.remove( p );
				}
		);
	}

}
