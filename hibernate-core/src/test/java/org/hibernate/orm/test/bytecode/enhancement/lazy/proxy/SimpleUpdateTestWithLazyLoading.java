/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.MatcherAssert;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				SimpleUpdateTestWithLazyLoading.Parent.class,
				SimpleUpdateTestWithLazyLoading.Child.class,
				SimpleUpdateTestWithLazyLoading.Person.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class SimpleUpdateTestWithLazyLoading  {

	private static final int CHILDREN_SIZE = 10;
	private Long lastChildID;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
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


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createQuery( "delete from Child" ).executeUpdate();
			s.createQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void updateSimpleField(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final String updatedName = "Barrabas_";

		final EntityPersister childPersister = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Child.class.getName() );

		scope.inTransaction(
				session -> {
					stats.clear();
					Child loadedChild = session.load( Child.class, lastChildID );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					MatcherAssert.assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					loadedChild.setName( updatedName );

					// ^ should have triggered "base fetch group" initialization which would mean a SQL select
					assertEquals( 1, stats.getPrepareStatementCount() );

					// check that the `#setName` "persisted"
					assertThat( loadedChild.getName(), is( updatedName ) );
					assertEquals( 1, stats.getPrepareStatementCount() );

					final EntityEntry entry = session.getPersistenceContext().getEntry( loadedChild );
					assertFalse( Hibernate.isInitialized( loadedChild.getRelatives() ) );

					// force a flush - the relatives collection should still be uninitialized afterwards
					session.flush();

					final EntityEntry updatedEntry = session.getPersistenceContext().getEntry( loadedChild );
					assertThat( updatedEntry, sameInstance( entry ) );

					assertFalse( Hibernate.isInitialized( loadedChild.getRelatives() ) );

					session.getEventListenerManager();
				}
		);

		scope.inTransaction(
				session -> {
					Child loadedChild = session.load( Child.class, lastChildID );
					assertThat( loadedChild.getName(), is( updatedName ) );

					assertFalse( Hibernate.isInitialized( loadedChild.getRelatives() ) );
				}
		);

	}

	@Test
	public void testUpdateAssociation(SessionFactoryScope scope) {
		String updatedName = "Barrabas_";
		String parentName = "Yodit";
		scope.inTransaction( s -> {
			final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction( s -> {
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
	public void testUpdateCollection(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Statistics stats = scope.getSessionFactory().getStatistics();
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

		scope.inTransaction( s -> {
			Child loadedChild = s.load( Child.class, lastChildID );
			assertThat( loadedChild.getRelatives().size(), is( 2 ) );
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	static class Parent {

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
	static class Person {
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
	static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
