/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.cascade;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Luis Barreiro
 */
@TestForIssue(jiraKey = "HHH-10252")
@RunWith(BytecodeEnhancerRunner.class)
public class CascadeDeleteManyToOneTest extends BaseCoreFunctionalTestCase {
	private Child originalChild;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}

	@Before
	public void prepare() {
		// Create a Parent with one Child
		originalChild = doInHibernate(
				this::sessionFactory, s -> {
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
	public void testManagedWithUninitializedAssociation() {
		// Delete the Child
		doInHibernate(
				this::sessionFactory, s -> {
					Child loadedChild = (Child) s.createQuery( "SELECT c FROM Child c WHERE name=:name" )
							.setParameter( "name", "CHILD" )
							.uniqueResult();
					checkInterceptor( loadedChild, false );
					assertFalse( Hibernate.isPropertyInitialized( loadedChild, "parent" ) );
					s.delete( loadedChild );
				}
		);
		// Explicitly check that both got deleted
		doInHibernate(
				this::sessionFactory, s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	@Test
	public void testManagedWithInitializedAssociation() {
		// Delete the Child
		doInHibernate(
				this::sessionFactory, s -> {
					Child loadedChild = (Child) s.createQuery( "SELECT c FROM Child c WHERE name=:name" )
							.setParameter( "name", "CHILD" )
							.uniqueResult();
					checkInterceptor( loadedChild, false );
					loadedChild.getParent();
					assertTrue( Hibernate.isPropertyInitialized( loadedChild, "parent" ) );
					s.delete( loadedChild );
				}
		);
		// Explicitly check that both got deleted
		doInHibernate(
				this::sessionFactory, s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	@Test
	public void testDetachedWithUninitializedAssociation() {
		final Child detachedChild = doInHibernate(
				this::sessionFactory, s -> {
					return s.get( Child.class, originalChild.getId() );
				}
		);

		assertFalse( Hibernate.isPropertyInitialized( detachedChild, "parent" ) );

		checkInterceptor( detachedChild, false );

		// Delete the detached Child with uninitialized parent
		doInHibernate(
				this::sessionFactory, s -> {
					s.delete( detachedChild );
				}
		);
		// Explicitly check that both got deleted
		doInHibernate(
				this::sessionFactory, s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	@Test
	public void testDetachedWithInitializedAssociation() {
		final Child detachedChild = doInHibernate(
				this::sessionFactory, s -> {
					Child child = s.get( Child.class, originalChild.getId() );
					assertFalse( Hibernate.isPropertyInitialized( child, "parent" ) );

					// initialize parent before detaching
					child.getParent();
					return child;
				}
		);

		assertTrue( Hibernate.isPropertyInitialized( detachedChild, "parent" ) );

		checkInterceptor( detachedChild, false );

		// Delete the detached Child with initialized parent
		doInHibernate(
				this::sessionFactory, s -> {
					s.delete( detachedChild );
				}
		);
		// Explicitly check that both got deleted
		doInHibernate(
				this::sessionFactory, s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	@Test
	public void testDetachedOriginal() {

		// originalChild#parent should be initialized
		assertTrue( Hibernate.isPropertyInitialized( originalChild, "parent" ) );

		checkInterceptor( originalChild, true );

		// Delete the Child
		doInHibernate(
				this::sessionFactory, s -> {
					s.delete( originalChild );
				}
		);
		// Explicitly check that both got deleted
		doInHibernate(
				this::sessionFactory, s -> {
					assertNull( s.createQuery( "FROM Child c" ).uniqueResult() );
					assertNull( s.createQuery( "FROM Parent p" ).uniqueResult() );
				}
		);
	}

	private void checkInterceptor(Child child, boolean isNullExpected) {
		final BytecodeEnhancementMetadata bytecodeEnhancementMetadata =
				sessionFactory()
						.getMetamodel()
						.entityPersister( Child.class )
						.getEntityMetamodel()
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
	private static class Child {

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
		@LazyToOne(LazyToOneOption.NO_PROXY)
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
