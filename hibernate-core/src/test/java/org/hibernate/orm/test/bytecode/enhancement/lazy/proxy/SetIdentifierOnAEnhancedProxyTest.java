/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@JiraKey("HHH-11147")
@DomainModel(
		annotatedClasses = {
				SetIdentifierOnAEnhancedProxyTest.Parent.class,
				SetIdentifierOnAEnhancedProxyTest.Child.class,
				SetIdentifierOnAEnhancedProxyTest.Person.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class SetIdentifierOnAEnhancedProxyTest {

	private static final int CHILDREN_SIZE = 10;
	private Long lastChildID;

	@Test
	public void setIdTest(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inTransaction(
				session -> {
					stats.clear();
					Child loadedChild = session.getReference( Child.class, lastChildID );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					assertThat( interceptor ).isInstanceOf( EnhancementAsProxyLazinessInterceptor.class );

					loadedChild.setId( lastChildID );

					assertEquals( 0, stats.getPrepareStatementCount() );

					assertThat( loadedChild.getId() ).isEqualTo( lastChildID );
					assertEquals( 0, stats.getPrepareStatementCount() );
				}
		);

		scope.inTransaction(
				session -> {
					Child loadedChild = session.getReference( Child.class, lastChildID );
					assertThat( loadedChild ).isNotNull();
				}
		);

	}

	@Test
	public void setIdClassTest(SessionFactoryScope scope){
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inTransaction(
				session -> {
					stats.clear();
					ModelId id = new ModelId();
					id.setId1( 1L );
					id.setId2( 2L );
					Parent parent = session.getReference( Parent.class, id );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) parent;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					assertThat( interceptor ).isInstanceOf( EnhancementAsProxyLazinessInterceptor.class );

					assertEquals( 0, stats.getPrepareStatementCount() );

					parent.getId1();
					parent.setId1( 1L );

					assertEquals( 0, stats.getPrepareStatementCount() );

					assertThat( parent.getId1() ).isEqualTo( 1L );
					assertThat( parent.getId2() ).isEqualTo( 2L );

					assertEquals( 0, stats.getPrepareStatementCount() );
				}
		);

		scope.inTransaction(
				session -> {
					Child loadedChild = session.getReference( Child.class, lastChildID );
					assertThat( loadedChild ).isNotNull();
				}
		);
	}

	@Test
	public void updateIdClassTest(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		assertThatThrownBy(
				() -> scope.inTransaction(
				session -> {
					stats.clear();
					ModelId id = new ModelId();
					id.setId1( 1L );
					id.setId2( 2L );
					Parent parent = session.getReference( Parent.class, id );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) parent;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					assertThat( interceptor ).isInstanceOf( EnhancementAsProxyLazinessInterceptor.class );

					// should trigger an exception
					parent.setId1( 3L );
				} )
		).isInstanceOf( PersistenceException.class );

//		scope.inTransaction(
//				session -> {
//					Child loadedChild = session.load( Child.class, lastChildID );
//					assertThat( loadedChild, is( notNullValue() ) );
//				}
//		);
	}

	@Test
	public void updateIdTest(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		Long updatedId = lastChildID + 1;
		assertThatThrownBy(
				() -> scope.inTransaction(
						session -> {
							stats.clear();
							Child loadedChild = session.getReference( Child.class, lastChildID );

							final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
							final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
							assertThat( interceptor ).isInstanceOf( EnhancementAsProxyLazinessInterceptor.class );

							// should trigger an exception
							loadedChild.setId( updatedId );
						}
				)
		).isInstanceOf( PersistenceException.class );
	}

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = new Parent();
			parent.setId1( 1L );
			parent.setId2( 2L );

			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child();
				child.setId( new Long( i ) );
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
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private static class ModelId implements Serializable {
		Long id1;

		Long id2;

		public Long getId1() {
			return id1;
		}

		public void setId1(Long id1) {
			this.id1 = id1;
		}

		public Long getId2() {
			return id2;
		}

		public void setId2(Long id2) {
			this.id2 = id2;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	@IdClass( ModelId.class )
	static class Parent {

		String name;

		public Parent() {
		}

		@Id
		Long id1;

		@Id
		Long id2;

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

		public Long getId1() {
			return id1;
		}

		public void setId1(Long id1) {
			this.id1 = id1;
		}

		public Long getId2() {
			return id2;
		}

		public void setId2(Long id2) {
			this.id2 = id2;
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
		Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//		@LazyToOne(LazyToOneOption.NO_PROXY)
				Parent parent;

		@OneToMany
		List<Person> relatives;

		String name;

		Child() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
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
