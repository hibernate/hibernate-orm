/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hamcrest.MatcherAssert;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class SimpleUpdateTestWithLazyLoading extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	private static final int CHILDREN_SIZE = 10;
	private Long lastChildID;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class, Person.class };
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child();
				// Association management should kick in here
				child.parent = parent;

				Person relative = new Person();
				relative.setName( "Luigi" );
				child.addRelative( relative );

				s.persist( relative );

				s.persist( child );
				lastChildID = child.id;
			}
			s.persist( parent );
		} );
	}


	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "delete from Child" ).executeUpdate();
			s.createQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void updateSimpleField() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final String updatedName = "Barrabas_";

		final EntityPersister childPersister = sessionFactory().getMetamodel().entityPersister( Child.class.getName() );

		final int relativesAttributeIndex = childPersister.getEntityMetamodel().getPropertyIndex( "relatives" );

		inTransaction(
				session -> {
					stats.clear();
					Child loadedChild = session.load( Child.class, lastChildID );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					MatcherAssert.assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );
					final EnhancementAsProxyLazinessInterceptor proxyInterceptor = (EnhancementAsProxyLazinessInterceptor) interceptor;

					loadedChild.setName( updatedName );

					// ^ should have triggered "base fetch group" initialization which would mean a SQL select
					assertEquals( 1, stats.getPrepareStatementCount() );

					// check that the `#setName` "persisted"
					assertThat( loadedChild.getName(), is( updatedName ) );
					assertEquals( 1, stats.getPrepareStatementCount() );

					final EntityEntry entry = session.getPersistenceContext().getEntry( loadedChild );
					assertThat(
							entry.getLoadedState()[ relativesAttributeIndex ],
							is( LazyPropertyInitializer.UNFETCHED_PROPERTY )
					);

					// force a flush - the relatives collection should still be UNFETCHED_PROPERTY afterwards
					session.flush();

					final EntityEntry updatedEntry = session.getPersistenceContext().getEntry( loadedChild );
					assertThat( updatedEntry, sameInstance( entry ) );

					assertThat(
							entry.getLoadedState()[ relativesAttributeIndex ],
							is( LazyPropertyInitializer.UNFETCHED_PROPERTY )
					);

					session.getEventListenerManager();
				}
		);

		inTransaction(
				session -> {
					Child loadedChild = session.load( Child.class, lastChildID );
					assertThat( loadedChild.getName(), is( updatedName ) );

					final EntityEntry entry = session.getPersistenceContext().getEntry( loadedChild );
					assertThat(
							entry.getLoadedState()[ relativesAttributeIndex ],
							is( LazyPropertyInitializer.UNFETCHED_PROPERTY )
					);
				}
		);

	}

	@Test
	public void testUpdateAssociation() {
		String updatedName = "Barrabas_";
		String parentName = "Yodit";
		doInHibernate( this::sessionFactory, s -> {
			final Statistics stats = sessionFactory().getStatistics();
			stats.clear();
			Child loadedChild = s.load( Child.class, lastChildID );

			loadedChild.setName( updatedName );

			Parent parent = new Parent();
			parent.setName( parentName );

			assertEquals( 1, stats.getPrepareStatementCount() );
			loadedChild.setParent( parent );
			assertEquals( 1, stats.getPrepareStatementCount() );
			assertThat( loadedChild.getParent().getName(), is( parentName ) );
			assertEquals( 1, stats.getPrepareStatementCount() );
			s.save( parent );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Child loadedChild = s.load( Child.class, lastChildID );
			assertThat( Hibernate.isInitialized( loadedChild ), is( false ) );
			assertThat( loadedChild.getName(), is( updatedName ) );
			assertThat( Hibernate.isInitialized( loadedChild ), is( true ) );
			assertThat( Hibernate.isInitialized( loadedChild.getParent() ), is( false ) );
			assertThat( loadedChild.getParent().getName(), is( parentName ) );
			assertThat( Hibernate.isInitialized( loadedChild.getParent() ), is( true ) );
		} );
	}

	@Test
	public void testUpdateCollection() {
		doInHibernate( this::sessionFactory, s -> {
			final Statistics stats = sessionFactory().getStatistics();
			stats.clear();
			Child loadedChild = s.load( Child.class, lastChildID );


			assertEquals( 0, stats.getPrepareStatementCount() );
			Person relative = new Person();
			relative.setName( "Luis" );
			assertThat( Hibernate.isInitialized( loadedChild ), is( false ) );
			loadedChild.addRelative( relative );
			assertThat( Hibernate.isInitialized( loadedChild ), is( true ) );
			assertEquals( 2, stats.getPrepareStatementCount() );
			s.persist( relative );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Child loadedChild = s.load( Child.class, lastChildID );
			assertThat( loadedChild.getRelatives().size(), is( 2 ) );
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	private static class Parent {

		String name;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		List<Child> children;

		void setChildren(List<Child> children) {
			this.children = children;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	private static class Person {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	private static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		Parent parent;

		@OneToMany
		List<Person> relatives;

		String name;

		Child() {
		}

		Child(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public List<Person> getRelatives() {
			return relatives;
		}

		public void setRelatives(List<Person> relatives) {
			this.relatives = relatives;
		}

		public void addRelative(Person person) {
			if ( this.relatives == null ) {
				this.relatives = new ArrayList<>();
			}
			this.relatives.add( person );
		}
	}
}