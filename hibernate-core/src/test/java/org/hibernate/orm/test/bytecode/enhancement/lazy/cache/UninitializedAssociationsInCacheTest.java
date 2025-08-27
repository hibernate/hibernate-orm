/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.CacheRegionStatistics;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.hibernate.cfg.TransactionSettings.ENABLE_LAZY_LOAD_NO_TRANS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				UninitializedAssociationsInCacheTest.Employee.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = ENABLE_LAZY_LOAD_NO_TRANS, value = "false" ),
				@Setting( name = GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory(
		// This test only makes sense if association properties *can* be uninitialized.
		applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
public class UninitializedAssociationsInCacheTest {

	@Test
	@JiraKey("HHH-11766")
	public void attributeLoadingFromCache(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					Employee boss = new Employee( 1, "boss" );
					Employee teamleader = new Employee( 2, "leader" );
					Employee teammember = new Employee( 3, "member" );
					s.persist( boss );
					s.persist( teamleader );
					s.persist( teammember );

					boss.subordinates.add( teamleader );
					teamleader.superior = boss;

					teamleader.subordinates.add( teammember );
					teammember.superior = teamleader;
				}
		);

		scope.getSessionFactory().getCache().evictAll();
		scope.getSessionFactory().getStatistics().clear();
		CacheRegionStatistics regionStatistics = scope.getSessionFactory().getStatistics().getCacheRegionStatistics( "Employee" );

		scope.inTransaction(
				(s) -> {
					final Employee boss = s.find( Employee.class, 1 );
					assertEquals( "boss", boss.regularString );
					final Employee leader = s.find( Employee.class, 2 );
					assertEquals( "leader", leader.regularString );
					final Employee member = s.find( Employee.class, 3 );
					assertEquals( "member", member.regularString );

					assertTrue( isPropertyInitialized( boss, "superior" ) );
					assertTrue( isInitialized( boss.superior ) );
					assertThat( boss.superior, not( instanceOf( HibernateProxy.class ) ) );
					assertFalse( isPropertyInitialized( boss, "subordinates" ) );

					assertTrue( isPropertyInitialized( leader, "superior" ) );
					assertTrue( isInitialized( leader.superior ) );
					assertThat( leader.superior, not( instanceOf( HibernateProxy.class ) ) );
					assertFalse( isPropertyInitialized( leader, "subordinates" ) );

					assertTrue( isPropertyInitialized( member, "superior" ) );
					assertTrue( isInitialized( member.superior ) );
					assertThat( member.superior, not( instanceOf( HibernateProxy.class ) ) );
					assertFalse( isPropertyInitialized( member, "subordinates" ) );
				}
		);

		assertEquals( 0, regionStatistics.getHitCount() );
		assertEquals( 3, regionStatistics.getMissCount() );
		assertEquals( 3, regionStatistics.getPutCount() );

		scope.inTransaction(
				(s) -> {
					final Employee boss = s.find( Employee.class, 1 );
					final Employee leader = s.find( Employee.class, 2 );
					final Employee member = s.find( Employee.class, 3 );
					assertTrue( isPropertyInitialized( boss, "superior" ) );
					assertFalse( isPropertyInitialized( boss, "subordinates" ) );

					assertTrue( isPropertyInitialized( member, "superior" ) );
					assertFalse( isPropertyInitialized( member, "subordinates" ) );
					Assertions.assertNull( boss.superior );

					assertTrue( isPropertyInitialized( boss, "superior" ) );
					assertFalse( isPropertyInitialized( boss, "subordinates" ) );
					assertEquals( leader, boss.subordinates.iterator().next() );
					assertTrue( isPropertyInitialized( boss, "subordinates" ) );

					assertTrue( isPropertyInitialized( leader, "superior" ) );
					assertFalse( isPropertyInitialized( leader, "subordinates" ) );
					assertEquals( boss, leader.superior );
					assertEquals( member, leader.subordinates.iterator().next() );
					assertTrue( isPropertyInitialized( leader, "subordinates" ) );

					assertTrue( isPropertyInitialized( member, "superior" ) );
					assertFalse( isPropertyInitialized( member, "subordinates" ) );
					assertEquals( leader, member.superior );
					assertTrue( member.subordinates.isEmpty() );
					assertTrue( isPropertyInitialized( member, "subordinates" ) );
				}
		);

		assertEquals( 3, regionStatistics.getHitCount() );
		assertEquals( 3, regionStatistics.getMissCount() );
		assertEquals( 3, regionStatistics.getPutCount() );
	}

	@Entity
	@Table(name = "EMPLOYEE_TABLE")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "Employee")
	static class Employee {
		@Id
		Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "SUPERIOR")
		Employee superior;

		@OneToMany(mappedBy = "superior")
		List<Employee> subordinates = new ArrayList<>();

		@Basic
		String regularString;

		private Employee() {
		}

		public Employee(Integer id, String regularString) {
			this.id = id;
			this.regularString = regularString;
		}

		@Override
		public String toString() {
			return String.format( "Employee( %s, %s )", id, regularString ) + "@" + System.identityHashCode( this );
		}
	}
}
