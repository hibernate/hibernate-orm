/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ternary;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = USE_SECOND_LEVEL_CACHE, value = "false"))
@DomainModel(xmlMappings = "mappings/map/Ternary.hbm.xml")
@SessionFactory
public class TernaryTest {
	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	public void testTernary(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var bob = new Employee("Bob");
			var tom = new Employee("Tom");
			var jim = new Employee("Jim");
			var tim = new Employee("Tim");
			var melb = new Site("Melbourne");
			var geel = new Site("Geelong");
			session.persist(bob);
			session.persist(tom);
			session.persist(jim);
			session.persist(tim);
			session.persist(melb);
			session.persist(geel);
			bob.getManagerBySite().put(melb, tom);
			bob.getManagerBySite().put(geel, jim);
			tim.getManagerBySite().put(melb, tom);
		} );

		factoryScope.inTransaction( (session) -> {
			var tom = session.find( Employee.class, "Tom" );
			assertFalse( Hibernate.isInitialized(tom.getUnderlings()) );
			assertEquals( 2, tom.getUnderlings().size() );

			var bob = session.find( Employee.class, "Bob" );
			assertFalse( Hibernate.isInitialized(bob.getManagerBySite()) );
			assertTrue( tom.getUnderlings().contains(bob) );

			var melb = session.find( Site.class, "Melbourne" );
			assertSame( bob.getManagerBySite().get(melb), tom );
			assertTrue( melb.getEmployees().contains(bob) );
			assertTrue( melb.getManagers().contains(tom) );

		} );

		factoryScope.inTransaction( (session) -> {
			var qry = """
					from Employee e
						join e.managerBySite m
					where m.name='Bob'
					""";
			List<Object[]> l = session.createQuery( qry, Object[].class).list();
			assertEquals( 0, l.size() );

			qry = """
				from Employee e
					join e.managerBySite m
				where m.name='Tom'
				""";
			l = session.createQuery(qry, Object[].class).list();
			assertEquals( 2, l.size() );
		} );

		factoryScope.inTransaction( (session) -> {
			var qry = "from Employee e left join fetch e.managerBySite";
			List<Employee> l = session.createQuery( qry, Employee.class ).list();
			assertEquals( 4, l.size() );
			Set<Employee> set = new HashSet<>(l);
			assertEquals( 4, set.size() );
			Iterator<Employee> iter = set.iterator();
			int total=0;
			while ( iter.hasNext() ) {
				Map<Site,Employee> map = iter.next().getManagerBySite();
				assertTrue( Hibernate.isInitialized(map) );
				total += map.size();
			}
			assertEquals( 3, total );
		} );
	}

	@Test
	public void testIndexRelatedFunctions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Employee e join e.managerBySite as m where index(m) is not null", Object[].class )
					.list();
			session.createQuery( "from Employee e where minIndex(e.managerBySite) is not null" )
					.list();
			session.createQuery( "from Employee e where maxIndex(e.managerBySite) is not null" )
					.list();
		} );
	}
}
