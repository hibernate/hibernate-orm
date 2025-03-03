/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.junit.Assert.assertThat;

/**
 * @author Luis Barreiro
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				LazyLoadingTest.Parent.class, LazyLoadingTest.Child.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyLoadingTest {

	private static final int CHILDREN_SIZE = 10;
	private Long parentID;
	private Long lastChildID;


	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child( "Child #" + i );
				child.parent = parent;
				parent.addChild( child );
				s.persist( child );
				lastChildID = child.id;
			}
			s.persist( parent );
			parentID = parent.id;
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Child loadedChild = s.getReference( Child.class, lastChildID );
			assertThat( loadedChild, not( instanceOf( HibernateProxy.class ) ) );
			assertThat( loadedChild, instanceOf( PersistentAttributeInterceptable.class ) );
			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

			assertThat( Hibernate.isPropertyInitialized( loadedChild, "name" ), is( false ) );
			assertThat( Hibernate.isPropertyInitialized( loadedChild, "parent" ), is( false ) );
			assertThat( Hibernate.isPropertyInitialized( loadedChild, "children" ), is( false ) );

			Parent loadedParent = loadedChild.parent;
			assertThat( loadedChild.name, notNullValue() );
			assertThat( loadedParent, notNullValue() );
			assertThat( loadedChild.parent, notNullValue() );

			checkDirtyTracking( loadedChild );

			assertThat( Hibernate.isPropertyInitialized( loadedChild, "name" ), is( true ) );
			assertThat( Hibernate.isPropertyInitialized( loadedChild, "parent" ), is( true ) );
			assertThat( Hibernate.isPropertyInitialized( loadedChild, "children" ), is( true ) );

			Collection<Child> loadedChildren = loadedParent.children;
			assertThat( Hibernate.isInitialized( loadedChildren ), is( false ) );

			checkDirtyTracking( loadedChild );
			checkDirtyTracking( loadedParent );

			loadedChildren.size();
			assertThat( Hibernate.isInitialized( loadedChildren ), is( true ) );
		} );
	}

	// --- //

	@Entity
	@Table( name = "PARENT" )
	static class Parent {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@OneToMany( mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		List<Child> children;

		void addChild(Child child) {
			if ( children == null ) {
				children = new ArrayList<>();
			}
			children.add( child );
		}
	}

	@Entity
	@Table( name = "CHILD" )
	static class Child {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		Parent parent;

		String name;

		Child() {
		}

		Child(String name) {
			this.name = name;
		}
	}
}
