/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Same as {@link CascadeDeleteCollectionTest},
 * but with {@code collectionInDefaultFetchGroup} set to {@code false} explicitly.
 * <p>
 * Kept here for <a href="https://github.com/hibernate/hibernate-orm/pull/5252#pullrequestreview-1095843220">historical reasons</a>.
 *
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10252" )
@DomainModel(
		annotatedClasses = {
			CascadeDeleteCollectionWithCollectionInDefaultFetchGroupFalseTest.Parent.class, CascadeDeleteCollectionWithCollectionInDefaultFetchGroupFalseTest.Child.class
		}
)
@SessionFactory(
		// We want to test with this setting set to false explicitly,
		// because another test already takes care of the default.
		applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
public class CascadeDeleteCollectionWithCollectionInDefaultFetchGroupFalseTest {
	private Parent originalParent;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		// Create a Parent with one Child
		originalParent = scope.fromTransaction( s -> {
					Parent p = new Parent();
					p.setName( "PARENT" );
					p.setLazy( "LAZY" );
					p.makeChild();
					s.persist( p );
					return p;
				}
		);
	}

	@Test
	public void testManagedWithUninitializedAssociation(SessionFactoryScope scope) {
		// Delete the Parent
		scope.inTransaction( s -> {
			Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
					.setParameter( "name", "PARENT" )
					.uniqueResult();
			checkInterceptor( scope, loadedParent, false );
			assertFalse( Hibernate.isPropertyInitialized( loadedParent, "children" ) );
			s.remove( loadedParent );
		} );
		// If the lazy relation is not fetch on cascade there is a constraint violation on commit
	}

	@Test
	@JiraKey("HHH-13129")
	public void testManagedWithInitializedAssociation(SessionFactoryScope scope) {
		// Delete the Parent
		scope.inTransaction( s -> {
			Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
					.setParameter( "name", "PARENT" )
					.uniqueResult();
			checkInterceptor( scope, loadedParent, false );
			loadedParent.getChildren();
			assertTrue( Hibernate.isPropertyInitialized( loadedParent, "children" ) );
			s.remove( loadedParent );
		} );
		// If the lazy relation is not fetch on cascade there is a constraint violation on commit
	}

	@Test
	@JiraKey("HHH-13129")
	public void testDetachedWithUninitializedAssociation(SessionFactoryScope scope) {
		final Parent detachedParent = scope.fromTransaction( s -> {
			return s.get( Parent.class, originalParent.getId() );
		} );

		assertFalse( Hibernate.isPropertyInitialized( detachedParent, "children" ) );

		checkInterceptor( scope, detachedParent, false );

		// Delete the detached Parent with uninitialized children
		scope.inTransaction( s -> {
			s.remove( detachedParent );
		} );
		// If the lazy relation is not fetch on cascade there is a constraint violation on commit
	}

	@Test
	@JiraKey("HHH-13129")
	public void testDetachedWithInitializedAssociation(SessionFactoryScope scope) {
		final Parent detachedParent = scope.fromTransaction( s -> {
			Parent parent = s.get( Parent.class, originalParent.getId() );
			assertFalse( Hibernate.isPropertyInitialized( parent, "children" ) );

			// initialize collection before detaching
			parent.getChildren();
			return parent;
		} );

		assertTrue( Hibernate.isPropertyInitialized( detachedParent, "children" ) );

		checkInterceptor( scope, detachedParent, false );

		// Delete the detached Parent with initialized children
		scope.inTransaction( s -> {
			s.remove( detachedParent );
		} );
		// If the lazy relation is not fetch on cascade there is a constraint violation on commit
	}

	@Test
	@JiraKey("HHH-13129")
	public void testDetachedOriginal(SessionFactoryScope scope) {

		// originalParent#children should be initialized
		assertTrue( Hibernate.isPropertyInitialized( originalParent, "children" ) );

		checkInterceptor( scope, originalParent, true );

		// Delete the Parent
		scope.inTransaction( s -> {
			s.remove( originalParent );
		} );
		// If the lazy relation is not fetch on cascade there is a constraint violation on commit
	}

	private void checkInterceptor(SessionFactoryScope scope, Parent parent, boolean isNullExpected) {
		final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Parent.class )
				.getBytecodeEnhancementMetadata();
		if ( isNullExpected ) {
			// if a null Interceptor is expected, then there shouldn't be any uninitialized attributes
			assertFalse( bytecodeEnhancementMetadata.hasUnFetchedAttributes( parent ) );
			assertNull( bytecodeEnhancementMetadata.extractInterceptor( parent ) );
		}
		else {
			assertNotNull( bytecodeEnhancementMetadata.extractInterceptor( parent ) );
		}
	}

	// --- //

	@Entity( name = "Parent" )
	@Table( name = "PARENT" )
	public static class Parent {

		Long id;

		String name;

		List<Child> children = new ArrayList<>();

		String lazy;

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		@OneToMany( mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY )
		List<Child> getChildren() {
			return Collections.unmodifiableList( children );
		}

		void setChildren(List<Child> children) {
			this.children = children;
		}

		String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}

		@Basic( fetch = FetchType.LAZY )
		String getLazy() {
			return lazy;
		}

		void setLazy(String lazy) {
			this.lazy = lazy;
		}

		void makeChild() {
			Child c = new Child();
			c.setParent( this );
			children.add( c );
		}
	}

	@Entity
	@Table( name = "CHILD" )
	static class Child {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@ManyToOne( optional = false )
		@JoinColumn( name = "parent_id" )
		Parent parent;

		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		Parent getParent() {
			return parent;
		}

		void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
