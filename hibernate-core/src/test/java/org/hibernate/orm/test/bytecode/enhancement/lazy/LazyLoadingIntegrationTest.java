/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

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

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Luis Barreiro
 */
@DomainModel(
		annotatedClasses = {
				LazyLoadingIntegrationTest.Parent.class, LazyLoadingIntegrationTest.Child.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
				@Setting( name = AvailableSettings.DEFAULT_LIST_SEMANTICS, value = "BAG" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyLoadingIntegrationTest {

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
				s.persist( child );
				lastChildID = child.id;
			}
			s.persist( parent );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Child loadedChild = s.getReference( Child.class, lastChildID );
			checkDirtyTracking( loadedChild );

			loadedChild.name = "Barrabas";
			checkDirtyTracking( loadedChild, "name" );

			Parent loadedParent = loadedChild.parent;
			checkDirtyTracking( loadedChild, "name" );
			checkDirtyTracking( loadedParent );

			List<Child> loadedChildren = new ArrayList<>( loadedParent.children );
			loadedChildren.remove( 0 );
			loadedChildren.remove( loadedChild );
			loadedParent.setChildren( loadedChildren );

			assertNull( loadedChild.parent );
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

		void setChildren(List<Child> children) {
			this.children = children;
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
