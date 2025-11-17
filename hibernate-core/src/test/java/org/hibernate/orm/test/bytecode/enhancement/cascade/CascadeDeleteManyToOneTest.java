/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
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
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.jdbc.SQLStatementInspector.extractFromSession;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Luis Barreiro
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-10252")
@DomainModel(
		annotatedClasses = {
				CascadeDeleteManyToOneTest.Parent.class, CascadeDeleteManyToOneTest.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class CascadeDeleteManyToOneTest {
	private Child originalChild;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		// Create a Parent with one Child
		originalChild = scope.fromTransaction( s -> {
					Child c = new Child();
					c.setName( "CHILD" );
					c.setLazy( "LAZY" );
					c.makeParent();
					s.persist( c );
					return c;
				}
		);
	}

	@Test
	public void testManagedWithInitializedAssociation(SessionFactoryScope scope) {
		// Delete the Child
		scope.inTransaction(
				(s) -> {
					final SQLStatementInspector statementInspector = extractFromSession( s );
					statementInspector.clear();

					final Child managedChild = (Child) s.createQuery( "SELECT c FROM Child c WHERE name=:name" )
							.setParameter( "name", "CHILD" )
							.uniqueResult();

					statementInspector.assertExecutedCount( 1 );

					// parent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( managedChild, "parent" ) );
					assertThat( managedChild.getParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( managedChild.getParent() ) );

					s.remove( managedChild );
				}
		);

		// Explicitly check that both got deleted
		scope.inTransaction( s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	@Test
	public void testDetachedWithInitializedAssociation(SessionFactoryScope scope) {
		final Child detachedChild = scope.fromTransaction(
				(s) -> {
					final SQLStatementInspector statementInspector = extractFromSession( s );
					statementInspector.clear();
					Child child = s.get( Child.class, originalChild.getId() );

					statementInspector.assertExecutedCount( 1 );

					// parent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( child, "parent" ) );
					assertThat( child.getParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( child.getParent() ) );

					return child;
				}
		);

		assertTrue( Hibernate.isPropertyInitialized( detachedChild, "parent" ) );

		checkInterceptor( scope, detachedChild, false );

		// Delete the detached Child with initialized parent
		scope.inTransaction(
				(s) -> s.remove( detachedChild )
		);

		// Explicitly check that both got deleted
		scope.inTransaction(
				(s) -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	@Test
	public void testDetachedOriginal(SessionFactoryScope scope) {

		// originalChild#parent should be initialized
		assertTrue( Hibernate.isPropertyInitialized( originalChild, "parent" ) );

		checkInterceptor( scope, originalChild, true );

		// Delete the Child
		scope.inTransaction( s -> {
					s.remove( originalChild );
				}
		);
		// Explicitly check that both got deleted
		scope.inTransaction( s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	private void checkInterceptor(SessionFactoryScope scope, Child child, boolean isNullExpected) {
		final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Child.class )
				.getBytecodeEnhancementMetadata();
		if ( isNullExpected ) {
			// if a null Interceptor is expected, then there shouldn't be any uninitialized attributes
			assertFalse( bytecodeEnhancementMetadata.hasUnFetchedAttributes( child ) );
			assertNull( bytecodeEnhancementMetadata.extractInterceptor( child ) );
		}
		else {
			assertNotNull( bytecodeEnhancementMetadata.extractInterceptor( child ) );
		}
	}

	// --- //

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {

		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		String name;

		@ManyToOne(optional = false, cascade = {
				CascadeType.PERSIST,
				CascadeType.MERGE,
				CascadeType.REMOVE
		}, fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		Parent parent;

		@Basic(fetch = FetchType.LAZY)
		String lazy;

		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}

		Parent getParent() {
			return parent;
		}

		void setParent(Parent parent) {
			this.parent = parent;
		}

		String getLazy() {
			return lazy;
		}

		void setLazy(String lazy) {
			this.lazy = lazy;
		}

		void makeParent() {
			parent = new Parent();
		}
	}
}
