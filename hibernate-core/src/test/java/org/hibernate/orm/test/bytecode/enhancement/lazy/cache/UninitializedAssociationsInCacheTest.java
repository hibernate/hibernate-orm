package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.CacheRegionStatistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(BytecodeEnhancerRunner.class)
public class UninitializedAssociationsInCacheTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Employee.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
		configuration.setProperty(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, false);
		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, true);
	}

	@Override
	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addService(
				SessionFactoryBuilderService.class,
				(SessionFactoryBuilderService) (metadata, bootstrapContext) -> {
					SessionFactoryOptionsBuilder optionsBuilder = new SessionFactoryOptionsBuilder(
							metadata.getMetadataBuildingOptions().getServiceRegistry(),
							bootstrapContext
					);
					// This test only makes sense if association properties *can* be uninitialized.
					optionsBuilder.enableCollectionInDefaultFetchGroup( false );
					return new SessionFactoryBuilderImpl( metadata, optionsBuilder, bootstrapContext );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11766")
	public void attributeLoadingFromCache() {
		inTransaction(
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

		sessionFactory().getCache().evictAll();
		sessionFactory().getStatistics().clear();
		CacheRegionStatistics regionStatistics = sessionFactory().getStatistics().getCacheRegionStatistics( "Employee" );

		inTransaction(
				(s) -> {
					final Employee boss = s.find( Employee.class, 1 );
					Assert.assertEquals( "boss", boss.regularString );
					final Employee leader = s.find( Employee.class, 2 );
					Assert.assertEquals( "leader", leader.regularString );
					final Employee member = s.find( Employee.class, 3 );
					Assert.assertEquals( "member", member.regularString );

					assertTrue( Hibernate.isPropertyInitialized( boss, "superior" ) );
					assertTrue( Hibernate.isInitialized( boss.superior ) );
					assertThat( boss.superior, not( instanceOf( HibernateProxy.class ) ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "subordinates" ) );

					assertTrue( Hibernate.isPropertyInitialized( leader, "superior" ) );
					assertTrue( Hibernate.isInitialized( leader.superior ) );
					assertThat( leader.superior, not( instanceOf( HibernateProxy.class ) ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "subordinates" ) );

					assertTrue( Hibernate.isPropertyInitialized( member, "superior" ) );
					assertTrue( Hibernate.isInitialized( member.superior ) );
					assertThat( member.superior, not( instanceOf( HibernateProxy.class ) ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
				}
		);

		assertEquals( 0, regionStatistics.getHitCount() );
		assertEquals( 3, regionStatistics.getMissCount() );
		assertEquals( 3, regionStatistics.getPutCount() );

		inTransaction(
				(s) -> {
					final Employee boss = s.find( Employee.class, 1 );
					final Employee leader = s.find( Employee.class, 2 );
					final Employee member = s.find( Employee.class, 3 );
					Assert.assertTrue( Hibernate.isPropertyInitialized( boss, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "subordinates" ) );

					Assert.assertTrue( Hibernate.isPropertyInitialized( member, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
					Assert.assertNull( boss.superior );

					assertTrue( Hibernate.isPropertyInitialized( boss, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( boss, "subordinates" ) );
					Assert.assertEquals( leader, boss.subordinates.iterator().next() );
					assertTrue( Hibernate.isPropertyInitialized( boss, "subordinates" ) );

					Assert.assertTrue( Hibernate.isPropertyInitialized( leader, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( leader, "subordinates" ) );
					Assert.assertEquals( boss, leader.superior );
					Assert.assertEquals( member, leader.subordinates.iterator().next() );
					assertTrue( Hibernate.isPropertyInitialized( leader, "subordinates" ) );

					Assert.assertTrue( Hibernate.isPropertyInitialized( member, "superior" ) );
					Assert.assertFalse( Hibernate.isPropertyInitialized( member, "subordinates" ) );
					Assert.assertEquals( leader, member.superior );
					assertTrue( member.subordinates.isEmpty() );
					assertTrue( Hibernate.isPropertyInitialized( member, "subordinates" ) );
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
		Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "SUPERIOR")
		@LazyToOne( value = LazyToOneOption.NO_PROXY )
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
