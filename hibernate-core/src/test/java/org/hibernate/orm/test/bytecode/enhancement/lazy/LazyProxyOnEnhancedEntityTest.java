/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10922" )
@DomainModel(
		annotatedClasses = {
				LazyProxyOnEnhancedEntityTest.Parent.class, LazyProxyOnEnhancedEntityTest.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext( {EnhancerTestContext.class, LazyProxyOnEnhancedEntityTest.NoLazyLoadingContext.class} )
public class LazyProxyOnEnhancedEntityTest {

	private Long parentID;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Child c = new Child();
			em.persist( c );

			Parent parent = new Parent();
			parent.setChild( c );
			em.persist( parent );
			parentID = parent.getId();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		EventListenerRegistry registry = scope.getSessionFactory().getEventListenerRegistry();
		registry.prependListeners( EventType.LOAD, new ImmediateLoadTrap() );

		scope.inTransaction( em -> {

			em.find( Parent.class, parentID );

			// unwanted lazy load occurs on flush
		} );
	}

	private static class ImmediateLoadTrap implements LoadEventListener {
		@Override
		public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
			if ( IMMEDIATE_LOAD == loadType ) {
				String msg = loadType + ":" + event.getEntityClassName() + "#" + event.getEntityId();
				throw new RuntimeException( msg );
			}
		}
	}

	// --- //

	@Entity(name = "Parent")
	@Table( name = "PARENT" )
	static class Parent {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@OneToOne( fetch = FetchType.LAZY
		)
		Child child;

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
	}

	@Entity(name = "Child")
	@Table( name = "CHILD" )
	static class Child {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String name;

		Child() {
			// No-arg constructor necessary for proxy factory
		}
	}

	// --- //

	public static class NoLazyLoadingContext extends EnhancerTestContext {
		@Override
		public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
			return false;
		}
	}
}
