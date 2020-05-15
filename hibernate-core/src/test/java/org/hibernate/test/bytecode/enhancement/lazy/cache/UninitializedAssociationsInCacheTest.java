package org.hibernate.test.bytecode.enhancement.lazy.cache;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.CacheRegionStatistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(BytecodeEnhancerRunner.class)
public class UninitializedAssociationsInCacheTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Employee.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
		configuration.setProperty(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "false");
		configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11766")
	public void attributeLoadingFromCache() {
		final AtomicLong bossId = new AtomicLong();
		final AtomicLong teamleaderId = new AtomicLong();
		final AtomicLong teammemberId = new AtomicLong();

		TransactionUtil.doInHibernate(
				this::sessionFactory, (s) -> {
					Employee boss = new Employee();
					Employee teamleader = new Employee();
					Employee teammember = new Employee();
					boss.regularString = "boss";
					teamleader.regularString = "leader";
					teammember.regularString = "member";
					s.persist( boss );
					s.persist( teamleader );
					s.persist( teammember );
					boss.subordinates.add( teamleader );
					teamleader.superior = boss;
					teamleader.subordinates.add( teammember );
					teammember.superior = teamleader;
					bossId.set( boss.id );
					teamleaderId.set( teamleader.id );
					teammemberId.set( teammember.id );
				}
		);

		sessionFactory().getCache().evictAll();
		sessionFactory().getStatistics().clear();
		CacheRegionStatistics regionStatistics = sessionFactory().getStatistics().getCacheRegionStatistics( "Employee" );

		TransactionUtil.doInHibernate(
				this::sessionFactory, (s) -> {
					final Employee boss = s.find( Employee.class, bossId.get() );
					Assert.assertEquals( "boss", boss.regularString );
					final Employee leader = s.find( Employee.class, teamleaderId.get() );
					Assert.assertEquals( "leader", leader.regularString );
					final Employee member = s.find( Employee.class, teammemberId.get() );
					Assert.assertEquals( "member", member.regularString );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "subordinates" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "subordinates" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
				}
		);

		assertEquals( 0, regionStatistics.getHitCount() );
		assertEquals( 3, regionStatistics.getMissCount() );
		assertEquals( 3, regionStatistics.getPutCount() );

		TransactionUtil.doInHibernate(
				this::sessionFactory, (s) -> {
					final Employee boss = s.find( Employee.class, bossId.get() );
					final Employee leader = s.find( Employee.class, teamleaderId.get() );
					final Employee member = s.find( Employee.class, teammemberId.get() );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "subordinates" ) );

					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
					Assert.assertNull( boss.superior );
					Assert.assertTrue( Hibernate.isPropertyInitialized( boss, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "subordinates" ) );
					Assert.assertEquals( leader, boss.subordinates.iterator().next() );
					Assert.assertTrue( Hibernate.isPropertyInitialized( boss, "subordinates" ) );

					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "subordinates" ) );
					Assert.assertEquals( boss, leader.superior );
					Assert.assertTrue( Hibernate.isPropertyInitialized( leader, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "subordinates" ) );
					Assert.assertEquals( member, leader.subordinates.iterator().next() );
					Assert.assertTrue( Hibernate.isPropertyInitialized( leader, "subordinates" ) );

					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
					Assert.assertEquals( leader, member.superior );
					Assert.assertTrue( Hibernate.isPropertyInitialized( member, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
					Assert.assertTrue( member.subordinates.isEmpty() );
					Assert.assertTrue( Hibernate.isPropertyInitialized( member, "subordinates" ) );
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
	private static class Employee {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "SUPERIOR")
		@LazyToOne( value = LazyToOneOption.NO_PROXY )
		Employee superior;

		@OneToMany(mappedBy = "superior")
		List<Employee> subordinates = new ArrayList<>();

		@Basic
		String regularString;
	}
}
